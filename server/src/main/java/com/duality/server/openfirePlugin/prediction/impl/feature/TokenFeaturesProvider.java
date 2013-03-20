package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.List;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;

public class TokenFeaturesProvider implements AtomicFeaturesProvider {

	public static String[] extractTokens(final String message) {
		final String[] tokens = message.split("\\W+");
		for (int i = 0; i < tokens.length; i++) {
			final String token = tokens[i];
			final String lowerCase = token.toLowerCase();
			tokens[i] = lowerCase;
		}

		return tokens;
	}

	@Override
	public void constructFeatures(final HistoryEntry history, final List<AtomicFeature<?>> features) {
		final String message = history.getMessage();
		final String[] tokens = extractTokens(message);
		for (final String token : tokens) {
			TfIdfUtils.addAtomicFeature(features, FeatureType.TOKEN, token);
		}
	}
}
