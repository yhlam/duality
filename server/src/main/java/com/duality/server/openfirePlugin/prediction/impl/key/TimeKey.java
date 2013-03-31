package com.duality.server.openfirePlugin.prediction.impl.key;

import java.util.Date;

import com.duality.server.openfirePlugin.prediction.impl.feature.VectorSpaceFeatureKey;

public class TimeKey extends VectorSpaceFeatureKey<Date> {
	private static final TimeKey INSTANCE = new TimeKey();

	public static TimeKey singleton() {
		return INSTANCE;
	}

	private TimeKey() {
		super("TIME", Date.class);
	}

	@Override
	public double multiply(final Date value1, final Date value2) {
		return 0;
	}
}
