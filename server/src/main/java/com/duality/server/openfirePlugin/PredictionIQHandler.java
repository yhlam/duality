package com.duality.server.openfirePlugin;

import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;

import com.duality.api.PredictionMessageInfo;

public class PredictionIQHandler extends IQHandler {
	
	private static final IQHandlerInfo INFO = new IQHandlerInfo(PredictionMessageInfo.NAMESPACE, PredictionMessageInfo.ELEMENT_NAME);
	private static final PredictionIQHandler INSTANCE = new PredictionIQHandler();
	
	public static PredictionIQHandler singleton() {
		return INSTANCE;
	}

	private PredictionIQHandler() {
		super("PredictionIQHandler");
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		// TODO (Hei): Handle the custom prediction IQ packet.
		return null;
	}

	@Override
	public IQHandlerInfo getInfo() {
		return INFO;
	}

}
