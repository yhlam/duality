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
import com.duality.server.openfirePlugin.dataTier.MessageType;
import com.duality.server.openfirePlugin.dataTier.NextHistoryInfo;
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
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

public class TfIdfNgramPredictionEngine extends PredictionEngine {
	private static final int MAX_PREDICTION_NUM = 10;
	private static final int NEXT_ENTRY_TIME_LIMIT = 60 * 60 * 1000; // 1 Hour
	private static final long LOOKBACK_MESSAGE_PERIOD = 15 * 60 * 1000; // 15 minutes
	private static final int LOOKBACK_MESSAGE_MAX_COUNT = 20;
	private static final double MIN_CLOSENESS = 0.0;
	private static final Class<? extends ClosenessAggregator> AGGREGATOR = ConsineAggregator.class;

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
	public List<String> getPredictions(final HistoryEntry entry, final String incompletedMessage, final MessageType type) {
		final TfIdfFeatureStore tfIdfFeatureStore = TfIdfFeatureStore.singleton();
		final Map<FeatureKey<?>, Object> context = tfIdfFeatureStore.getFeatures(entry);
		return getPredictions(context, incompletedMessage, type);
	}

	@Override
	public List<String> getPredictions(final Map<FeatureKey<?>, Object> context, final String incompletedMessage, final MessageType type) {
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

				long nextTimeInterval = NEXT_ENTRY_TIME_LIMIT;
				int nextId = id;
				int count = 0;
				while (count < LOOKBACK_MESSAGE_MAX_COUNT && nextTimeInterval >= 0) {
					final NextHistoryInfo historyInfo = historyDb.nextHistoryEntry(nextId);
					if (historyInfo == null) {
						break;
					}

					final HistoryEntry history = historyInfo.history;
					nextId = history.getId();
					if (historyInfo.type == type && historyInfo.interval <= nextTimeInterval) {
						final String message = history.getMessage();
						if (incompletedMessage == null || message.startsWith(incompletedMessage)) {
							final double frequency = tf.getFrequency();
							final double closeness = tfIdfIdf * frequency;
							final double discount = Math.exp(-count);
							aggregator.offer(message, id, nextId, discount, closeness);
						}
					}
					count++;
					if(count == 1) {
						nextTimeInterval = LOOKBACK_MESSAGE_PERIOD;
					}
					nextTimeInterval -= historyInfo.interval;
				}
			}
		}

		final MinMaxPriorityQueue<MessageCloseness> queue = MinMaxPriorityQueue.maximumSize(MAX_PREDICTION_NUM).create();
		final double minCloseness = aggregator.getMinCloseness(context);
		for (final MessageCloseness messageCloseness : aggregator) {
			final double closeness = messageCloseness.getCloseness();
			if (closeness >= minCloseness) {
				queue.add(messageCloseness);
			}
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

		public double getCloseness() {
			return closeness;
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
		public void offer(final String message, final int msgId, final int fromId, final double discount, final double tfIdfProduct);

		public double getMinCloseness(final Map<FeatureKey<?>, Object> context);
	}

	private static class DotProductAggregator implements ClosenessAggregator {

		private final Map<String, Multiset<String>> key2Message = Maps.newHashMap();
		private final Map<String, List<Double>> closenesses = Maps.newHashMap();

		@Override
		public void offer(final String message, final int msgId, final int fromId, final double discount, final double tfIdfProduct) {
			final String key = message.toLowerCase();
			final List<Double> existing = closenesses.get(message);
			if (existing == null) {
				final List<Double> tfIdfs = Lists.newLinkedList();
				tfIdfs.add(tfIdfProduct * discount);
				closenesses.put(key, tfIdfs);
			} else {
				existing.add(tfIdfProduct * discount);
			}

			Multiset<String> messages = key2Message.get(key);
			if (messages == null) {
				messages = HashMultiset.create();
				key2Message.put(key, messages);
			}
			messages.add(message);
		}

		@Override
		public Iterator<MessageCloseness> iterator() {
			final Set<Entry<String, List<Double>>> entrySet = closenesses.entrySet();
			final Iterator<Entry<String, List<Double>>> iterator = entrySet.iterator();
			final Iterator<MessageCloseness> closenessIt = Iterators.transform(iterator, new Function<Entry<String, List<Double>>, MessageCloseness>() {
				@Override
				public MessageCloseness apply(final Entry<String, List<Double>> entry) {
					final String key = entry.getKey();
					final Multiset<String> messages = key2Message.get(key);
					final Set<com.google.common.collect.Multiset.Entry<String>> entrySet = messages.entrySet();
					int maxCount = 0;
					String message = null;
					for (final com.google.common.collect.Multiset.Entry<String> messageCount : entrySet) {
						final int count = messageCount.getCount();
						if (count > maxCount) {
							maxCount = count;
							message = messageCount.getElement();
						}
					}
					final List<Double> closenesses = entry.getValue();
					final int size = closenesses.size();
					final double closeness;
					if (size == 1) {
						closeness = closenesses.get(0);
					} else {
						double total = 0.0;
						for (final Double value : closenesses) {
							total += value;
						}
						total /= size - Math.log(size);
						closeness = total;
					}

					return new MessageCloseness(message, closeness);
				}
			});

			return closenessIt;
		}

		@Override
		public double getMinCloseness(final Map<FeatureKey<?>, Object> context) {
			return MIN_CLOSENESS;
		}
	}

	private static class ConsineAggregator implements ClosenessAggregator {

		private final Map<String, Multiset<String>> key2Message = Maps.newHashMap();
		private final Table<String, Integer, List<IdDiscountCloseness>> closenesses = HashBasedTable.create();

		@Override
		public void offer(final String message, final int msgId, final int fromId, final double discount, final double tfIdfProduct) {
			final String key = message.toLowerCase();
			final List<IdDiscountCloseness> existing = closenesses.get(key, msgId);
			if (existing == null) {
				final List<IdDiscountCloseness> newList = Lists.newLinkedList();
				newList.add(new IdDiscountCloseness(fromId, discount, tfIdfProduct));
				closenesses.put(key, msgId, newList);
			} else {
				existing.add(new IdDiscountCloseness(fromId, discount, tfIdfProduct));
			}

			Multiset<String> messages = key2Message.get(key);
			if (messages == null) {
				messages = HashMultiset.create();
				key2Message.put(key, messages);
			}
			messages.add(message);
		}

		@Override
		public Iterator<MessageCloseness> iterator() {
			final Map<Integer, Double> modulusCache = Maps.newHashMap();
			final TfIdfFeatureStore tfIdfFeatureStore = TfIdfFeatureStore.singleton();

			final Map<String, Map<Integer, List<IdDiscountCloseness>>> rowMap = closenesses.rowMap();
			final Set<Entry<String, Map<Integer, List<IdDiscountCloseness>>>> rows = rowMap.entrySet();
			final Iterator<Entry<String, Map<Integer, List<IdDiscountCloseness>>>> iterator = rows.iterator();

			final Iterator<MessageCloseness> closenessIt = Iterators.transform(iterator, new Function<Entry<String, Map<Integer, List<IdDiscountCloseness>>>, MessageCloseness>() {
				@Override
				public MessageCloseness apply(final Entry<String, Map<Integer, List<IdDiscountCloseness>>> entry) {
					double closeness = 0;

					final Map<Integer, List<IdDiscountCloseness>> dotProducts = entry.getValue();
					final Set<Entry<Integer, List<IdDiscountCloseness>>> dotProductEntries = dotProducts.entrySet();
					for (final Entry<Integer, List<IdDiscountCloseness>> dotProduct : dotProductEntries) {
						final List<IdDiscountCloseness> values = dotProduct.getValue();
						final Map<Integer, Double> id2discount = Maps.newHashMap();
						double consine = 0;
						for (IdDiscountCloseness value : values) {
							id2discount.put(value.id, value.discount);
							consine += value.discount * value.closeness;
						}
						
						double totalModulus = 0;
						final Set<Entry<Integer, Double>> idDiscounts = id2discount.entrySet();
						for (Entry<Integer, Double> idDiscount : idDiscounts) {
							final Integer id = idDiscount.getKey();
							Double modulus = modulusCache.get(id);
							if (modulus == null) {
								final Map<FeatureKey<?>, Object> features = tfIdfFeatureStore.getFeatures(id);
								modulus = calcModulus(features);
								modulusCache.put(id, modulus);
							}
							
							final Double discount = idDiscount.getValue();
							totalModulus += modulus * discount * discount;
						}
						consine /= Math.sqrt(totalModulus);
						closeness += consine;
					}

					final int size = dotProductEntries.size();
					final double discount = size - Math.log10(size);
					closeness /= discount;

					final String key = entry.getKey();
					final Multiset<String> messages = key2Message.get(key);
					final Set<com.google.common.collect.Multiset.Entry<String>> entrySet = messages.entrySet();
					int maxCount = 0;
					String message = null;
					for (final com.google.common.collect.Multiset.Entry<String> messageCount : entrySet) {
						final int count = messageCount.getCount();
						if (count > maxCount) {
							maxCount = count;
							message = messageCount.getElement();
						}
					}
					return new MessageCloseness(message, closeness);
				}
			});

			return closenessIt;
		}

		@Override
		public double getMinCloseness(final Map<FeatureKey<?>, Object> context) {
			return calcModulus(context) * MIN_CLOSENESS;
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

			return modulus;
		}
		
		private static class IdDiscountCloseness {
			public final int id;
			public final double discount;
			public final double closeness;

			public IdDiscountCloseness(int id, double discount, double closeness) {
				this.id = id;
				this.discount = discount;
				this.closeness = closeness;
			}
		}
	}
}
