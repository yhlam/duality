package com.duality.xValidation;

public class Prediction {
	private final int testId;
	private final int charGiven;
	private final int rank;
	private final String prediction;

	public Prediction(int testId, int charGiven, int rank, String prediction) {
		this.testId = testId;
		this.charGiven = charGiven;
		this.rank = rank;
		this.prediction = prediction;
	}
	
	public int getTestId() {
		return testId;
	}

	public int getCharGiven() {
		return charGiven;
	}
	
	public int getRank() {
		return rank;
	}

	public String getPrediction() {
		return prediction;
	}
	
	
}
