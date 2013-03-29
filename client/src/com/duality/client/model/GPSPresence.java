package com.duality.client.model;

import org.jivesoftware.smack.packet.Presence;


public class GPSPresence extends Presence{

	private double mAltitude;
	private double mLongitude;
	private double mLatitude;
	private long mTime;

	public GPSPresence(Presence.Type type){
		super(type);
	}

	public GPSPresence(double a, double lon, double lat, long time, Presence.Type type){
		super(type);
		mAltitude = a;
		mLongitude = lon;
		mLatitude = lat;
		mTime = time;
	}
	
	public void setAltitude(double altitude){
		mAltitude = altitude;
	}
	
	public void setLongitude(double longitude){
		mLongitude = longitude;
	}
	
	public void setLatitude(double latitude){
		mLatitude = latitude;
	}
	
	public void setTime(long time){
		mTime = time;
	}
	
	public double getAltitude(){
		return mAltitude;
	}
	
	public double getLongitude(){
		return mLongitude;
	}
	
	public double getLatitude(){
		return mLatitude;
	}
	
	public long getTime(){
		return mTime;
	}
	
	@Override
	public String toXML() {
		// TODO Auto-generated method stub
		return super.toXML();
	}

}
