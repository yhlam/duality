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

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

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

	private static final String INSERT_SQL = "INSERT INTO duality "
			+ "(SENDER, RECEIVER, TIME, MESSAGE, SENDER_LATITUDE, SENDER_LONGITUDE, RECEIVER_LATITUDE, RECEIVER_LONGITUDE) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String IDENTITY_SQL = "CALL IDENTITY()";

	private static final String SELECT_ALL_SQL = "SELECT ID, SENDER, RECEIVER, TIME, MESSAGE, SENDER_LATITUDE, SENDER_LONGITUDE, RECEIVER_LATITUDE, RECEIVER_LONGITUDE FROM duality";
	private static final String SELECT_BY_ID_SQL = SELECT_ALL_SQL + " WHERE ID = ?";

	private static final Logger LOG = LoggerFactory.getLogger(HistoryDatabaseAdapter.class);

	private final List<NewHistoryHandler> handlers;

	private HistoryDatabaseAdapter() {
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
	public List<HistoryEntry> getAllHistory() {

		Connection con = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			statement = con.createStatement();
			rs = statement.executeQuery(SELECT_ALL_SQL);

			final List<HistoryEntry> result = readFromDb(rs);
			return result;
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			DbConnectionManager.closeConnection(rs, statement, con);
		}

		return Collections.emptyList();
	}

	private List<HistoryEntry> readFromDb(final ResultSet rs) throws SQLException {
		final List<HistoryEntry> result = Lists.newLinkedList();
		while (rs.next()) {
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
			result.add(entry);
		}
		return result;
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

	/**
	 * Get History for a given ID
	 * 
	 * @param id ID of a history entry
	 * @return a HistoryEntry with the ID
	 */
	public HistoryEntry getHistoryById(final int id) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(SELECT_BY_ID_SQL);
			pstmt.setInt(1, id);
			rs = pstmt.executeQuery();

			final List<HistoryEntry> result = readFromDb(rs);
			if (result.isEmpty()) {
				return null;
			} else {
				final HistoryEntry firstResult = result.get(0);
				return firstResult;
			}
		} catch (final SQLException e) {
			LOG.error("Failed to get history of ID: " + id, e);
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}

		return null;
	}

	/**
	 * Write a HistoryEntry to database
	 * 
	 * @param entry HistoryEntry to be written in database
	 */
	public void addHistory(final String sender, final String receiver, final Date time, final String message, final Location senderLocation,
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
				return;
			}

			con.commit();

		} catch (final SQLException e) {
			LOG.error("Error in insert history");
			return;
		} finally {
			DbConnectionManager.closeConnection(con);
		}

		final HistoryEntry historyEntry = new HistoryEntry(id, sender, receiver, time, message, senderLocation, receiverLocation);
		synchronized (handlers) {
			for (final NewHistoryHandler handler : handlers) {
				handler.onNewHistory(historyEntry);
			}
		}
	}

}
