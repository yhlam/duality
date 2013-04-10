package com.duality.server.openfirePlugin.prediction.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.TfIdfKey;
import com.duality.server.openfirePlugin.prediction.impl.feature.VectorSpaceFeatureKey;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfFeatureStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore.TermFrequency;
import com.google.common.base.Function;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Table;

public class TfIdfNgramPredictionEngine extends PredictionEngine {
	private static final int MAX_PREDICTION_NUM = 10;
	private static final int NEXT_ENTRY_TIME_LIMIT = 60 * 60 * 1000; // 1 Hour
	private static final Class<? extends ClosenessAggregator> AGGREGATOR = DotProductAggregator.class;

	private static final ClosenessAggregator createAggregator() {
		try {
			final Constructor<? extends ClosenessAggregator> constructor = AGGREGATOR.getDeclaredConstructor();
			constructor.setAccessible(true);
			final ClosenessAggregator newInstance = constructor.newInstance();
			return newInstance;
		} catch (final InstantiationException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		} catch (final NoSuchMethodException e) {
			e.printStackTrace();
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	private TfIdfNgramPredictionEngine() {
	}

	@Override
	public List<String> getPredictions(final HistoryEntry entry, final String incompletedMessage) {
		final TfIdfFeatureStore tfIdfFeatureStore = TfIdfFeatureStore.singleton();
		final Map<FeatureKey<?>, Object> context = tfIdfFeatureStore.getFeatures(entry);
		return getPredictions(context, incompletedMessage);
	}

	@Override
	public List<String> getPredictions(final Map<FeatureKey<?>, Object> context, final String incompletedMessage) {
		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		final Set<FeatureKey<?>> contextFeatures = context.keySet();
		final ClosenessAggregator aggregator = createAggregator();

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		for (final FeatureKey<?> feature : contextFeatures) {
			final TfIdfKey key = (TfIdfKey) feature;
			final Set<AtomicFeature<?>> tokens = key.getTokens();
			final Double tfIdf = key.getValue(context);
			final double idf = tfIdfStore.getInvertedDocumentFrequency(tokens);
			final double tfIdfIdf = tfIdf * idf;

			final List<TermFrequency> tfs = tfIdfStore.getRelevantTermFrequencies(tokens);
			for (final TermFrequency tf : tfs) {
				final int id = tf.getDocumentId();

				final HistoryEntry nextHistory = historyDb.nextHistoryEntry(id, NEXT_ENTRY_TIME_LIMIT);
				if (nextHistory == null) {
					continue;
				}

				final String message = nextHistory.getMessage();
				if (incompletedMessage != null) {
					if (!message.startsWith(incompletedMessage)) {
						continue;
					}
				}

				final double frequency = tf.getFrequency();
				final double closeness = tfIdfIdf * frequency;
				aggregator.offer(message, id, closeness);
			}
		}

		final MinMaxPriorityQueue<MessageCloseness> queue = MinMaxPriorityQueue.maximumSize(MAX_PREDICTION_NUM).create();
		for (final MessageCloseness messageCloseness : aggregator) {
			queue.add(messageCloseness);
		}

		final List<String> predictions = Lists.newArrayListWithCapacity(queue.size());

		while (!queue.isEmpty()) {
			final MessageCloseness closeness = queue.poll();
			final String message = closeness.getMessage();
			predictions.add(message);
		}

		if (predictions.isEmpty()) {
			// TODO: use n-gram here
			// final String[] split = incompletedMessage.split(" ");
		}

		return predictions;
	}

	private static class MessageCloseness implements Comparable<MessageCloseness> {
		private final String message;
		private final double closeness;

		public MessageCloseness(final String message, final double closeness) {
			this.message = message;
			this.closeness = closeness;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public int compareTo(final MessageCloseness that) {
			if (this.closeness > that.closeness) {
				return -1;
			} else if (this.closeness == that.closeness) {
				return 0;
			} else {
				return 1;
			}
		}

		@Override
		public String toString() {
			return "{message: " + message + ", closeness: " + closeness + "}";
		}
	}

	private static interface ClosenessAggregator extends Iterable<MessageCloseness> {
		public void offer(final String message, final int id, final double tfIdfProduct);
	}

	private static class DotProductAggregator implements ClosenessAggregator {

		private final Map<String, Double> closenesses = Maps.newHashMap();

		@Override
		public void offer(final String message, final int id, final double tfIdfProduct) {
			final Double existing = closenesses.get(message);
			if (existing == null) {
				closenesses.put(message, tfIdfProduct);
			} else {
				final double total = existing + tfIdfProduct;
				closenesses.put(message, total);
			}
		}

		@Override
		public Iterator<MessageCloseness> iterator() {
			final Set<Entry<String, Double>> entrySet = closenesses.entrySet();
			final Iterator<Entry<String, Double>> iterator = entrySet.iterator();
			final Iterator<MessageCloseness> closenessIt = Iterators.transform(iterator, new Function<Entry<String, Double>, MessageCloseness>() {
				@Override
				public MessageCloseness apply(final Entry<String, Double> entry) {
					final String message = entry.getKey();
					final Double closeness = entry.getValue();
					return new MessageCloseness(message, closeness);
				}
			});

			return closenessIt;
		}
	}

	@SuppressWarnings("unused")
	private static class ConsineAggregator implements ClosenessAggregator {

		private final Table<String, Integer, Double> closenesses = HashBasedTable.create();

		@Override
		public void offer(final String message, final int id, final double tfIdfProduct) {
			final Double existing = closenesses.get(message, id);
			if (existing == null) {
				closenesses.put(message, id, tfIdfProduct);
			} else {
				final double total = existing + tfIdfProduct;
				closenesses.put(message, id, total);
			}
		}

		@Override
		public Iterator<MessageCloseness> iterator() {
			final Map<Integer, Double> modulusCache = Maps.newHashMap();
			final TfIdfFeatureStore tfIdfFeatureStore = TfIdfFeatureStore.singleton();

			final Map<String, Map<Integer, Double>> rowMap = closenesses.rowMap();
			final Set<Entry<String, Map<Integer, Double>>> rows = rowMap.entrySet();
			final Iterator<Entry<String, Map<Integer, Double>>> iterator = rows.iterator();

			final Iterator<MessageCloseness> closenessIt = Iterators.transform(iterator, new Function<Entry<String, Map<Integer, Double>>, MessageCloseness>() {
				@Override
				public MessageCloseness apply(final Entry<String, Map<Integer, Double>> entry) {
					double closeness = 0;

					final Map<Integer, Double> dotProducts = entry.getValue();
					final Set<Entry<Integer, Double>> dotProductEntries = dotProducts.entrySet();
					for (final Entry<Integer, Double> dotProduct : dotProductEntries) {
						final Integer id = dotProduct.getKey();
						Double modulus = modulusCache.get(id);
						if (modulus == null) {
							final Map<FeatureKey<?>, Object> features = tfIdfFeatureStore.getFeatures(id);
							modulus = calcModulus(features);
							modulusCache.put(id, modulus);
						}

						final Double value = dotProduct.getValue();
						final double consine = value / modulus;
						closeness += consine;
					}

					final String message = entry.getKey();
					return new MessageCloseness(message, closeness);
				}
			});

			return closenessIt;
		}

		private double calcModulus(final Map<FeatureKey<?>, Object> context) {
			double modulus = 0.0;
			final Set<FeatureKey<?>> keySet = context.keySet();
			for (final FeatureKey<?> key : keySet) {
				if (key instanceof VectorSpaceFeatureKey) {
					@SuppressWarnings("rawtypes")
					final VectorSpaceFeatureKey vectorSpaceFeatureKey = (VectorSpaceFeatureKey) key;
					@SuppressWarnings("unchecked")
					final double product = vectorSpaceFeatureKey.multiply(context, context);
					modulus += product;
				}
			}

			return Math.sqrt(modulus);
		}
	}
}
