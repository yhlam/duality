package com.duality.server.openfirePlugin.dataTier;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * HistoryDatabaseAdapter is responsible for read and write the History database
 */
public abstract class HistoryDatabaseAdapter {
	private static final HistoryDatabaseAdapter INSTANCE = new OpenfireDbAdapter();

	/**
	 * @return An instance of HistoryDatabaseAdapter
	 */
	public static HistoryDatabaseAdapter singleton() {
		return INSTANCE;
	}

	protected final List<NewHistoryHandler> handlers;

	protected HistoryDatabaseAdapter() {
		handlers = Lists.newLinkedList();
	}

	public void register(final NewHistoryHandler handler) {
		synchronized (handlers) {
			handlers.add(handler);
		}
	}

	public void deregister(final NewHistoryHandler handler) {
		synchronized (handlers) {
			handlers.remove(handler);
		}
	}

	/**
	 * @return All chat history stored in database
	 */
	public abstract List<HistoryEntry> getAllHistory();

	/**
	 * Get History for a given ID
	 * 
	 * @param id
	 *            ID of a history entry
	 * @return a HistoryEntry with the ID
	 */
	public abstract HistoryEntry getHistoryById(final int id);

	/**
	 * Get the next History Entry of a sender-receiver pair, within a time interval
	 * 
	 * @param id
	 *            The ID of the preceding history entry of the returned history entry
	 * @param timeInterval
	 *            Only return next history entry if it is sent within timeInterval after the currentHistoryEntry is created; in milliseconds
	 * @return the next History Entry if it is within the timeInterval; null otherwise.
	 */
	public abstract HistoryEntry nextHistoryEntry(final int id, final long timeInterval);

	/**
	 * Get the last History Entry of a sender-receiver pair
	 * 
	 * @param user1
	 * @param user2
	 * @return the last HistoryEntry the given user
	 */
	public abstract HistoryEntry getLastHistoryEntryOfUsers(final String user1, final String user2);

	/**
	 * Write a HistoryEntry to database
	 * 
	 * @param sender
	 * @param receiver
	 * @param time
	 * @param message
	 * @param senderLocation
	 * @param receiverLocation
	 */
	public abstract void addHistory(final String sender, final String receiver, final Date time, final String message, final Location senderLocation,
			final Location receiverLocation);
}
