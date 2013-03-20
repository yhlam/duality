package com.duality.server.openfirePlugin.prediction.impl.feature;

public class AtomicFeature<T> {
	private final FeatureType type;
	private final T value;
	
	public AtomicFeature(final FeatureType type, final T value) {
		this.type = type;
		this.value = value;
	}

	public FeatureType getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return type.hashCode() * 31 + value.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (obj instanceof AtomicFeature) {
			final AtomicFeature<?> that = (AtomicFeature<?>) obj;
			final boolean typeEqual = this.getType() == that.getType();
			return typeEqual && this.getValue().equals(that.getValue());
		}

		return false;
	}
	
	@Override
	public String toString() {
		return type + ": " + value;
	}

	public static enum FeatureType {
		SENDER,
		RECEIVER,
		DAY_OF_WEEK,
		MONTH,
		TIME,
		SENDER_LOCATION,
		RECEIVER_LOCATION,
		TOKEN;
	}
}
