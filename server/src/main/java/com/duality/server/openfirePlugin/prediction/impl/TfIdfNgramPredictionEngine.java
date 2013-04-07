package com.duality.server.openfirePlugin.prediction.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.duality.server.openfirePlugin.prediction.impl.feature.TfIdfKey;
import com.duality.server.openfirePlugin.prediction.impl.feature.VectorSpaceFeatureKey;
import com.duality.server.openfirePlugin.prediction.impl.store.FPStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Sets;

public class TfIdfNgramPredictionEngine extends PredictionEngine {
	private static final int MAX_PREDICTION_NUM = 10;
	private static final int NEXT_ENTRY_TIME_LIMIT = 60 * 60 * 1000; // 1 Hour

	public TfIdfNgramPredictionEngine() {
	}
	
	@Override
	public List<String> getPredictions(HistoryEntry entry, String incompletedMessage) {
		final Map<FeatureKey<?>, Object> context = extractFeatures(entry);
		return getPredictions(context, incompletedMessage);
	}

	@Override
	public List<String> getPredictions(final Map<FeatureKey<?>, Object> context, final String incompletedMessage) {
		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> histories = historyDb.getAllHistory();
		final MinMaxPriorityQueue<MessageCloseness> queue = MinMaxPriorityQueue.maximumSize(MAX_PREDICTION_NUM).create();
		for (final HistoryEntry history : histories) {
			final Map<FeatureKey<?>, Object> features = extractFeatures(history);
			final double closeness = dotProduct(context, features);
			final MessageCloseness messageCloseness = new MessageCloseness(history, closeness);
			queue.add(messageCloseness);
		}

		final List<String> predictions = Lists.newArrayListWithCapacity(queue.size());

		while (!queue.isEmpty()) {
			final MessageCloseness closeness = queue.poll();
			final HistoryEntry historyEntry = closeness.getHistoryEntry();
			final HistoryEntry nextHistory = historyDb.nextHistoryEntry(historyEntry, NEXT_ENTRY_TIME_LIMIT);
			if (nextHistory != null) {
				final String message = nextHistory.getMessage();
				if (incompletedMessage == null || message.startsWith(incompletedMessage)) {
					predictions.add(message);
				}
			}
		}

		if (predictions.isEmpty()) {
			// TODO: use n-gram here
			// final String[] split = incompletedMessage.split(" ");
		}

		return predictions;
	}

	private Map<FeatureKey<?>, Object> extractFeatures(final HistoryEntry entry) {
		final Map<FeatureKey<?>, Object> tfIdfs = Maps.newHashMap();
		final int id = entry.getId();

		final FPStore fpStore = FPStore.singleton();
		final Set<Set<AtomicFeature<?>>> frequetPatterns = fpStore.getFrequetPatterns();

		final AtomicFeaturesManager atomicFeaturesManager = AtomicFeaturesManager.singleton();
		final List<AtomicFeature<?>> features = atomicFeaturesManager.getFeatures(entry);
		final HashSet<AtomicFeature<?>> featureSet = Sets.newHashSet(features);
		final List<Set<AtomicFeature<?>>> compoundFeatures = Lists.newLinkedList();

		for (final Set<AtomicFeature<?>> fp : frequetPatterns) {
			final boolean containsFp = featureSet.containsAll(fp);
			if (containsFp) {
				compoundFeatures.add(fp);
			}
		}

		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		for (final Set<AtomicFeature<?>> feature : compoundFeatures) {
			final double idf = tfIdfStore.getInvertedDocumentFrequency(feature);
			final double tf = tfIdfStore.getTermFrequency(id, feature);
			final double tfIdf = tf * idf;
			final TfIdfKey tfIdfKey = TfIdfKey.getKey(feature);
			tfIdfs.put(tfIdfKey, tfIdf);
		}

		return tfIdfs;
	}

	private double cosine(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features) {
		final Set<FeatureKey<?>> contextKeySet = context.keySet();
		final Set<FeatureKey<?>> featuresKeySet = features.keySet();

		final Set<FeatureKey<?>> intersaction = Sets.newHashSet(featuresKeySet);
		intersaction.retainAll(contextKeySet);

		final double dotProduct = dotProduct(context, features, intersaction);

		final Set<Entry<FeatureKey<?>, Object>> entrySet = features.entrySet();
		double sumOfProducts = 0.0;
		for (final Entry<FeatureKey<?>, Object> entry : entrySet) {
			final FeatureKey<?> key = entry.getKey();

			if (key instanceof VectorSpaceFeatureKey) {
				@SuppressWarnings("rawtypes")
				final VectorSpaceFeatureKey vectorSpaceFeatureKey = (VectorSpaceFeatureKey) key;
				final Object value = entry.getValue();
				@SuppressWarnings("unchecked")
				final double product = vectorSpaceFeatureKey.multiply(value, value);
				sumOfProducts += product;
			}
		}

		return dotProduct / sumOfProducts;
	}

	private double dotProduct(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features) {
		final Set<FeatureKey<?>> contextKeySet = context.keySet();
		final Set<FeatureKey<?>> featuresKeySet = features.keySet();

		final Set<FeatureKey<?>> intersaction = Sets.newHashSet(featuresKeySet);
		intersaction.retainAll(contextKeySet);

		final double dotProduct = dotProduct(context, features, intersaction);
		return dotProduct;
	}

	private double dotProduct(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features, final Set<FeatureKey<?>> intersaction) {
		double dotProduct = 0.0;
		for (final FeatureKey<?> key : intersaction) {
			if (key instanceof VectorSpaceFeatureKey) {
				@SuppressWarnings("rawtypes")
				final VectorSpaceFeatureKey vectorSpaceFeatureKey = (VectorSpaceFeatureKey) key;
				@SuppressWarnings("unchecked")
				final double product = vectorSpaceFeatureKey.multiply(context, features);
				dotProduct += product;
			}
		}

		if (dotProduct == 0.0) {
			return 0.0;
		}
		return dotProduct;
	}

	private static class MessageCloseness implements Comparable<MessageCloseness> {
		private final HistoryEntry entry;
		private final double closeness;

		public MessageCloseness(final HistoryEntry entry, final double closeness) {
			this.entry = entry;
			this.closeness = closeness;
		}

		public HistoryEntry getHistoryEntry() {
			return entry;
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
			return "{historyEntry: " + entry + ", closeness: " + closeness + "}";
		}
	}
}
