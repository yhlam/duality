package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NgramStore {
	private static double MIN_SUPPORT = 0.1;
	private static double MIN_CONF = 0.2;

	private static final NgramStore INSTANCE = new NgramStore();

	public static NgramStore singleton() {
		return INSTANCE;
	}

	private volatile NgramNode root;

	private NgramStore() {
		refresh();
	}

	public Set<List<AtomicFeature<?>>> getAllNgrams() {
		final Set<List<AtomicFeature<?>>> ngrams = Sets.newHashSet();
		final Deque<AtomicFeature<?>> prefix = Lists.newLinkedList();
		constructNgramSet(root, prefix, ngrams);

		return ngrams;
	}

	private void constructNgramSet(final NgramNode node, final Deque<AtomicFeature<?>> prefix, final Set<List<AtomicFeature<?>>> ngrams) {
		final List<NgramNode> children = node.getChildren();
		for (final NgramNode child : children) {
			final AtomicFeature<?> token = child.getFeature();
			prefix.add(token);
			final List<AtomicFeature<?>> ngram = Lists.newArrayList(prefix);
			ngrams.add(ngram);

			constructNgramSet(child, prefix, ngrams);

			prefix.removeLast();
		}

	}

	public void refresh() {
		final NgramNode tree = new NgramNode(null);

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> allHistory = historyDb.getAllHistory();
		final AtomicFeaturesManager atomicFeaturesManager = AtomicFeaturesManager.singleton();
		for (final HistoryEntry history : allHistory) {
			final List<AtomicFeature<?>> features = atomicFeaturesManager.getFeatures(history);
			final List<AtomicFeature<?>> tokenFeatures = FluentIterable
					.from(features)
					.filter(new Predicate<AtomicFeature<?>>() {
						public boolean apply(AtomicFeature<?> feature) {
							final FeatureType type = feature.getType();
							return type == FeatureType.TOKEN;
						}
					})
					.toList();
			addNgrams(tree, tokenFeatures);
		}

		final int count = tree.getCount();
		final int minSupport = (int) (count * MIN_SUPPORT);

		final List<NgramNode> children = tree.getChildren();
		for (final NgramNode child : children) {
			pruneTree(child, minSupport);
		}

		this.root = tree;
	}

	private void addNgrams(final NgramNode tree, final List<AtomicFeature<?>> features) {
		final int length = features.size();
		for (int start = 0; start < length; start++) {
			NgramNode parent = tree;
			final List<AtomicFeature<?>> subList = features.subList(start, length);
			for (AtomicFeature<?> atomicFeature : subList) {
				parent = parent.addChild(atomicFeature);
			}
		}

		if (length > 0) {
			tree.incrementCount();
		}
	}

	private void pruneTree(final NgramNode tree, final int minSupportCount) {
		final List<NgramNode> children = tree.getChildren();
		final int parentCount = tree.getCount();
		final int minConf = (int) (parentCount * MIN_CONF);

		final ListIterator<NgramNode> it = children.listIterator();
		while (it.hasNext()) {
			final NgramNode child = it.next();
			final int count = child.getCount();
			if (count <= minSupportCount || count <= minConf) {
				it.remove();
			} else {
				pruneTree(child, minSupportCount);
			}
		}
	}

	private static class NgramNode {
		private final AtomicFeature<?> feature;
		private final List<NgramNode> children;
		private int count;

		public NgramNode(final AtomicFeature<?> feature) {
			this.feature = feature;
			this.children = Lists.newLinkedList();
			this.count = 1;
		}

		public AtomicFeature<?> getFeature() {
			return feature;
		}

		public List<NgramNode> getChildren() {
			return children;
		}

		public int getCount() {
			return count;
		}

		public void incrementCount() {
			incrementCount(1);
		}

		public void incrementCount(final int count) {
			this.count += count;
		}

		public NgramNode addChild(final AtomicFeature<?> token) {
			NgramNode node = null;
			for (final NgramNode child : children) {
				final AtomicFeature<?> childToken = child.getFeature();
				if (childToken.equals(token)) {
					node = child;
					break;
				}
			}

			if (node == null) {
				node = new NgramNode(token);
				children.add(node);
			} else {
				node.incrementCount();
			}

			return node;
		}

		@Override
		public String toString() {
			return "{feature: " + feature + ", count: " + count + ", children: " + children + "}";
		}
	}
}
