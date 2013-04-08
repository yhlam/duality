package com.duality.server.openfirePlugin.dataTier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

public class OpenfireDbAdapter extends HistoryDatabaseAdapter {

	private static final String INSERT_SQL = "INSERT INTO duality "
			+ "(SENDER, RECEIVER, TIME, MESSAGE, SENDER_LATITUDE, SENDER_LONGITUDE, RECEIVER_LATITUDE, RECEIVER_LONGITUDE) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String IDENTITY_SQL = "CALL IDENTITY()";

	private static final String ORDER_BY = "{ORDER_BY}";

	private static final String SELECT_ALL_SQL = "SELECT ID, SENDER, RECEIVER, TIME, MESSAGE, "
			+ "SENDER_LATITUDE, SENDER_LONGITUDE, RECEIVER_LATITUDE, RECEIVER_LONGITUDE " + "FROM duality " + "ORDER BY " + ORDER_BY;

	private static final Logger LOG = LoggerFactory.getLogger(OpenfireDbAdapter.class);

	private final Object cacheLock;
	private List<HistoryEntry> cache;
	private RangeMap<Integer, Integer> offsets;
	private List<HistoryAndInterval> nextEntry;
	private Map<ConversationID, HistoryEntry> openConversations;

	OpenfireDbAdapter() {
		cacheLock = new Object();
		refresh();
	}

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
						final HistoryAndInterval historyAndInterval = new HistoryAndInterval(historyEntry, interval);

						final int index = getIndex(openEntry);
						nextEntry.set(index, historyAndInterval);
					}
				}
			} else {
				offsets.put(Range.<Integer> all(), 0);
			}

		}
	}

	private List<HistoryEntry> loadAllHistory(final OrderAttribute attr, final OrderDirection dir) {
		final String orderCol;
		switch (attr) {
		case ID:
			orderCol = "ID";
			break;
		case TIME:
			orderCol = "TIME";
			break;

		default:
			throw new RuntimeException("Unsupported OrderAttribute: " + attr);
		}

		final String direction;
		switch (dir) {
		case ASC:
			direction = "ASC";
			break;
		case DESC:
			direction = "DESC";
			break;

		default:
			throw new RuntimeException("Unsupported OrderDirection: " + dir);
		}

		final String sql = SELECT_ALL_SQL.replace(ORDER_BY, orderCol + " " + direction);

		Connection con = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			statement = con.createStatement();
			rs = statement.executeQuery(sql);

			final List<HistoryEntry> result = readAllFromDb(rs);
			return result;
		} catch (final SQLException e) {
			LOG.error("Failed to get all history", e);
		} finally {
			DbConnectionManager.closeConnection(rs, statement, con);
		}

		return Collections.emptyList();
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
	public HistoryEntry nextHistoryEntry(final int id, final long timeInterval) {
		synchronized (cacheLock) {
			final HistoryAndInterval historyAndInterval = getValueById(id, nextEntry);
			if (historyAndInterval == null) {
				return null;
			}
			final HistoryEntry history = historyAndInterval.getHistoryIfWithin(timeInterval);
			return history;
		}
	}

	@Override
	public HistoryEntry nextHistoryEntry(final HistoryEntry currentHistoryEntry, final long timeInterval) {
		final int id = currentHistoryEntry.getId();
		return nextHistoryEntry(id, timeInterval);
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
				final HistoryAndInterval historyAndInterval = new HistoryAndInterval(historyEntry, interval);

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

	private HistoryEntry insertIntoDb(final String sender, final String receiver, final Date time, final String message, final Location senderLocation,
			final Location receiverLocation) {
		final long timestamp = time.getTime();

		final int id;
		Connection con = null;
		try {
			con = DbConnectionManager.getConnection();
			con.setAutoCommit(false);

			final PreparedStatement st = con.prepareStatement(INSERT_SQL);
			st.setString(1, sender);
			st.setString(2, receiver);
			st.setLong(3, timestamp);
			st.setString(4, message);

			if (senderLocation != null) {
				final double latitude = senderLocation.getLatitude();
				final double longtitude = senderLocation.getLongtitude();
				st.setDouble(5, latitude);
				st.setDouble(6, longtitude);
			} else {
				st.setNull(5, Types.DOUBLE);
				st.setNull(6, Types.DOUBLE);
			}

			if (receiverLocation != null) {
				final double latitude = receiverLocation.getLatitude();
				final double longtitude = receiverLocation.getLongtitude();
				st.setDouble(7, latitude);
				st.setDouble(8, longtitude);
			} else {
				st.setNull(7, Types.DOUBLE);
				st.setNull(8, Types.DOUBLE);
			}
			st.executeUpdate();

			final Statement idStatement = con.createStatement();
			final ResultSet resultSet = idStatement.executeQuery(IDENTITY_SQL);
			if (resultSet.next()) {
				id = resultSet.getInt(1);
			} else {
				LOG.error("Should never happens! Cannot get return from CALL IDENTITY()");
				return null;
			}

			con.commit();

		} catch (final SQLException e) {
			LOG.error("Error in insert history", e);
			return null;
		} finally {
			DbConnectionManager.closeConnection(con);
		}

		final HistoryEntry historyEntry = new HistoryEntry(id, sender, receiver, time, message, senderLocation, receiverLocation);
		return historyEntry;
	}

	private List<HistoryEntry> readAllFromDb(final ResultSet rs) throws SQLException {
		final List<HistoryEntry> result = Lists.newLinkedList();
		while (rs.next()) {
			final HistoryEntry entry = readFromDb(rs);
			result.add(entry);
		}
		return result;
	}

	private HistoryEntry readFromDb(final ResultSet rs) throws SQLException {
		// make the list of HistoryEntry
		final int id = rs.getInt(1);
		final String sender = rs.getString(2);
		final String receiver = rs.getString(3);
		final long timestamp = rs.getLong(4);
		final Date time = new Date(timestamp);
		final String message = rs.getString(5);

		final Location senderLocation = getLocation(rs, 6, 7);
		final Location receiverLocation = getLocation(rs, 8, 9);

		final HistoryEntry entry = new HistoryEntry(id, sender, receiver, time, message, senderLocation, receiverLocation);
		return entry;
	}

	private Location getLocation(final ResultSet rs, final int latCol, final int longCol) throws SQLException {
		final double latitude = rs.getDouble(latCol);
		final boolean nullLatitude = rs.wasNull();

		final double longitude = rs.getDouble(longCol);
		final boolean nullLongitude = rs.wasNull();

		if (nullLatitude || nullLongitude) {
			return null;
		} else {
			final Location location = new Location(latitude, longitude);
			return location;
		}
	}

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

	private static enum OrderAttribute {
		ID, TIME
	}

	private static enum OrderDirection {
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

	private static class HistoryAndInterval {
		private final HistoryEntry history;
		private final long interval;

		public HistoryAndInterval(final HistoryEntry history, final long interval) {
			this.history = history;
			this.interval = interval;
		}

		public HistoryEntry getHistoryIfWithin(final long interval) {
			if (this.interval <= interval) {
				return history;
			}

			return null;
		}
	}
}
