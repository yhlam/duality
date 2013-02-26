package com.duality.server.openfirePlugin;

import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

public class HistoryPacketInterceptor implements PacketInterceptor {
	private static final HistoryPacketInterceptor INSTANCE = new HistoryPacketInterceptor();

	public static HistoryPacketInterceptor singleton() {
		return INSTANCE;
	}

	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
		if(incoming && packet instanceof Message) {
			final Message message = (Message) packet;
			final String body = message.getBody();
			if(body != null && !"".equals(body)) {
				recordMessage(message);
			}
		}
	}

	private void recordMessage(Message message) {
		// TODO (Terry): record the package to database by using HistoryDatabaseAdapter 
	}
}
