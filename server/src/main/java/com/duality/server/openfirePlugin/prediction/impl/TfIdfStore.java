package com.duality.server.openfirePlugin.prediction.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

public class TfIdfStore {
	private final Multiset<List<String>> docFreq;
	private final Map<Long, Multiset<List<String>>> termFreq;
	private final Multiset<List<String>> ngrams;

	public TfIdfStore(final List<HistoryEntry> histories) {
		docFreq = HashMultiset.create();
		termFreq = Maps.newHashMap();
		ngrams = HashMultiset.create();
		init(histories);
	}

	public void init(final List<HistoryEntry> histories) {
		for (final HistoryEntry entry : histories) {
			add(entry);
		}
	}

	public void add(final HistoryEntry entry) {
		final String message = entry.getMessage();
		final String[] tokens = message.split(" ");

		final List<String> tokenList = Arrays.asList(tokens);
		final Iterable<List<String>> unigram = createNgramIterable(tokenList, 1);
		final Iterable<List<String>> bigram = createNgramIterable(tokenList, 2);
		final Iterable<List<String>> trigram = createNgramIterable(tokenList, 3);
		final Iterable<List<String>> ngram = Iterables.concat(unigram, bigram, trigram);

		final ImmutableMultiset<List<String>> tf = ImmutableMultiset.copyOf(ngram);

		final long id = entry.getId();
		termFreq.put(id, tf);

		final Set<List<String>> tokenGroupSet = tf.elementSet();
		docFreq.addAll(tokenGroupSet);

		final ImmutableSet<Entry<List<String>>> tokenGroupCounts = tf.entrySet();
		for (final Entry<List<String>> tokenGroupEntry : tokenGroupCounts) {
			final List<String> tokenGroup = tokenGroupEntry.getElement();
			final int count = tokenGroupEntry.getCount();
			ngrams.add(tokenGroup, count);
		}
	}

	private Iterable<List<String>> createNgramIterable(final List<String> tokenList, final int n) {
		final Set<String> nullCollection = Collections.<String> singleton(null);
		final Iterable<String> startEndFilled = Iterables.concat(nullCollection, tokenList, nullCollection);
		return Iterables.partition(startEndFilled, n);
	}

	public int getDocumentFrequency(List<String> phrase) {
		final int count = docFreq.count(phrase);
		return count;
	}
	
	public Multiset<List<String>> getTermFrequencies(final long id) {
		final Multiset<List<String>> termFrequencies = termFreq.get(id);
		return Multisets.unmodifiableMultiset(termFrequencies);
	}
	
	public int getTermFrequency(final long id, final List<String> phrase) {
		final Multiset<List<String>> termFrequencies = getTermFrequencies(id);
		final int count = termFrequencies.count(phrase);
		return count;
	}
}
