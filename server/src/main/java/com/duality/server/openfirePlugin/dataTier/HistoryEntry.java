package com.duality.server.openfirePlugin.dataTier;

import java.util.Date;

/**
 * An entry of chat in database. It includes sender, receiver, message body, time, location and activity
 */
// TODO (Terry): Reverify the fields base on history database schema, add or remove them if needed
public class HistoryEntry {
	private final long id;
	private final String sender;
	private final String receiver;
	private final Date time;
	private final String message;
	private final double senderlatitude;
	private final double senderLongtitude;
	private final double receiverlatitude;
	private final double receiverLongtitude;

	public HistoryEntry(final long id, final String sender, final String receiver, final Date time, final String message, final double senderlatitude,
			final double senderLongtitude, final double receiverlatitude, final double receiverLongtitude) {
		this.id = id;
		this.sender = sender;
		this.receiver = receiver;
		this.time = time;
		this.message = message;
		this.senderlatitude = senderlatitude;
		this.senderLongtitude = senderLongtitude;
		this.receiverlatitude = receiverlatitude;
		this.receiverLongtitude = receiverLongtitude;
	}

	public long getId() {
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

	public double getSenderlatitude() {
		return senderlatitude;
	}

	public double getSenderLongtitude() {
		return senderLongtitude;
	}

	public double getReceiverlatitude() {
		return receiverlatitude;
	}

	public double getReceiverLongtitude() {
		return receiverLongtitude;
	}

}
