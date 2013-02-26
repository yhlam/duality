package com.duality.server.openfirePlugin.prediction.impl;

import com.duality.server.openfirePlugin.prediction.FeatureKey;

/**
 * @author hei
 * 
 * A FeatureKey for TF-IDF features
 */
public class TfIdfKey extends FeatureKey<Double> {

	private final String token;
	
	/**
	 * @param token Token of the TF-IDF value.
	 */
	public TfIdfKey(final String token) {
		super("TF_IDF_" + token, Double.class);
		this.token = token;
	}
	
	/**
	 * @return Token of the TF-IDF value.
	 */
	public String getToken() {
		return token;
	}
}
