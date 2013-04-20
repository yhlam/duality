package com.duality.xValidation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class MergeScore {

	private static final String SELECT_SCORE_SQL = "select distinct query, actual, prediction, score " + "from xValidation_query "
			+ "join xValidation_prediction on xValidation_query.id = xValidation_prediction.query_id " + "where score is not null";

	private static final String QUERY_ID_SQL = "select id from xValidation_query where query=? and actual=?";
	private static final String UPDATE_SQL = "update xValidation_prediction set score=? where query_id=? and prediction=?";

	/**
	 * @param args
	 */
	public static void main(final String[] args) throws Exception {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (final ClassNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		final String scroreConnStr = "jdbc:sqlite:db.score.sqlite";
		final String connStr = "jdbc:sqlite:db.limit.sqlite";

		// Making Connection
		final Connection scoreConnection = DriverManager.getConnection(scroreConnStr);
		final Connection connection = DriverManager.getConnection(connStr);

		final Statement scoreStmt = scoreConnection.createStatement();
		final ResultSet rs = scoreStmt.executeQuery(SELECT_SCORE_SQL);
		while (rs.next()) {
			final String query = rs.getString(1);
			final String actual = rs.getString(2);
			final String prediction = rs.getString(3);
			final double score = rs.getDouble(4);

			final PreparedStatement prepareStatement = connection.prepareStatement(QUERY_ID_SQL);
			prepareStatement.setString(1, query);
			prepareStatement.setString(2, actual);
			final ResultSet idRs = prepareStatement.executeQuery();
			if (idRs.next()) {
				final int queryId = idRs.getInt(1);

				final PreparedStatement updateStmt = connection.prepareStatement(UPDATE_SQL);
				updateStmt.setDouble(1, score);
				updateStmt.setInt(2, queryId);
				updateStmt.setString(3, prediction);
				updateStmt.executeUpdate();
			}
		}
	}

}
