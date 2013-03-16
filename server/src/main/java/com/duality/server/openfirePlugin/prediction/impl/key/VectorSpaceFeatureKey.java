package com.duality.server.openfirePlugin.prediction.impl.key;

import java.util.Map;

import com.duality.server.openfirePlugin.prediction.FeatureKey;

public abstract class VectorSpaceFeatureKey<T> extends FeatureKey<T> {

	public VectorSpaceFeatureKey(final String name, final Class<T> cls) {
		super(name, cls);
	}

	public abstract double multiply(T value1, T value2);

	public double multiply(final Map<? extends FeatureKey<?>, ?> context1, final Map<? extends FeatureKey<?>, ?> context2) {
		final T value1 = getValue(context1);
		final T value2 = getValue(context2);
		return multiply(value1, value2);
	}
}
