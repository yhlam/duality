package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.List;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;

public class LocationFeaturesProvider implements AtomicFeaturesProvider {

	private static final double UNIT_DEGREE = 0.05;

	@Override
	public void constructFeatures(final HistoryEntry history, final List<AtomicFeature<?>> features) {
		final double senderLatitude = history.getSenderlatitude();
		final double senderLongtitude = history.getSenderLongtitude();
		addLocationFeature(features, FeatureType.SENDER_LOCATION, senderLatitude, senderLongtitude);

		final double receiverLatitude = history.getReceiverlatitude();
		final double receiverLongtitude = history.getReceiverLongtitude();
		addLocationFeature(features, FeatureType.RECEIVER_LOCATION, receiverLatitude, receiverLongtitude);
	}

	private void addLocationFeature(final List<AtomicFeature<?>> features, final FeatureType type, final double latitude, final double longtitude) {
		final int latSegment = (int) (latitude / UNIT_DEGREE);
		final int longSegment = (int) (longtitude / UNIT_DEGREE);
		final Location location = new Location(latSegment, longSegment);
		TfIdfUtils.addAtomicFeature(features, type, location);
	}

	public static class Location {
		private final int latSegment;
		private final int longSegment;

		public Location(final int latSegment, final int longSegment) {
			this.latSegment = latSegment;
			this.longSegment = longSegment;
		}

		public int getLatSegment() {
			return latSegment;
		}

		public int getLongSegment() {
			return longSegment;
		}

		@Override
		public int hashCode() {
			return latSegment * 31 + longSegment;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof Location) {
				final Location that = (Location) obj;
				return this.latSegment == that.latSegment && this.longSegment == that.longSegment;
			}

			return false;
		}

		@Override
		public String toString() {
			return "(" + latSegment + ", " + longSegment + ")";
		}
	}
}
