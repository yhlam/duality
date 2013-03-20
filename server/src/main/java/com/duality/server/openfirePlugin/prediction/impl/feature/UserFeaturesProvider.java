package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.List;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;

public class UserFeaturesProvider implements AtomicFeaturesProvider {

	@Override
	public void constructFeatures(final HistoryEntry history, final List<AtomicFeature<?>> features) {
		final String sender = history.getSender();
		TfIdfUtils.addAtomicFeature(features, FeatureType.SENDER, sender);
		
		final String receiver = history.getReceiver();
		TfIdfUtils.addAtomicFeature(features, FeatureType.RECEIVER, receiver);
	}
}
