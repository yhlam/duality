package com.duality.server.openfirePlugin.prediction;

import java.util.List;
import java.util.Map;

public abstract class PredictionEngine {
	private static PredictionEngine instance = createPredictionEngine();

	/**
	 * @return An instance of PredictionEngine
	 */
	public static PredictionEngine singleton() {
		return instance;
	}

	/**
	 * Create a new instance of PredictionEngine.
	 * It returns different implementation base on configuration
	 * 
	 * @return A new instance of PredictionEngine
	 */
	private static PredictionEngine createPredictionEngine() {
		return null;
	}
	
	/**
	 * @param context An feature to value map
	 * @param incompletedMessage Incompleted message typed by the user
	 * @return List of ranked predictions
	 */
	public abstract List<String> getPredictions(Map<FeatureKey<?>, ?> context, String incompletedMessage);
}
