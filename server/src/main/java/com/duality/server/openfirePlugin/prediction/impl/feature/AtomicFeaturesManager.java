package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.Arrays;
import java.util.List;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.google.common.collect.Lists;

public class AtomicFeaturesManager {
	private static final AtomicFeaturesManager INSTANCE = new AtomicFeaturesManager();

	public static AtomicFeaturesManager singleton() {
		return INSTANCE;
	}

	private final List<AtomicFeaturesProvider> providers;

	private AtomicFeaturesManager() {
		providers = Arrays.asList(
				new UserFeaturesProvider(),
				new TimeFeaturesProdiver(),
				new LocationFeaturesProvider(),
				new TokenFeaturesProvider());
	}

	public List<AtomicFeature<?>> constructFeatures(final HistoryEntry history) {
		final List<AtomicFeature<?>> features = Lists.newLinkedList();

		for (final AtomicFeaturesProvider provider : providers) {
			provider.constructFeatures(history, features);
		}

		return features;
	}
}
