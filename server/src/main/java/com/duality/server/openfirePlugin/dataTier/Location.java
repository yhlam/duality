package com.duality.server.openfirePlugin.dataTier;

public class Location {
	private final double latitude;
	private final double longtitude;

	public Location(double latitude, double longtitude) {
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
	public String toString() {
		return "(lat:" + latitude + ", long:" + longtitude + ")";
	}
}
