package com.duality.server.openfirePlugin.dataTier;

public class NextHistoryInfo {
	public final HistoryEntry history;
	public final long interval;
	public final MessageType type;

	public NextHistoryInfo(final HistoryEntry history, final long interval, final MessageType type) {
		this.history = history;
		this.interval = interval;
		this.type = type;
	}
}
