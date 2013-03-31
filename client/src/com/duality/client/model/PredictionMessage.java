package com.duality.client.model;

import org.jivesoftware.smack.packet.IQ;

import com.duality.api.PredictionMessageInfo;

public class PredictionMessage extends IQ {
	private final String text;
	private final String recipent;
	
	public PredictionMessage(final String from, final String to, String text, String recipent) {
		this.text = text;
		this.recipent = recipent;
		setFrom(from);
		setTo(to);
		setType(Type.SET);
	}
	
	public String getText() {
		return text;
	}
	
	public String getRecipent() {
		return recipent;
	}

	@Override
	public String getChildElementXML() {
		final String xml = "<" + PredictionMessageInfo.ELEMENT_NAME + " xmlns='" + PredictionMessageInfo.NAMESPACE + "'>"
				+ "<" + PredictionMessageInfo.TEXT + ">" + text + "</" + PredictionMessageInfo.TEXT + ">"
				+ "<" + PredictionMessageInfo.RECIPTENT + ">" + recipent + "</" + PredictionMessageInfo.RECIPTENT + ">"
				+ "</" + PredictionMessageInfo.ELEMENT_NAME + ">";
		return xml;
	}
}
