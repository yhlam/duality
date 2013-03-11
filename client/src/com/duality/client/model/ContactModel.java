package com.duality.client.model;

public class ContactModel {

	private String mName;
	private String mUsername;
	
	public ContactModel(String name, String username){
		mName = name;
		mUsername = username;
	}
	
	public String getName(){
		return mName;
	}
	
	public String getUsername(){
		return mUsername;
	}
	
	public void setName(String name){
		mName = name;
	}
	
	public void setUsername(String username){
		mUsername = username;
	}
}
