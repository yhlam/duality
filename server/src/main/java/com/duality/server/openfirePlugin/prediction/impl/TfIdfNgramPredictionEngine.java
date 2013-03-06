package com.duality.server.openfirePlugin.prediction.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.ClientContextKey.Location;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class TfIdfNgramPredictionEngine extends PredictionEngine {
	private static final int MAX_PREDICTION_NUM = 10;

	private final TfIdfStore tfIdfStore;

	public TfIdfNgramPredictionEngine() {
		final HistoryDatabaseAdapter database = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> histories = database.getAllHistory();

		tfIdfStore = new TfIdfStore(histories);
	}

	@Override
	public List<String> getPredictions(final Map<FeatureKey<?>, Object> context, final String incompletedMessage) {
		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> histories = historyDb.getAllHistory();
		final MinMaxPriorityQueue<MessageCloseness> queue = MinMaxPriorityQueue.maximumSize(MAX_PREDICTION_NUM).create();
		for (final HistoryEntry history : histories) {
			final Map<FeatureKey<?>, Object> features = extractFeatures(history);
			final long id = history.getId();
			final double cosine = cosine(context, features);
			final MessageCloseness messageCloseness = new MessageCloseness(id, cosine);
			queue.add(messageCloseness);
		}

		final List<String> predictions = Lists.newArrayListWithCapacity(queue.size());
		for (final MessageCloseness closeness : queue) {
			final long id = closeness.getId();
			// TODO: Change it to get next message when the method is available
			final HistoryEntry nextHistory = historyDb.getHistoryById(id);
			final String message = nextHistory.getMessage();
			predictions.add(message);
		}

		return predictions;
	}

	@Override
	public void addHistoryData(final HistoryEntry history) {
		tfIdfStore.add(history);
	}

	private Map<FeatureKey<?>, Object> extractFeatures(final HistoryEntry entry) {
		final Map<FeatureKey<?>, Object> features = Maps.newHashMap();
		final long id = entry.getId();
		final Multiset<List<String>> termFrequencies = tfIdfStore.getTermFrequencies(id);

		final String message = entry.getMessage();
		final String[] tokens = message.split(" ");
		for (final String token : tokens) {
			final List<String> phrase = Collections.singletonList(token);
			final int df = tfIdfStore.getDocumentFrequency(phrase);
			final int tf = termFrequencies.count(df);
			final double tfIdf = tf / (double) df;
			final TfIdfKey tfIdfKey = new TfIdfKey(phrase);
			features.put(tfIdfKey, tfIdf);
		}

		final String sender = entry.getSender();
		features.put(ClientContextKey.SENDER, sender);

		final String receiver = entry.getReceiver();
		features.put(ClientContextKey.RECEIVER, receiver);

		final double senderLatitude = entry.getSenderlatitude();
		final double senderLongtitude = entry.getSenderLongtitude();
		features.put(ClientContextKey.SENDER_LOCATION, new Location(senderLatitude, senderLongtitude));

		final double receiverLatitude = entry.getReceiverlatitude();
		final double receiverLongtitude = entry.getReceiverLongtitude();
		features.put(ClientContextKey.RECEIVER_LOCATION, new Location(receiverLatitude, receiverLongtitude));

		final Date time = entry.getTime();
		features.put(ClientContextKey.TIME, time);

		return features;
	}

	private double cosine(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features) {
		final Set<FeatureKey<?>> contextKeySet = context.keySet();
		final Set<FeatureKey<?>> featuresKeySet = features.keySet();

		final Set<FeatureKey<?>> intersaction = Sets.newHashSet(featuresKeySet);
		intersaction.retainAll(contextKeySet);

		double dotProduct = 0.0;
		for (final FeatureKey<?> key : intersaction) {
			final Object dataValue = features.get(key);
			final Object contextValue = context.get(key);
			final double product = multiply(key, contextValue, dataValue);
			dotProduct += product;
		}

		if (dotProduct == 0.0) {
			return 0.0;
		}

		final Set<Entry<FeatureKey<?>, Object>> entrySet = features.entrySet();
		double sumOfProducts = 0.0;
		for (final Entry<FeatureKey<?>, Object> entry : entrySet) {
			final FeatureKey<?> key = entry.getKey();
			final Object value = entry.getValue();
			final double product = multiply(key, value, value);
			sumOfProducts += product;
		}

		return dotProduct / sumOfProducts;
	}

	private double multiply(final FeatureKey<?> key, final Object value1, final Object value2) {
		final Class<?> type = key.getType();
		if (type.isAssignableFrom(Number.class)) {
			final double doubleValue1 = ((Number) value1).doubleValue();
			final double doubleValue2 = ((Number) value2).doubleValue();
			return doubleValue1 * doubleValue2;
		} else if (type.isAssignableFrom(String.class)) {
			return value1.equals(value2) ? 1 : 0;
		} else if (type.isAssignableFrom(Date.class)) {
			final long time1 = ((Date) value1).getTime();
			final long time2 = ((Date) value2).getTime();
			return time1 * time2;
		} else if (type.isAssignableFrom(Location.class)) {
			final Location loc1 = (Location) value1;
			final Location loc2 = (Location) value2;
			final double latitude1 = loc1.getLatitude();
			final double latitude2 = loc2.getLatitude();
			final double longtitude1 = loc1.getLongtitude();
			final double longtitude2 = loc2.getLongtitude();

			return latitude1 * latitude2 + longtitude1 * longtitude2;
		}

		throw new RuntimeException("Unsupported feature type: " + type + " in " + key);
	}

	private static class MessageCloseness implements Comparable<MessageCloseness> {
		private final long id;
		private final double closeness;

		public MessageCloseness(final long id, final double closeness) {
			this.id = id;
			this.closeness = closeness;
		}

		public long getId() {
			return id;
		}

		@Override
		public int compareTo(final MessageCloseness that) {
			if (this.closeness < that.closeness) {
				return -1;
			} else if (this.closeness == that.closeness) {
				return 0;
			} else {
				return 1;
			}
		}
	}
}
