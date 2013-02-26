package com.duality.server.openfirePlugin;

import java.io.File;

import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;

public class DualityPlugin implements Plugin {

	@Override
	public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
		// Register HistoryPacketInterceptor
		final HistoryPacketInterceptor historyPacketInterceptor = HistoryPacketInterceptor.singleton();
		InterceptorManager.getInstance().addInterceptor(historyPacketInterceptor);
		
		// Register PredictionIQHandler
		final XMPPServer xmppServer = XMPPServer.getInstance();
		final IQRouter iqRouter = xmppServer.getIQRouter();
		final PredictionIQHandler predictionIQHandler = PredictionIQHandler.singleton();
		iqRouter.addHandler(predictionIQHandler);
	}

	@Override
	public void destroyPlugin() {
		// Degister HistoryPacketInterceptor
		final HistoryPacketInterceptor historyPacketInterceptor = HistoryPacketInterceptor.singleton();
		final InterceptorManager interceptorManager = InterceptorManager.getInstance();
		interceptorManager.removeInterceptor(historyPacketInterceptor);
		
		// Deregister PredictionIQHandler
		final XMPPServer xmppServer = XMPPServer.getInstance();
		final IQRouter iqRouter = xmppServer.getIQRouter();
		final PredictionIQHandler predictionIQHandler = PredictionIQHandler.singleton();
		iqRouter.removeHandler(predictionIQHandler);
	}
}
