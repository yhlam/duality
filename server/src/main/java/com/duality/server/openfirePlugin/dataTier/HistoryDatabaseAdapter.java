package com.duality.server.openfirePlugin.dataTier;

import java.util.List;

/**
 * HistoryDatabaseAdapter is responsible for read and write the History database
 */
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
		// TODO (Terry): Read the history from database
		return null;
	}
	
	/**
	 * Write a HistoryEntry to database
	 * @param entry HistoryEntry to be written in database
	 */
	public void addHistory(HistoryEntry entry) {
		// TODO (Terry): Write the history entry to database
	}
}
