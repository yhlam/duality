package com.duality.server.openfirePlugin.dataTier;

import java.util.List;

/**
 * HistoryDatabaseAdapter is responsible for read and write the History database
 */
// TODO (Terry): Think about do we need to build a cache to improve performance
public class HistoryDatabaseAdapter {
	private static final HistoryDatabaseAdapter INSTANCE = new HistoryDatabaseAdapter();

	/**
	 * @return An instance of HistoryDatabaseAdapter
	 */
	public static HistoryDatabaseAdapter singleton() {
		return INSTANCE;
	}

	private HistoryDatabaseAdapter() {
	}

	/**
	 * @return All chat history stored in database
	 */
	public List<HistoryEntry> getAllHistory() {
		// TODO (Terry): Read the history from database or cache
		return null;
	}

	/**
	 * Get History for a given ID
	 * 
	 * @param id
	 *            ID of a history entry
	 * @return a HistoryEntry with the ID
	 */
	public HistoryEntry getHistoryById(final long id) {
		// TODO (Terry): Get the corresponding ID from database or cache
		return null;
	}

	/**
	 * Write a HistoryEntry to database
	 * 
	 * @param entry
	 *            HistoryEntry to be written in database
	 */
	public void addHistory(final HistoryEntry entry) {
		// TODO (Terry): Write the history entry to database
	}
}
