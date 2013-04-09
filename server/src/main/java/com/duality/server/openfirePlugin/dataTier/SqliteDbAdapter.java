package com.duality.server.openfirePlugin.dataTier;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

public class SqliteDbAdapter extends CachingHistoryDbAdapter {

	private static final String DB_PATH = "chatlog.db";
	private static final String CONNECTION_STRING = "jdbc:sqlite:" + DB_PATH;

	private static final String ORDER_BY = "{ORDER_BY}";
	private static final String SELECT_SQL = "SELECT id, senderName, recipentName, datetime, message FROM chatlog " + "ORDER BY " + ORDER_BY;

	@Override
	protected List<HistoryEntry> loadAllHistory(final OrderAttribute attr, final OrderDirection dir) {
		final String sql = constructSql(attr, dir);

		Connection con = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			con = getConnection();
			statement = con.createStatement();
			rs = statement.executeQuery(sql);

			final List<HistoryEntry> result = readAllFromDb(rs);
			return result;
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			closeConnection(con, statement, rs);
		}

		return Collections.emptyList();
	}

	@Override
	protected HistoryEntry insertIntoDb(final String sender, final String receiver, final Date time, final String message, final Location senderLocation,
			final Location receiverLocation) {
		return null;
	}

	private Connection getConnection() throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		final Connection connection = DriverManager.getConnection(CONNECTION_STRING);
		return connection;
	}

	private void closeConnection(final Connection con, final Statement statement, final ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (final SQLException e) {
				e.printStackTrace();
			}
		}
		if (statement != null) {
			try {
				statement.close();
			} catch (final SQLException e) {
				e.printStackTrace();
			}
		}
		if (con != null) {
			try {
				con.close();
			} catch (final SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private String constructSql(final OrderAttribute attr, final OrderDirection dir) {
		final String orderCol;
		switch (attr) {
		case ID:
			orderCol = "id";
			break;

		case TIME:
			orderCol = "datetime";
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

			final HistoryEntry entry = new HistoryEntry(id, sender, receiver, time, message, null, null);
			result.add(entry);
		}
		return result;
	}
}
