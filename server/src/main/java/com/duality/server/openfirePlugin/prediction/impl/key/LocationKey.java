package com.duality.server.openfirePlugin.prediction.impl.key;

import com.duality.server.openfirePlugin.prediction.impl.feature.VectorSpaceFeatureKey;
import com.duality.server.openfirePlugin.prediction.impl.key.LocationKey.Location;

public class LocationKey extends VectorSpaceFeatureKey<Location> {

	public static final LocationKey SENDER_LOCATION = new LocationKey("SENDER_LOCATION");
	public static final LocationKey RECEIVER_LOCATION = new LocationKey("RECEIVER_LOCATION");

	private LocationKey(final String name) {
		super(name, Location.class);
	}

	@Override
	public double multiply(final Location value1, final Location value2) {
		// TODO Auto-generated method stub
		return 0;
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

		@Override
		public String toString() {
			return "{lat=" + latitude + ", long=" + longtitude + "}";
		}
	}
}
