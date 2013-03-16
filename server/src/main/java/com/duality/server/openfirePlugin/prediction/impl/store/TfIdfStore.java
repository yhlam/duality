package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.MessageUtils;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class TfIdfStore {
	private static final TfIdfStore INSTANCE = new TfIdfStore();

	public static TfIdfStore singleton() {
		return INSTANCE;
	}

	private Map<List<String>, List<TermFrequency>> tfDf;
	private int totalCount;
	
	private TfIdfStore() {
		refresh();
	}
	
	public void refresh() {
		final Map<List<String>, List<TermFrequency>> tfDf = Maps.newHashMap();
		
		final NgramStore ngramStore = NgramStore.singleton();
		final Set<List<String>> ngrams = ngramStore.getAllNgrams();

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> allHistory = historyDb.getAllHistory();
		for (final HistoryEntry history : allHistory) {
			final long id = history.getId();
			
			final String[] tokens = MessageUtils.extractTokens(history);
			final Multiset<List<String>> tf = HashMultiset.create();
			
			for(int start=0; start<tokens.length; start++) {
				final int maxLength = tokens.length - start;
				for(int length=1; length<=maxLength; length++) {
					final List<String> ngram = Lists.newArrayListWithCapacity(length);
					
					final int end = start + length;
					for(int i=start; i<end; i++) {
						ngram.add(tokens[i]);
					}
					
					if(ngrams.contains(ngram)) {
						tf.add(ngram);
					}
				}
			}
			
			if(!tf.isEmpty()) {
				double maxCount = 0;
				final Set<Entry<List<String>>> entrySet = tf.entrySet();
				for (Entry<List<String>> entry : entrySet) {
					final int count = entry.getCount();
					if(count > maxCount) {
						maxCount = count;
					}
				}

				for (Entry<List<String>> entry : entrySet) {
					final List<String> ngram = entry.getElement();
					List<TermFrequency> tfList = tfDf.get(ngram);
					if(tfList == null) {
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

	public double getInvertedDocumentFrequency(List<String> phrase) {
		final List<TermFrequency> tfList = tfDf.get(phrase);
		final double df = tfList.size();
		final double idf = Math.log(totalCount / df);
		return idf;
	}
	
	public double getTermFrequency(final long id, final List<String> phrase) {
		final List<TermFrequency> tfList = tfDf.get(phrase);
		if(tfList == null) {
			return 0;
		}
		
		for (TermFrequency termFrequency : tfList) {
			final long documentId = termFrequency.getDocumentId();
			if(documentId == id) {
				final double frequency = termFrequency.getFrequency();
				return frequency;
			}
		}

		return 0;
	}

	public List<TermFrequency> getRelevantTermFrequencies(final List<String> phrase) {
		final List<TermFrequency> tfs = tfDf.get(phrase);
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
