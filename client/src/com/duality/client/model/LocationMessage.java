package com.duality.client.model;

import org.jivesoftware.smack.packet.IQ;

import com.duality.api.LocationMessageInfo;

public class LocationMessage extends IQ {

	private final double mLongitude;
	private final double mLatitude;

	public LocationMessage(final String from, final String to, final double lon, final double lat) {
		mLongitude = lon;
		mLatitude = lat;
		setFrom(from);
		setTo(to);
		setType(Type.SET);
	}

	public double getLongitude() {
		return mLongitude;
	}

	public double getLatitude() {
		return mLatitude;
	}

	@Override
	public String getChildElementXML() {
		final String xml = "<" + LocationMessageInfo.ELEMENT_NAME + " xmlns='" + LocationMessageInfo.NAMESPACE + "'>"
				+ "<" + LocationMessageInfo.LATITUDE + ">" + mLatitude + "</" + LocationMessageInfo.LATITUDE + "> "
				+ "<" + LocationMessageInfo.LONGITUDE + ">" + mLongitude + "</"	+ LocationMessageInfo.LONGITUDE + "> "
				+ "</" + LocationMessageInfo.ELEMENT_NAME + ">";
		return xml;
	}

}
