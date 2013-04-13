package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.duality.server.openfirePlugin.prediction.impl.feature.TfIdfKey;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TfIdfFeatureStore {

	private static final TfIdfFeatureStore INSTANCE = new TfIdfFeatureStore();

	public static TfIdfFeatureStore singleton() {
		return INSTANCE;
	}

	private final Map<Integer, Map<FeatureKey<?>, Object>> featureCache;

	private TfIdfFeatureStore() {
		featureCache = Maps.newConcurrentMap();
	}

	public Map<FeatureKey<?>, Object> getFeatures(final int historyId) {
		final Map<FeatureKey<?>, Object> features = featureCache.get(historyId);
		if (features != null) {
			return features;
		}

		final HistoryDatabaseAdapter historyDbAdapter = HistoryDatabaseAdapter.singleton();
		final HistoryEntry history = historyDbAdapter.getHistoryById(historyId);
		return consturctFeatures(history);
	}

	public Map<FeatureKey<?>, Object> getFeatures(final HistoryEntry history) {
		final int id = history.getId();
		final Map<FeatureKey<?>, Object> features = featureCache.get(id);
		if (features != null) {
			return features;
		}

		return consturctFeatures(history);
	}

	public void refresh() {
		featureCache.clear();
	}

	private Map<FeatureKey<?>, Object> consturctFeatures(final HistoryEntry entry) {
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

		featureCache.put(id, tfIdfs);

		return Collections.unmodifiableMap(tfIdfs);
	}
}
