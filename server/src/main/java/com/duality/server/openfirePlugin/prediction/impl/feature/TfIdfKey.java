package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author hei
 * 
 *         A FeatureKey for TF-IDF features
 */
public class TfIdfKey extends VectorSpaceFeatureKey<Double> {

	private static final Map<Set<AtomicFeature<?>>, TfIdfKey> KEYS = Maps.newHashMap();
	

	public static final TfIdfKey getKey(final AtomicFeature<?> term) {
		final Set<AtomicFeature<?>> termList = Collections.<AtomicFeature<?>>singleton(term);
		return getKey(termList);
	}
	
	public static final TfIdfKey getKey(final AtomicFeature<?>... term) {
		final Set<AtomicFeature<?>> termList = Sets.newHashSet(term);
		return getKey(termList);
	}

	public static final TfIdfKey getKey(final Set<AtomicFeature<?>> term) {

		final TfIdfKey key = KEYS.get(term);
		if (key != null) {
			return key;
		}

		final TfIdfKey newKey = new TfIdfKey(term);
		KEYS.put(term, newKey);

		return newKey;
	}

	private final Set<AtomicFeature<?>> tokens;

	/**
	 * @param token
	 *            Token of the TF-IDF value.
	 */
	private TfIdfKey(final Set<AtomicFeature<?>> tokens) {
		super("TF_IDF_" + tokens, Double.class);
		this.tokens = tokens;
	}

	/**
	 * @return Token of the TF-IDF value.
	 */
	public Set<AtomicFeature<?>> getTokens() {
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
