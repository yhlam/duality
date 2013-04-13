package com.duality.server.openfirePlugin.prediction;

import java.util.List;
import java.util.Map;

import com.duality.server.openfirePlugin.InstanceLoader;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.dataTier.MessageType;

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
		final InstanceLoader instanceLoader = InstanceLoader.singleton();
		return instanceLoader.createInstance(PredictionEngine.class);
	}

	/**
	 * @param entry The history entry that define user context
	 * @param incompletedMessage Incompleted message typed by the user
	 * @return List of ranked predictions
	 */
	public abstract List<String> getPredictions(HistoryEntry entry, String incompletedMessage, final MessageType type);
	
	/**
	 * @param context An feature to value map
	 * @param incompletedMessage Incompleted message typed by the user
	 * @return List of ranked predictions
	 */
	public abstract List<String> getPredictions(Map<FeatureKey<?>, Object> context, String incompletedMessage, MessageType type);
}
