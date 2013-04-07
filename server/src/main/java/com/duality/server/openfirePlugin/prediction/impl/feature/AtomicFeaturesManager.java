package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AtomicFeaturesManager {
	private static final AtomicFeaturesManager INSTANCE = new AtomicFeaturesManager();

	public static AtomicFeaturesManager singleton() {
		return INSTANCE;
	}

	private final List<AtomicFeaturesProvider> providers;
	private final Map<Integer, List<AtomicFeature<?>>> featureCache;

	private AtomicFeaturesManager() {
		providers = Arrays.asList(
				new UserFeaturesProvider(),
				new TimeFeaturesProdiver(),
				new LocationFeaturesProvider(),
				new TokenFeaturesProvider());

		featureCache = Maps.newConcurrentMap();
	}

	public List<AtomicFeature<?>> getFeatures(final HistoryEntry history) {
		final int id = history.getId();
		final List<AtomicFeature<?>> features = featureCache.get(id);
		if (features != null) {
			return features;
		}

		return consturctFeatures(history);
	}

	private List<AtomicFeature<?>> consturctFeatures(final HistoryEntry history) {
		final List<AtomicFeature<?>> features = Lists.newLinkedList();

		for (final AtomicFeaturesProvider provider : providers) {
			provider.constructFeatures(history, features);
		}

		final int id = history.getId();
		featureCache.put(id, features);

		return features;
	}
}
