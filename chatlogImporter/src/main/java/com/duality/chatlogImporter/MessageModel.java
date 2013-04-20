package com.duality.chatlogImporter;

public class MessageModel {
	private String senderName;
	private String recipentName;
	private String message;
	private String dateTime;
	
	public MessageModel(String n, String r, String m, String d){
		senderName = n;
		recipentName = r;
		message = m;
		dateTime = d;

	}
	
	public MessageModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getRecipentName() {
		return recipentName;
	}

	public void setRecipentName(String recipentName) {
		this.recipentName = recipentName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDateTime() {
		return dateTime;
	}

	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
}

