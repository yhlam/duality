package com.duality.server.openfirePlugin.dataTier;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

public abstract class CachingHistoryDbAdapter extends HistoryDatabaseAdapter {

	private final Object cacheLock;
	private List<HistoryEntry> cache;
	private RangeMap<Integer, Integer> offsets;
	private List<NextHistoryInfo> nextEntry;
	private Map<ConversationID, HistoryEntry> openConversations;

	protected CachingHistoryDbAdapter() {
		cacheLock = new Object();
		refresh();
	}

	protected abstract List<HistoryEntry> loadAllHistory(final OrderAttribute attr, final OrderDirection dir);

	public void refresh() {
		synchronized (cacheLock) {
			final List<HistoryEntry> allHistories = loadAllHistory(OrderAttribute.ID, OrderDirection.ASC);
			final int size = allHistories.size();

			cache = Lists.newArrayListWithExpectedSize(size);
			cache.addAll(allHistories);

			offsets = TreeRangeMap.create();

			if (size > 0) {
				// Initialize offsets
				final HistoryEntry firstHistory = allHistories.get(0);
				int offset = -firstHistory.getId();
				final int lower = 0;
				int count = 0;

				for (final HistoryEntry history : allHistories) {
					final int id = history.getId();
					final int offsetAdj = id + offset - count;
					if (offsetAdj != 0) {
						offsets.put(Range.closedOpen(lower, count), offset);
						offset = offset + offsetAdj;
					}
					count++;
				}
				offsets.put(Range.atLeast(lower), offset);

				// Initialize nextEntry
				nextEntry = Lists.newArrayListWithExpectedSize(size);
				for (int i = 0; i < size; i++) {
					nextEntry.add(null);
				}

				openConversations = Maps.newHashMap();
				final List<HistoryEntry> historiesOrderedByTime = loadAllHistory(OrderAttribute.TIME, OrderDirection.ASC);
				for (final HistoryEntry historyEntry : historiesOrderedByTime) {
					final String sender = historyEntry.getSender();
					final String receiver = historyEntry.getReceiver();
					final ConversationID conversationID = new ConversationID(sender, receiver);
					final HistoryEntry openEntry = openConversations.put(conversationID, historyEntry);
					if (openEntry != null) {
						final Date startTime = openEntry.getTime();
						final Date endTime = historyEntry.getTime();
						final long startTimestamp = startTime.getTime();
						final long endTimestamp = endTime.getTime();
						final long interval = endTimestamp - startTimestamp;
						final String lastSender = openEntry.getSender();
						final MessageType type = sender.equals(lastSender) ? MessageType.SUPPLEMENT : MessageType.REPLY;
						final NextHistoryInfo historyAndInterval = new NextHistoryInfo(historyEntry, interval, type);

						final int index = getIndex(openEntry);
						nextEntry.set(index, historyAndInterval);
					}
				}
			} else {
				offsets.put(Range.<Integer> all(), 0);
			}

		}
	}

	@Override
	public List<HistoryEntry> getAllHistory() {
		synchronized (cacheLock) {
			return cache;
		}
	}

	@Override
	public HistoryEntry getHistoryById(final int id) {
		synchronized (cacheLock) {
			return getValueById(id, cache);
		}
	}

	@Override
	public HistoryEntry nextHistoryEntry(final int id, final long timeInterval, final MessageType type) {
		long nextTimeInterval = timeInterval;
		int nextId = id;
		synchronized (cacheLock) {
			while (nextTimeInterval >= 0) {
				final NextHistoryInfo historyInfo = getValueById(nextId, nextEntry);
				if (historyInfo == null) {
					return null;
				}

				if (historyInfo.type == type && historyInfo.interval <= timeInterval) {
					return historyInfo.history;
				}

				if (type == MessageType.SUPPLEMENT && historyInfo.type == MessageType.REPLY) {
					return null;
				}

				nextId = historyInfo.history.getId();
				nextTimeInterval -= historyInfo.interval;
			}
		}

		return null;
	}

	@Override
	public HistoryEntry getLastHistoryEntryOfUsers(final String user1, final String user2) {
		final ConversationID conversationID = new ConversationID(user1, user2);
		synchronized (cacheLock) {
			final HistoryEntry historyEntry = openConversations.get(conversationID);
			return historyEntry;
		}
	}

	@Override
	public void addHistory(final String sender, final String receiver, final Date time, final String message, final Location senderLocation,
			final Location receiverLocation) {
		final HistoryEntry historyEntry = insertIntoDb(sender, receiver, time, message, senderLocation, receiverLocation);
		if (historyEntry == null) {
			return;
		}

		synchronized (cacheLock) {
			final int id = historyEntry.getId();
			final int expectedIndex = cache.size();
			final int index = getIndex(id);
			if (expectedIndex != index) {
				final int realOffset = expectedIndex - id;
				offsets.put(Range.atLeast(id), realOffset);
			}

			cache.add(historyEntry);
			nextEntry.add(null);

			final ConversationID conId = new ConversationID(sender, receiver);
			final HistoryEntry lastEntry = openConversations.put(conId, historyEntry);
			if (lastEntry != null) {
				final Date startTime = lastEntry.getTime();
				final Date endTime = historyEntry.getTime();
				final long startTimestamp = startTime.getTime();
				final long endTimestamp = endTime.getTime();
				final long interval = endTimestamp - startTimestamp;
				final String lastSender = lastEntry.getSender();
				final MessageType type = sender.equals(lastSender) ? MessageType.SUPPLEMENT : MessageType.REPLY;
				final NextHistoryInfo historyAndInterval = new NextHistoryInfo(historyEntry, interval, type);

				final int nextEntryId = getIndex(lastEntry);
				nextEntry.set(nextEntryId, historyAndInterval);
			}

		}

		synchronized (handlers) {
			for (final NewHistoryHandler handler : handlers) {
				handler.onNewHistory(historyEntry);
			}
		}
	}

	private int getIndex(final HistoryEntry entry) {
		final int id = entry.getId();
		return getIndex(id);
	}

	private int getIndex(final int id) {
		final Integer offset = offsets.get(id);
		return id + offset;
	}

	protected abstract HistoryEntry insertIntoDb(final String sender, final String receiver, final Date time, final String message,
			final Location senderLocation, final Location receiverLocation);

	private <T> T getValueById(final int id, final List<T> list) {
		synchronized (cacheLock) {
			final Integer offset = offsets.get(id);
			if (offset == null) {
				return null;
			}

			final int index = id + offset;
			final T value = list.get(index);
			return value;
		}
	}

	public static enum OrderAttribute {
		ID, TIME
	}

	public static enum OrderDirection {
		ASC, DESC
	}

	private static class ConversationID {
		private final String user1;
		private final String user2;
		private final int hash;

		public ConversationID(final String user1, final String user2) {
			this.user1 = user1;
			this.user2 = user2;
			this.hash = user1.hashCode() + user2.hashCode();
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof ConversationID) {
				final ConversationID that = (ConversationID) obj;
				return (this.user1.equals(that.user1) && this.user2.equals(that.user2)) || (this.user1.equals(that.user2) && this.user2.equals(that.user1));
			}

			return false;
		}
	}

	private static class NextHistoryInfo {
		public final HistoryEntry history;
		public final long interval;
		public final MessageType type;

		public NextHistoryInfo(final HistoryEntry history, final long interval, final MessageType type) {
			this.history = history;
			this.interval = interval;
			this.type = type;
		}
	}
}
