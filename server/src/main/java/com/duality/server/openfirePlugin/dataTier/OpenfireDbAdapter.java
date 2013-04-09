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

public class OpenfireDbAdapter extends CachingHistoryDbAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(OpenfireDbAdapter.class);

	private static final String INSERT_SQL = "INSERT INTO duality "
			+ "(SENDER, RECEIVER, TIME, MESSAGE, SENDER_LATITUDE, SENDER_LONGITUDE, RECEIVER_LATITUDE, RECEIVER_LONGITUDE) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String IDENTITY_SQL = "CALL IDENTITY()";

	private static final String ORDER_BY = "{ORDER_BY}";

	private static final String SELECT_SQL = "SELECT ID, SENDER, RECEIVER, TIME, MESSAGE, "
			+ "SENDER_LATITUDE, SENDER_LONGITUDE, RECEIVER_LATITUDE, RECEIVER_LONGITUDE " + "FROM duality " + "ORDER BY " + ORDER_BY;

	@Override
	protected List<HistoryEntry> loadAllHistory(final OrderAttribute attr, final OrderDirection dir) {
		final String sql = constructSql(attr, dir);

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
	protected HistoryEntry insertIntoDb(final String sender, final String receiver, final Date time, final String message, final Location senderLocation,
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

	private String constructSql(final OrderAttribute attr, final OrderDirection dir) {
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

		final String sql = SELECT_SQL.replace(ORDER_BY, orderCol + " " + direction);
		return sql;
	}

	private List<HistoryEntry> readAllFromDb(final ResultSet rs) throws SQLException {
		final List<HistoryEntry> result = Lists.newLinkedList();
		while (rs.next()) {
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
}
