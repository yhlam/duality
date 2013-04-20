package com.duality.chatlogImporter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Sqlite2Hsqldb {

	private static final String READ_SQL = "SELECT DATETIME, SENDERNAME, RECIPENTNAME, MESSAGE FROM CHATLOG;";
	private static final String INSERT_SQL = "INSERT INTO DUALITY (TIME, SENDER, RECEIVER, MESSAGE) VALUES (?, ?, ?, ?);";

	public static void main(final String[] args) {
		if (!libCheck()) {
			return;
		}

		final Connection outConn = getOpenfireDbConn(args);
		if (outConn == null) {
			return;
		}

		final Connection inConn = getChatlogDbConn(args);
		if (inConn == null) {
			return;
		}

		final Statement readStatement;
		try {
			readStatement = inConn.createStatement();
		} catch (final SQLException e) {
			System.out.println("Cannot create statement from chat log db.");
			e.printStackTrace();
			return;
		}

		int i = 0;
		try {
			final ResultSet resultSet = readStatement.executeQuery(READ_SQL);
			while (resultSet.next()) {
				final long time = resultSet.getLong(1);
				final String sender = resultSet.getString(2);
				final String receiver = resultSet.getString(3);
				final String message = resultSet.getString(4);

				PreparedStatement insertStmt = null;
				try {
					insertStmt = outConn.prepareStatement(INSERT_SQL);
					insertStmt.setLong(1, time);
					insertStmt.setString(2, sender);
					insertStmt.setString(3, receiver);
					insertStmt.setString(4, message);
					insertStmt.executeUpdate();
				}
				catch (final SQLException e) {
					System.out.println("Failed to insert");
					e.printStackTrace();
					continue;
				} finally {
					if(insertStmt != null) {
						try {
							insertStmt.close();
						}
						catch (final SQLException e) {
							e.printStackTrace();
						} 
					}
				}
				i++;
			}
		} catch (final SQLException e) {
			e.printStackTrace();
			return;
		} finally {
			try {
				if(readStatement != null) {
					readStatement.close();
				}
				inConn.close();
				outConn.close();
			}
			catch (final SQLException e) {
				e.printStackTrace();
			} 
		}

		System.out.println("The import is done. " + i + " entries are imported.");
	}

	private static boolean libCheck() {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (final ClassNotFoundException e) {
			System.out.println("Missing HSQLDB driver");
			return false;
		}

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (final ClassNotFoundException e) {
			System.out.println("Missing SQLite driver");
			return false;
		}

		return true;
	}

	private static Connection getOpenfireDbConn(final String[] args) {
		final String hsqldbPath;
		if (args.length > 0) {
			hsqldbPath = args[0];
		} else {
			System.out.print("Please enter the path to Openfire's HSQLDB: ");
			final Scanner scanner = new Scanner(System.in);
			hsqldbPath = scanner.nextLine();
			scanner.close();
		}
		Connection conn;
		try {
			conn = DriverManager.getConnection("jdbc:hsqldb:" + hsqldbPath, "sa", "");
		} catch (final SQLException e) {
			System.out.println("Cannot open connection to Openfire's HSQLDB");
			e.printStackTrace();
			return null;
		}
		return conn;
	}

	private static Connection getChatlogDbConn(final String[] args) {
		final String filepath;
		if (args.length > 1) {
			filepath = args[1];
		} else {
			filepath = "chatlog.db";
		}

		Connection conn;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + filepath);
		} catch (final SQLException e) {
			System.out.println("Cannot open connection to chat log database");
			e.printStackTrace();
			return null;
		}

		return conn;
	}
}
