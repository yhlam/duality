package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.List;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;

interface AtomicFeaturesProvider {
	void constructFeatures(HistoryEntry history, List<AtomicFeature<?>> features);
}
