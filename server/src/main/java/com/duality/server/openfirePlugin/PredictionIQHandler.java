package com.duality.server.openfirePlugin;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.PacketError.Condition;

import com.duality.api.PredictionMessageInfo;
import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;

public class PredictionIQHandler extends IQHandler {

	private static final Logger LOG = Logger.getLogger(PredictionIQHandler.class);
	private static final IQHandlerInfo INFO = new IQHandlerInfo(PredictionMessageInfo.ELEMENT_NAME, PredictionMessageInfo.NAMESPACE);
	private static final PredictionIQHandler INSTANCE = new PredictionIQHandler();

	public static PredictionIQHandler singleton() {
		return INSTANCE;
	}

	private PredictionIQHandler() {
		super("PredictionIQHandler");
	}

	@Override
	public IQ handleIQ(final IQ packet) throws UnauthorizedException {
		final IQ replyPacket = IQ.createResultIQ(packet);
		final IQ.Type type = packet.getType();
		if (type == IQ.Type.set) {
			final Element childElement = packet.getChildElement();
			if (childElement == null) {
				replyPacket.setError(new PacketError(Condition.bad_request, org.xmpp.packet.PacketError.Type.modify,
						"IQ stanzas of type 'get' and 'set' MUST contain one and only one child element (RFC 3920 section 9.2.3)."));
			} else {
				final String namespace = childElement.getNamespaceURI();
				if (PredictionMessageInfo.NAMESPACE.equals(namespace)) {
					final Element receipentElem = childElement.element(PredictionMessageInfo.RECIPTENT);
					if (receipentElem == null) {
						replyPacket.setError(new PacketError(Condition.bad_request, org.xmpp.packet.PacketError.Type.modify,
								"Prediction set package must has a recipent element."));
					} else {
						final String recipent = receipentElem.getTextTrim();
						final JID senderJid = packet.getFrom();
						final String sender = senderJid.toBareJID();
						final Element textElement = childElement.element(PredictionMessageInfo.TEXT);
						final String text = textElement == null ? null : textElement.getTextTrim();
						final Element response = replyPacket.setChildElement(PredictionMessageInfo.ELEMENT_NAME, PredictionMessageInfo.NAMESPACE);
						consturctResponse(sender, recipent, text, response);
					}

				} else {
					replyPacket.setError(Condition.feature_not_implemented);
				}
			}
		} else {
			replyPacket.setError(Condition.feature_not_implemented);
		}

		return replyPacket;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return INFO;
	}

	private void consturctResponse(final String sender, final String recipent, final String text, final Element response) {
		final HistoryDatabaseAdapter historyDbAdapter = HistoryDatabaseAdapter.singleton();
		final HistoryEntry lastHistory = historyDbAdapter.getLastHistoryEntryOfUsers(sender, recipent);

		if (lastHistory != null) {
			final PredictionEngine predictionEngine = PredictionEngine.singleton();
			final long start = System.currentTimeMillis();
			final List<String> predictions = predictionEngine.getPredictions(lastHistory, text);
			final long end = System.currentTimeMillis();
			LOG.info(String.format("Complete query {" + sender + " -> " + recipent + " : " + text +"} in %dms", end - start));

			for (final String prediction : predictions) {
				response.addElement(PredictionMessageInfo.PREDICTION).addText(prediction);
			}
		}
	}

}
