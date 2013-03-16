package com.duality.server.openfirePlugin.prediction.impl.key;

public class UserKey extends VectorSpaceFeatureKey<String> {

	public static final UserKey SENDER = new UserKey("SENDER");
	public static final UserKey RECEIVER = new UserKey("RECEIVER");

	private UserKey(final String name) {
		super(name, String.class);
	}

	@Override
	public double multiply(final String value1, final String value2) {
		// TODO Auto-generated method stub
		return 0;
	}

}
