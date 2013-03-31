package com.duality.server.openfirePlugin;

import java.util.Map;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.PacketError.Condition;

import com.duality.api.LocationMessageInfo;
import com.duality.server.openfirePlugin.dataTier.Location;
import com.google.common.collect.Maps;

public class LocationRepository extends IQHandler {
	private static final IQHandlerInfo INFO = new IQHandlerInfo(LocationMessageInfo.ELEMENT_NAME, LocationMessageInfo.NAMESPACE);
	private static final LocationRepository INSTANCE = new LocationRepository();

	public static LocationRepository singleton() {
		return INSTANCE;
	}

	private final Map<String, Location> locations;

	public LocationRepository() {
		super("LocationRepository");
		locations = Maps.newConcurrentMap();
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
				if (LocationMessageInfo.NAMESPACE.equals(namespace)) {
					final Element latitudeElem = childElement.element(LocationMessageInfo.LATITUDE);
					final Element longitudeElem = childElement.element(LocationMessageInfo.LONGITUDE);
					if (latitudeElem == null || longitudeElem == null) {
						replyPacket.setError(new PacketError(Condition.bad_request, org.xmpp.packet.PacketError.Type.modify,
								"Location package must have latitude and longitude elements."));
					} else {
						final JID jid = packet.getFrom();
						final String username = jid.toBareJID();
						final String latitudeStr = latitudeElem.getTextTrim();
						final String longitudeStr = longitudeElem.getTextTrim();
						try {
							final double latitude = Double.parseDouble(latitudeStr);
							final double longitude = Double.parseDouble(longitudeStr);
							final Location location = new Location(latitude, longitude);
							locations.put(username, location);
						} catch (final NumberFormatException e) {
							replyPacket.setError(new PacketError(Condition.bad_request, org.xmpp.packet.PacketError.Type.modify,
									"Invalid latitude or longitude"));
						}
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

	public Location getLocation(final String username) {
		final Location location = locations.get(username);
		return location;
	}
}
