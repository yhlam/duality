package com.chatlogimporter;

public class MessageModel {
	private String name;
	private String message;
	private String date;
	private String time;
	private String ampm;
	
	public MessageModel(String n, String m, String d, String t, String a){
		name = n;
		message = m;
		date = d;
		time = t;
		ampm = a;
	}
	
	public MessageModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getName(){
		return name;
	}
	
	public String getMessage(){
		return message;
	}
	
	public String getDate(){
		return date;
	}
	
	public String getTime(){
		return time;
	}
	
	public String getAmpm(){
		return ampm;
	}
	
	public void setName(String n){
		name = n;
	}
	
	public void setDate(String d){
		date = d;
	}
	
	public void setTime(String t){
		time = t;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setAmpm(String ampm) {
		this.ampm = ampm;
	}
	
	
}

