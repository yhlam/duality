package com.duality.server.openfirePlugin.prediction.impl;

import java.util.Date;

import com.duality.server.openfirePlugin.prediction.FeatureKey;

/**
 * @author hei
 * 
 *         Keys of features that available in client.
 * 
 * @param <T>
 *            Type of the value
 */
public class ClientContextKey<T> extends FeatureKey<T> {

	public static ClientContextKey<Date> TIME = new ClientContextKey<Date>("TIME", Date.class);
	public static ClientContextKey<String> SENDER = new ClientContextKey<String>("SENDER", String.class);
	public static ClientContextKey<String> RECEIVER = new ClientContextKey<String>("RECEIVER", String.class);
	public static ClientContextKey<Location> SENDER_LOCATION = new ClientContextKey<Location>("SENDER_LOCATION", Location.class);
	public static ClientContextKey<Location> RECEIVER_LOCATION = new ClientContextKey<Location>("RECEIVER_LOCATION", Location.class);
	public static ClientContextKey<String> SENDER_ACTIVITY = new ClientContextKey<String>("SENDER_ACTIVITY", String.class);
	public static ClientContextKey<String> RECEIVER_ACTIVITY = new ClientContextKey<String>("RECEIVER_ACTIVITY", String.class);

	/**
	 * Set as private to prevent construction of new key
	 */
	private ClientContextKey(final String name, final Class<T> cls) {
		super(name, cls);
	}

	/**
	 * @author hei
	 * 
	 *         GPS location
	 */
	public static class Location {
		private final double latitude;
		private final double longtitude;

		public Location(final double latitude, final double longtitude) {
			super();
			this.latitude = latitude;
			this.longtitude = longtitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongtitude() {
			return longtitude;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof Location) {
				final Location that = (Location) obj;
				return this.latitude == that.latitude && this.latitude == that.latitude;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return (int) (latitude * 31 + longtitude);
		}
	}
}
