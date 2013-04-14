package com.duality.server.xValidation;

public class Prediction {
	private final int charGiven;
	private final String prediction;

	public Prediction(int charGiven, String prediction) {
		this.charGiven = charGiven;
		this.prediction = prediction;
	}

	public int getCharGiven() {
		return charGiven;
	}
	public String getPrediction() {
		return prediction;
	}
	
	
}
