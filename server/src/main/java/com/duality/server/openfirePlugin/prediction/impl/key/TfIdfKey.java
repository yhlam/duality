package com.duality.server.openfirePlugin.prediction.impl.key;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author hei
 * 
 *         A FeatureKey for TF-IDF features
 */
public class TfIdfKey extends VectorSpaceFeatureKey<Double> {

	private static final Map<List<String>, TfIdfKey> KEYS = Maps.newHashMap();

	public static final TfIdfKey getKey(final List<String> tokens) {
		final List<String> lowerCaseTokens = Lists.transform(tokens, new Function<String, String>() {

			@Override
			public String apply(final String token) {
				return token.toLowerCase();
			}
		});

		final TfIdfKey key = KEYS.get(lowerCaseTokens);
		if (key != null) {
			return key;
		}

		final TfIdfKey newKey = new TfIdfKey(lowerCaseTokens);
		KEYS.put(lowerCaseTokens, newKey);

		return newKey;
	}

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

	@Override
	public double multiply(final Double value1, final Double value2) {
		if (value1 == null || value2 == null) {
			return 0;
		}

		return value1 * value2;
	}
}
