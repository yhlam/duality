package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.dataTier.NewHistoryHandler;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

public class TfIdfStore implements NewHistoryHandler {
	private static final AtomicFeaturesManager ATOMIC_FEATURES_MANAGER = AtomicFeaturesManager.singleton();
	private static final FPStore FP_STORE = FPStore.singleton();

	private static final TfIdfStore INSTANCE = new TfIdfStore();

	public static TfIdfStore singleton() {
		return INSTANCE;
	}

	private Map<Set<AtomicFeature<?>>, List<TermFrequency>> tfDf;
	private int totalCount;

	private TfIdfStore() {
		refresh();
		final HistoryDatabaseAdapter historyDbAdapter = HistoryDatabaseAdapter.singleton();
		historyDbAdapter.register(this);
	}

	public void refresh() {
		final Map<Set<AtomicFeature<?>>, List<TermFrequency>> tfDf = Maps.newHashMap();

		final Set<Set<AtomicFeature<?>>> frequetPatterns = FP_STORE.getFrequetPatterns();

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> allHistory = historyDb.getAllHistory();
		for (final HistoryEntry history : allHistory) {
			processHistory(tfDf, frequetPatterns, history);
		}

		this.tfDf = tfDf;
		this.totalCount = allHistory.size();
	}

	private void processHistory(final Map<Set<AtomicFeature<?>>, List<TermFrequency>> tfDf, final Set<Set<AtomicFeature<?>>> frequetPatterns, final HistoryEntry history) {
		final int id = history.getId();

		final Multiset<Set<AtomicFeature<?>>> tf = HashMultiset.create();
		final List<AtomicFeature<?>> features = ATOMIC_FEATURES_MANAGER.constructFeatures(history);
		final HashSet<AtomicFeature<?>> featureSet = Sets.newHashSet(features);
		for (Set<AtomicFeature<?>> fp: frequetPatterns) {
			final boolean hasFp = featureSet.containsAll(fp);
			if (hasFp) {
				tf.add(Sets.newCopyOnWriteArraySet(fp));
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

	public double getInvertedDocumentFrequency(final Set<AtomicFeature<?>> compoundFeature) {
		final List<TermFrequency> tfList = tfDf.get(compoundFeature);
		final double df = tfList == null ? 1 : tfList.size();
		final double idf = Math.log(totalCount / df);
		return idf;
	}

	public double getTermFrequency(final int id, final Set<AtomicFeature<?>> compoundFeature) {
		final List<TermFrequency> tfList = tfDf.get(compoundFeature);
		if (tfList == null) {
			return 0;
		}

		for (final TermFrequency termFrequency : tfList) {
			final int documentId = termFrequency.getDocumentId();
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
		private final int id;
		private final double tf;

		public TermFrequency(final int id, final double tf) {
			this.id = id;
			this.tf = tf;
		}

		public int getDocumentId() {
			return id;
		}

		public double getFrequency() {
			return tf;
		}
	}

	@Override
	public void onNewHistory(HistoryEntry newHistory) {
		final Set<Set<AtomicFeature<?>>> frequetPatterns = FP_STORE.getFrequetPatterns();
		processHistory(tfDf, frequetPatterns, newHistory);
	}
}
