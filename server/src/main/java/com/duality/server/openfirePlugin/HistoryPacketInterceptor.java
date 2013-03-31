package com.duality.server.openfirePlugin;

import java.util.Date;

import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.Location;

public class HistoryPacketInterceptor implements PacketInterceptor {
	private static final HistoryPacketInterceptor INSTANCE = new HistoryPacketInterceptor();

	public static HistoryPacketInterceptor singleton() {
		return INSTANCE;
	}

	@Override
	public void interceptPacket(final Packet packet, final Session session, final boolean incoming, final boolean processed) throws PacketRejectedException {

		if (processed && incoming && packet instanceof Message) {
			final Message message = (Message) packet;
			final String body = message.getBody();
			if (body != null && !"".equals(body)) {
				recordMessage(message);
			}
		}
	}

	private void recordMessage(final Message message) {
		final Date now = new Date();
		final String sender = message.getFrom().toBareJID();
		final String receiver = message.getTo().toBareJID();
		final String messageBody = message.getBody();

		final LocationRepository locationRepo = LocationRepository.singleton();
		final Location senderLocation = locationRepo.getLocation(sender);
		final Location receiverLocation = locationRepo.getLocation(receiver);

		final HistoryDatabaseAdapter historyDatabaseAdapter = HistoryDatabaseAdapter.singleton();
		historyDatabaseAdapter.addHistory(sender, receiver, now, messageBody, senderLocation, receiverLocation);
	}
}
