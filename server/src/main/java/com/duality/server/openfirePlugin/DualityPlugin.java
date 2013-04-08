package com.duality.server.openfirePlugin;

import java.io.File;

import org.apache.log4j.Logger;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;

import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore;

public class DualityPlugin implements Plugin {
	
	private static final Logger LOG = Logger.getLogger(DualityPlugin.class);
	
	private void initialize() {
		final long start = System.currentTimeMillis();
		LOG.info("Initializing Duality Plugin...");
		TfIdfStore.singleton();
		final long end = System.currentTimeMillis();
		final double time = (end - start) / 1000.0 / 60.0;
		LOG.info(String.format("Initialization of Duality Plugin complete in %.2f minutes", time));
	}

	@Override
	public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
		initialize();
		
		// Register HistoryPacketInterceptor
		final HistoryPacketInterceptor historyPacketInterceptor = HistoryPacketInterceptor.singleton();
		InterceptorManager.getInstance().addInterceptor(historyPacketInterceptor);
		
		// Register PredictionIQHandler
		final XMPPServer xmppServer = XMPPServer.getInstance();
		final IQRouter iqRouter = xmppServer.getIQRouter();
		final PredictionIQHandler predictionIQHandler = PredictionIQHandler.singleton();
		iqRouter.addHandler(predictionIQHandler);
		
		// Register LocationRepository
		final LocationRepository locationRepo = LocationRepository.singleton();
		iqRouter.addHandler(locationRepo);
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
		
		// Deregister LocationRepository
		final LocationRepository locationRepo = LocationRepository.singleton();
		iqRouter.removeHandler(locationRepo);
	}
}
