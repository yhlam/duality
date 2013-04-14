package com.duality.server.xValidation;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.duality.server.openfirePlugin.InstanceLoader;
import com.duality.server.openfirePlugin.dataTier.CachingHistoryDbAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.dataTier.MessageType;
import com.duality.server.openfirePlugin.dataTier.NextHistoryInfo;
import com.duality.server.openfirePlugin.dataTier.SqliteDbAdapter;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.duality.server.openfirePlugin.prediction.impl.feature.TfIdfKey;
import com.duality.server.openfirePlugin.prediction.impl.store.FPStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfFeatureStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;


public class CrossValidation {

	static final int N_FOLD = 10;
	private static final long MAX_DEAD_AIR_INTERVAL = 60 * 60 * 1000L;
	private static final String INSERT_TEST_SQL = "INSERT INTO xValidation_test (name) VALUES (?)";
	private static final String INSERT_QUERY_SQL = "INSERT INTO xValidation_query (test_id, query, actual) VALUES (?, ?, ?)";
	private static final String INSERT_PREDICTION_SQL = "INSERT INTO xValidation_prediction (query_id, char_given, prediction) VALUES (?, ?, ?)";
	private static final String ID_SQL = "SELECT last_insert_rowid()";

	private static void initialize() {
		final InstanceLoader instanceLoader = InstanceLoader.singleton();
		instanceLoader.setBinding(CachingHistoryDbAdapter.class, SqliteDbAdapter.class);
		instanceLoader.setBinding(HistoryDatabaseAdapter.class, ChunkdedHistoryDbAdapter.class);
	}

	private static Map<FeatureKey<?>, Object> extractFeatures(final HistoryEntry entry) {
		final Map<FeatureKey<?>, Object> tfIdfs = Maps.newHashMap();

		final FPStore fpStore = FPStore.singleton();
		final Set<Set<AtomicFeature<?>>> frequetPatterns = fpStore.getFrequetPatterns();

		final AtomicFeaturesManager atomicFeaturesManager = AtomicFeaturesManager.singleton();
		final List<AtomicFeature<?>> features = atomicFeaturesManager.getFeatures(entry);
		final HashSet<AtomicFeature<?>> featureSet = Sets.newHashSet(features);
		final List<Set<AtomicFeature<?>>> compoundFeatures = Lists.newLinkedList();
		final Multiset<Set<AtomicFeature<?>>> tfs = HashMultiset.create();

		for (final Set<AtomicFeature<?>> fp : frequetPatterns) {
			final boolean containsFp = featureSet.containsAll(fp);
			if (containsFp) {
				compoundFeatures.add(fp);
				tfs.add(fp);
			}
		}

		double maxCount = 0;
		final Set<Entry<Set<AtomicFeature<?>>>> entrySet = tfs.entrySet();
		for (final Entry<Set<AtomicFeature<?>>> counts : entrySet) {
			final int count = counts.getCount();
			if (count > maxCount) {
				maxCount = count;
			}
		}

		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		for (final Set<AtomicFeature<?>> feature : compoundFeatures) {
			final double idf = tfIdfStore.getInvertedDocumentFrequency(feature);
			final int count = tfs.count(feature);
			final double tf = count / maxCount;
			final double tfIdf = tf * idf;
			final TfIdfKey tfIdfKey = TfIdfKey.getKey(feature);
			tfIdfs.put(tfIdfKey, tfIdf);
		}

		return Collections.unmodifiableMap(tfIdfs);
	}
	
	private static void writeToDb(final String filename, final String testName, Map<Query, List<Prediction>> predictions) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		String connStr = "jdbc:sqlite:" + filename;

		// Making Connection
		Connection connection = null;
		try{
			connection = DriverManager.getConnection(connStr);
			connection.setAutoCommit(false);
			
			PreparedStatement statement = connection.prepareStatement(INSERT_TEST_SQL);
			statement.setString(1, testName);
			statement.execute();
			
			final int testId = getId(connection);
			
			final Set<Map.Entry<Query, List<Prediction>>> entrySet = predictions.entrySet();
			for (Map.Entry<Query, List<Prediction>> entry : entrySet) {
				final Query key = entry.getKey();
				final String query = key.getQuery();
				final String actualResponse = key.getActualResponse();
				
				statement = connection.prepareStatement(INSERT_QUERY_SQL);
				statement.setInt(1, testId);
				statement.setString(2, query);
				statement.setString(3, actualResponse);
				statement.execute();
				
				final int queryId = getId(connection);
				final List<Prediction> value = entry.getValue();
				for (Prediction prediction : value) {
					final int charGiven = prediction.getCharGiven();
					final String str = prediction.getPrediction();
					statement = connection.prepareStatement(INSERT_PREDICTION_SQL);
					statement.setInt(1, queryId);
					statement.setInt(2, charGiven);
					statement.setString(3, str);
					statement.execute();
				}
			}
			
			connection.commit();
		} catch(SQLException e){
			e.printStackTrace();
		} finally {
			try {
				if(connection != null)
					connection.close();
			} catch(SQLException e){
				e.printStackTrace();
			}
		}
	}

	private static int getId(Connection connection) throws SQLException {
		final Statement stmt = connection.createStatement();
		final ResultSet rs = stmt.executeQuery(ID_SQL);
		rs.next();
		final int id = rs.getInt(1);
		return id;
	}

	public static void main(String[] args) throws IOException {
		initialize();
		
		final ChunkdedHistoryDbAdapter dbAdapter = (ChunkdedHistoryDbAdapter) HistoryDatabaseAdapter.singleton();
		final CachingHistoryDbAdapter underlyingDbAdapter = dbAdapter.getUnderlying();
		final FPStore fpStore = FPStore.singleton();
		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		final TfIdfFeatureStore tfIdfFeatureStore = TfIdfFeatureStore.singleton();
		final PredictionEngine predictionEngine = PredictionEngine.singleton();
		
		final FileWriter writer = new FileWriter("crossValdataion.out");
		
		for(int round = 1; round <= 10; round++) {
			if(round != 1) {
				dbAdapter.refresh();
				fpStore.refresh();
				tfIdfStore.refresh();
				tfIdfFeatureStore.refresh();
			}
			
			System.out.println("Validating (" + round + "/" + N_FOLD + ")");
//			writer.write("*******************************************************\n");
//			writer.write("* Validation Round " + round + "\n");
//			writer.write("*******************************************************\n\n");
			final Map<Query, List<Prediction>> map = new HashMap<Query, List<Prediction>>();

			final SortedSet<Integer> testSetIds = dbAdapter.getTestSetIds();
			for (Integer id : testSetIds) {
				final NextHistoryInfo nextHistoryInfo = underlyingDbAdapter.nextHistoryEntry(id);
				final HistoryEntry nextHistoryEntry = nextHistoryInfo == null ? null : (nextHistoryInfo.interval < MAX_DEAD_AIR_INTERVAL ? nextHistoryInfo.history : null);
				if(nextHistoryEntry != null) {
					final HistoryEntry history = underlyingDbAdapter.getHistoryById(id);
					final String message = history.getMessage();
					final String target = nextHistoryEntry.getMessage();
					
//					writer.write("Message: " + message + "\n");
//					writer.write("Target : " + target + "\n");
					Query query = new Query(message, target);					
					
					final String sender = history.getSender();
					final String nextSender = nextHistoryEntry.getSender();
					final MessageType type = sender.equals(nextSender) ? MessageType.SUPPLEMENT : MessageType.REPLY;
					
					final Map<FeatureKey<?>, Object> features = extractFeatures(history);
					
					final StringBuilder incomplete = new StringBuilder();
					final String[] splites = target.split("");
					for (String character : splites) {
						incomplete.append(character);
						final String incompleteStr = incomplete.toString();
						final List<String> predictions = predictionEngine.getPredictions(features, incompleteStr, type);
						if(predictions.isEmpty()) {
							break;
						}
						final List<Prediction> pList = new ArrayList<Prediction>();
//						writer.write("\nPredictions on incomplete message \"" + incompleteStr + "\":\n");
						for (String pred : predictions) {
//							writer.write("\t" + pred + "\n");
							Prediction input = new Prediction(pred.length(), pred);
							pList.add(input);							
						}
						map.put(query, pList);
					}
					

//					writer.write("\n--------------------------------------------------\n\n");
				}
			}
			
			writeToDb("db.sqlite", "Validation" + round, map);
			dbAdapter.nextChunk();
		}

//		writer.close();
	}
}
