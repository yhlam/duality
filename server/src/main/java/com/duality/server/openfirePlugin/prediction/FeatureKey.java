package com.duality.server.openfirePlugin.prediction;

import java.util.Map;

/**
 * @author hei
 *
 * Context is defined as a FeatureKey to feature value map.
 * FeatureKey is used as a key in the context map.
 *
 * @param <T> Type of value
 */
public abstract class FeatureKey<T> {
	private final String name;
	private final Class<T> cls;

	/**
	 * @param name Name of the key. An unique identifier of the key.
	 * @param cls Type of value used by the keys.
	 */
	public FeatureKey(final String name, final Class<T> cls) {
		this.name = name;
		this.cls = cls;
	}

	/**
	 * @return Name of the key. An unique identifier of the key.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Type of value used by the keys.
	 */
	public Class<T> getType() {
		return cls;
	}

	/**
	 * @param context Context map
	 * @return Casted value of the feature
	 */
	public T getValue(final Map<? extends FeatureKey<?>, ?> context) {
		final Object value = context.get(this);
		final T casted = cls.cast(value);
		return casted;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FeatureKey<?>) {
			final FeatureKey<?> that = (FeatureKey<?>) obj;
			final String thatName = that.getName();
			return name.equals(thatName);
		}
		
		return false;
	}
}
