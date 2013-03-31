package com.duality.server.openfirePlugin.dataTier;

import java.util.Date;

/**
 * An entry of chat in database. It includes sender, receiver, message body, time, location and activity
 */
// TODO (Terry): Reverify the fields base on history database schema, add or remove them if needed
public class HistoryEntry {
	private final int id;
	private final String sender;
	private final String receiver;
	private final Date time;
	private final String message;
	private final Location senderLocation;
	private final Location receiverLocation;

	public HistoryEntry(final int id, final String sender, final String receiver, final Date time, final String message,
			final Location senderLocation, final Location receiverLocation) {
		this.id = id;
		this.sender = sender;
		this.receiver = receiver;
		this.time = time;
		this.message = message;
		this.senderLocation = senderLocation;
		this.receiverLocation = receiverLocation;
	}

	public int getId() {
		return id;
	}

	public String getSender() {
		return sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public Date getTime() {
		return time;
	}

	public String getMessage() {
		return message;
	}
	
	public Location getSenderLocation() {
		return senderLocation;
	}
	
	public Location getReceiverLocation() {
		return receiverLocation;
	}
}
