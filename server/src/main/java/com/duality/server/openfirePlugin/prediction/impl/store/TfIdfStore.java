package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

public class TfIdfStore {
	private static final TfIdfStore INSTANCE = new TfIdfStore();

	public static TfIdfStore singleton() {
		return INSTANCE;
	}

	private Map<Set<AtomicFeature<?>>, List<TermFrequency>> tfDf;
	private int totalCount;

	private TfIdfStore() {
		refresh();
	}

	public void refresh() {
		final Map<Set<AtomicFeature<?>>, List<TermFrequency>> tfDf = Maps.newHashMap();

		final FPStore fsStore = FPStore.singleton();
		final Set<Set<AtomicFeature<?>>> frequetPatterns = fsStore.getFrequetPatterns();
		final AtomicFeaturesManager atomicFeaturesManager = AtomicFeaturesManager.singleton();

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> allHistory = historyDb.getAllHistory();
		for (final HistoryEntry history : allHistory) {
			final long id = history.getId();

			final Multiset<Set<AtomicFeature<?>>> tf = HashMultiset.create();
			final List<AtomicFeature<?>> features = atomicFeaturesManager.constructFeatures(history);
			final Set<Set<AtomicFeature<?>>> combinations = TfIdfUtils.combinations(features);
			for (final Set<AtomicFeature<?>> group : combinations) {
				if (frequetPatterns.contains(group)) {
					tf.add(group);
				}
			}

			if (!tf.isEmpty()) {
				double maxCount = 0;
				final Set<Entry<Set<AtomicFeature<?>>>> entrySet = tf.entrySet();
				for (final Entry<Set<AtomicFeature<?>>> entry : entrySet) {
					final int count = entry.getCount();
					if (count > maxCount) {
						maxCount = count;
					}
				}

				for (final Entry<Set<AtomicFeature<?>>> entry : entrySet) {
					final Set<AtomicFeature<?>> ngram = entry.getElement();
					List<TermFrequency> tfList = tfDf.get(ngram);
					if (tfList == null) {
						tfList = Lists.newLinkedList();
						tfDf.put(ngram, tfList);
					}

					final int count = entry.getCount();
					final double normalizedTf = count / maxCount;
					final TermFrequency termFrequency = new TermFrequency(id, normalizedTf);
					tfList.add(termFrequency);
				}
			}
		}

		this.tfDf = tfDf;
		this.totalCount = allHistory.size();
	}

	public double getInvertedDocumentFrequency(final Set<AtomicFeature<?>> compoundFeature) {
		final List<TermFrequency> tfList = tfDf.get(compoundFeature);
		final double df = tfList.size();
		final double idf = Math.log(totalCount / df);
		return idf;
	}

	public double getTermFrequency(final long id, final Set<AtomicFeature<?>> compoundFeature) {
		final List<TermFrequency> tfList = tfDf.get(compoundFeature);
		if (tfList == null) {
			return 0;
		}

		for (final TermFrequency termFrequency : tfList) {
			final long documentId = termFrequency.getDocumentId();
			if (documentId == id) {
				final double frequency = termFrequency.getFrequency();
				return frequency;
			}
		}

		return 0;
	}

	public List<TermFrequency> getRelevantTermFrequencies(final Set<AtomicFeature<?>> compoundFeature) {
		final List<TermFrequency> tfs = tfDf.get(compoundFeature);
		return tfs;
	}

	public static class TermFrequency {
		private final long id;
		private final double tf;

		public TermFrequency(final long id, final double tf) {
			this.id = id;
			this.tf = tf;
		}

		public long getDocumentId() {
			return id;
		}

		public double getFrequency() {
			return tf;
		}
	}
}
