package com.duality.server.openfirePlugin.prediction.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.FeatureKey;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.key.TfIdfKey;
import com.duality.server.openfirePlugin.prediction.impl.key.VectorSpaceFeatureKey;
import com.duality.server.openfirePlugin.prediction.impl.store.NgramStore;
import com.duality.server.openfirePlugin.prediction.impl.store.TfIdfStore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Sets;

public class TfIdfNgramPredictionEngine extends PredictionEngine {
	private static final int MAX_PREDICTION_NUM = 10;

	public TfIdfNgramPredictionEngine() {
	}

	@Override
	public List<String> getPredictions(final Map<FeatureKey<?>, Object> context, final String incompletedMessage) {
		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> histories = historyDb.getAllHistory();
		final MinMaxPriorityQueue<MessageCloseness> queue = MinMaxPriorityQueue.maximumSize(MAX_PREDICTION_NUM).create();
		for (final HistoryEntry history : histories) {
			final Map<FeatureKey<?>, Object> features = extractFeatures(history);
			final long id = history.getId();
			final double closeness = dotProduct(context, features);
			final MessageCloseness messageCloseness = new MessageCloseness(id, closeness);
			queue.add(messageCloseness);
		}

		final List<String> predictions = Lists.newArrayListWithCapacity(queue.size());

		while (!queue.isEmpty()) {
			final MessageCloseness closeness = queue.poll();
			final long id = closeness.getId();
			// TODO: Change it to get next message when the method is available
			final HistoryEntry nextHistory = historyDb.getHistoryById(id);
			if (nextHistory != null) {
				final String message = nextHistory.getMessage();
				if (incompletedMessage == null || message.startsWith(incompletedMessage)) {
					predictions.add(message);
				}
			}
		}

		if (predictions.isEmpty()) {
			// TODO: use n-gram here
			// final String[] split = incompletedMessage.split(" ");
		}

		return predictions;
	}

	@Override
	public void addHistoryData(final HistoryEntry history) {
	}

	private Map<FeatureKey<?>, Object> extractFeatures(final HistoryEntry entry) {
		final Map<FeatureKey<?>, Object> features = Maps.newHashMap();
		final long id = entry.getId();

		final NgramStore ngramStore = NgramStore.singleton();
		final Set<List<String>> ngrams = ngramStore.getAllNgrams();

		final String[] tokens = MessageUtils.extractTokens(entry);
		final Set<List<String>> ngramSet = Sets.newHashSet();

		for (int start = 0; start < tokens.length; start++) {
			final int maxLength = tokens.length - start;
			for (int length = 1; length <= maxLength; length++) {
				final List<String> ngram = Lists.newArrayListWithCapacity(length);

				final int end = start + length;
				for (int i = start; i < end; i++) {
					ngram.add(tokens[i]);
				}

				if (ngrams.contains(ngram)) {
					ngramSet.add(ngram);
				}
			}
		}

		final TfIdfStore tfIdfStore = TfIdfStore.singleton();
		for (final List<String> ngram : ngramSet) {
			final double idf = tfIdfStore.getInvertedDocumentFrequency(ngram);
			final double tf = tfIdfStore.getTermFrequency(id, ngram);
			final double tfIdf = tf * idf;
			final TfIdfKey tfIdfKey = new TfIdfKey(ngram);
			features.put(tfIdfKey, tfIdf);
		}

		// FIXME: Think a way to assign weighting on TF-IDF
		// final String sender = entry.getSender();
		// features.put(UserKey.SENDER, sender);
		//
		// final String receiver = entry.getReceiver();
		// features.put(UserKey.RECEIVER, receiver);
		//
		// final double senderLatitude = entry.getSenderlatitude();
		// final double senderLongtitude = entry.getSenderLongtitude();
		// features.put(ClientContextKey.SENDER_LOCATION, new Location(senderLatitude, senderLongtitude));
		//
		// final double receiverLatitude = entry.getReceiverlatitude();
		// final double receiverLongtitude = entry.getReceiverLongtitude();
		// features.put(ClientContextKey.RECEIVER_LOCATION, new Location(receiverLatitude, receiverLongtitude));
		//
		// final Date time = entry.getTime();
		// features.put(ClientContextKey.TIME, time);

		return features;
	}

	private double cosine(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features) {
		final Set<FeatureKey<?>> contextKeySet = context.keySet();
		final Set<FeatureKey<?>> featuresKeySet = features.keySet();

		final Set<FeatureKey<?>> intersaction = Sets.newHashSet(featuresKeySet);
		intersaction.retainAll(contextKeySet);

		final double dotProduct = dotProduct(context, features, intersaction);

		final Set<Entry<FeatureKey<?>, Object>> entrySet = features.entrySet();
		double sumOfProducts = 0.0;
		for (final Entry<FeatureKey<?>, Object> entry : entrySet) {
			final FeatureKey<?> key = entry.getKey();

			if (key instanceof VectorSpaceFeatureKey) {
				@SuppressWarnings("rawtypes")
				final VectorSpaceFeatureKey vectorSpaceFeatureKey = (VectorSpaceFeatureKey) key;
				final Object value = entry.getValue();
				@SuppressWarnings("unchecked")
				final double product = vectorSpaceFeatureKey.multiply(value, value);
				sumOfProducts += product;
			}
		}

		return dotProduct / sumOfProducts;
	}

	private double dotProduct(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features) {
		final Set<FeatureKey<?>> contextKeySet = context.keySet();
		final Set<FeatureKey<?>> featuresKeySet = features.keySet();

		final Set<FeatureKey<?>> intersaction = Sets.newHashSet(featuresKeySet);
		intersaction.retainAll(contextKeySet);

		final double dotProduct = dotProduct(context, features, intersaction);
		return dotProduct;
	}

	private double dotProduct(final Map<FeatureKey<?>, Object> context, final Map<FeatureKey<?>, Object> features, final Set<FeatureKey<?>> intersaction) {
		double dotProduct = 0.0;
		for (final FeatureKey<?> key : intersaction) {
			if (key instanceof VectorSpaceFeatureKey) {
				@SuppressWarnings("rawtypes")
				final VectorSpaceFeatureKey vectorSpaceFeatureKey = (VectorSpaceFeatureKey) key;
				@SuppressWarnings("unchecked")
				final double product = vectorSpaceFeatureKey.multiply(context, features);
				dotProduct += product;
			}
		}

		if (dotProduct == 0.0) {
			return 0.0;
		}
		return dotProduct;
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
			if (this.closeness > that.closeness) {
				return -1;
			} else if (this.closeness == that.closeness) {
				return 0;
			} else {
				return 1;
			}
		}

		@Override
		public String toString() {
			return "{id: " + id + ", closeness: " + closeness + "}";
		}
	}
}
