package com.duality.server.openfirePlugin;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

public class DualityPlugin implements Plugin, PacketInterceptor {
	private final Logger LOG = LoggerFactory.getLogger(DualityPlugin.class);

	@Override
	public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
		final InterceptorManager interceptorManager = InterceptorManager.getInstance();
		interceptorManager.addInterceptor(this);
	}

	@Override
	public void destroyPlugin() {
		final InterceptorManager interceptorManager = InterceptorManager.getInstance();
		interceptorManager.removeInterceptor(this);
	}

	@Override
	public void interceptPacket(final Packet packet, final Session session, final boolean incoming, final boolean processed) throws PacketRejectedException {
		if (incoming && !processed && packet instanceof Message) {
			final Message msg = (Message) packet;
			final String body = msg.getBody();
			System.out.println(body);
			LOG.debug(body);
		}
	}
}
