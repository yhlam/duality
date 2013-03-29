package com.duality.server.openfirePlugin.dataTier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;

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
		List<HistoryEntry> result = new ArrayList<HistoryEntry>();
		
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con= DbConnectionManager.getConnection();
			pstmt = con.prepareStatement("SELECT * FROM duality");
			rs = pstmt.executeQuery();
			while (rs.next()){
				//make the list of HistoryEntry
				
				long id=rs.getLong("ID");
				String sender = rs.getString("SENDER");
				String receiver=rs.getString("RECEIVER");
				Date time = new Date (rs.getLong("TIME"));
				String message = rs.getString("MESSAGE");
				double senderlatitude = rs.getLong("SENDER_LATITUDE");
				double senderlongitude = rs.getLong("SENDER_LONGITUDE");;
				double receiverlatitude = rs.getLong("RECEIVER_LATITUDE");;
				double receiverlongitude = rs.getLong("RECEIVER_LONGITUDE");;
				HistoryEntry entry = new HistoryEntry(id,sender,receiver,time,message,senderlatitude,senderlongitude,receiverlatitude,receiverlongitude);
				result.add(entry);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			DbConnectionManager.closeConnection(rs,pstmt,con);
		}
		
		return result;
	}

	/**
	 * Get History for a given ID
	 * @param id ID of a history entry
	 * @return a HistoryEntry with the ID
	 */
	public HistoryEntry getHistoryById(long id) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con= DbConnectionManager.getConnection();
			pstmt = con.prepareStatement("SELECT * FROM duality WHERE ID=" + id);
			rs = pstmt.executeQuery();
			if (rs.next()){
				long entryId=rs.getLong("ID");
				String sender = rs.getString("SENDER");
				String receiver=rs.getString("RECEIVER");
				Date time = new Date (rs.getLong("TIME"));
				String message = rs.getString("MESSAGE");
				double senderlatitude = rs.getLong("SENDER_LATITUDE");
				double senderlongitude = rs.getLong("SENDER_LONGITUDE");;
				double receiverlatitude = rs.getLong("RECEIVER_LATITUDE");;
				double receiverlongitude = rs.getLong("RECEIVER_LONGITUDE");;
				HistoryEntry entry = new HistoryEntry(id,sender,receiver,time,message,senderlatitude,senderlongitude,receiverlatitude,receiverlongitude);

				return entry;
			}
			else{
				return null;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			DbConnectionManager.closeConnection(rs,pstmt,con);
		}
		
		
		return null;
	}

	/**
	 * Write a HistoryEntry to database
	 * @param entry HistoryEntry to be written in database
	 */
	public void addHistory(HistoryEntry entry) {
		Connection con = null;
		
		long time = entry.getTime().getTime();
		
		try {
			con= DbConnectionManager.getConnection();
			String INSERT_SQL = "INSERT INTO duality (ID,SENDER,RECEIVER,TIME,MESSAGE,SENDER_LATITUDE,SENDER_LONGITUDE,RECEIVER_LATITUDE,RECEIVER_LONGITUDE) VALUES (?,?,?,?,?,?,?,?,?)";
			PreparedStatement st = con.prepareStatement(INSERT_SQL);
			st.setLong(1, entry.getId());
			st.setString(2,entry.getSender());
			st.setString(3,entry.getReceiver());
			st.setLong(4,time);
			st.setString(5,entry.getMessage());
			st.setDouble(6, entry.getSenderlatitude());
			st.setDouble(7, entry.getSenderLongtitude());
			st.setDouble(8, entry.getReceiverlatitude());
			st.setDouble(9, entry.getReceiverlatitude());
			st.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			DbConnectionManager.closeConnection(con);
		}

	}
	
}
