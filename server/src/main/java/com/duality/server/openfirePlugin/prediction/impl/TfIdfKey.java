package com.duality.server.openfirePlugin.prediction.impl;

import java.util.List;

import com.duality.server.openfirePlugin.prediction.FeatureKey;

/**
 * @author hei
 * 
 *         A FeatureKey for TF-IDF features
 */
public class TfIdfKey extends FeatureKey<Double> {

	private final List<String> tokens;

	/**
	 * @param token
	 *            Token of the TF-IDF value.
	 */
	public TfIdfKey(final List<String> tokens) {
		super("TF_IDF_" + tokens, Double.class);
		this.tokens = tokens;
	}

	/**
	 * @return Token of the TF-IDF value.
	 */
	public List<String> getTokens() {
		return tokens;
	}
}
