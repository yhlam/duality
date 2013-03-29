package com.duality.server.openfirePlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;

public class HistoryPacketInterceptor implements PacketInterceptor {
	private static final HistoryPacketInterceptor INSTANCE = new HistoryPacketInterceptor();

	public static HistoryPacketInterceptor singleton() {
		return INSTANCE;
	}

	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
		
		if(processed && incoming && packet instanceof Message) {
			final Message message = (Message) packet;
			final String body = message.getBody();
			if(body != null && !"".equals(body)) {
				recordMessage(message);
			}
		}
	}

	private void recordMessage(Message message) {
		// TODO (Terry): record the package to database by using HistoryDatabaseAdapter 
		final  HistoryDatabaseAdapter historyDatabaseAdapter = HistoryDatabaseAdapter.singleton();
		
		long id = 0; //default value; should be MAX(ID)+1 from database 
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con= DbConnectionManager.getConnection();
			pstmt = con.prepareStatement("SELECT MAX(ID) FROM DUALITY");
			rs = pstmt.executeQuery();
			if (rs.next()){
				id = rs.getInt(1) + 1;
			}
			else{ //empty table
				id = 0;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			DbConnectionManager.closeConnection(rs,pstmt,con);
		}
		
		
		
		Element contextDataElement = message.getChildElement("contextData", "duality");
		//double senderlatitude = Double.parseDouble(contextDataElement.element("senderlatitude").getTextTrim());
		//double senderlongtitude = Double.parseDouble(contextDataElement.element("senderlongtitude").getTextTrim());
		//double receiverlatitude = Double.parseDouble(contextDataElement.element("receiverlatitude").getTextTrim());
		//double receiverlongtitude = Double.parseDouble(contextDataElement.element("receiverlongtitude").getTextTrim());
		double senderlatitude = 12.1; //hard code
		double senderlongtitude = 13.1; //hard code
		double receiverlatitude = 14; //hard code
		double receiverlongtitude =15; //hard code
		String sender = message.getFrom().toString();
		String receiver = message.getTo().toString();
		String messageBody = message.getBody();
		HistoryEntry historyEntry = new HistoryEntry(id, sender,receiver, new Date(),messageBody, 
				senderlatitude,	senderlongtitude,receiverlatitude,receiverlongtitude);
		
		historyDatabaseAdapter.addHistory(historyEntry);
	}
}
