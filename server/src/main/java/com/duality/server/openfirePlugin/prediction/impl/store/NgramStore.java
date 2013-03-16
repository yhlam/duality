package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.MessageUtils;
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

	public Set<List<String>> getAllNgrams() {
		final Set<List<String>> ngrams = Sets.newHashSet();
		final Deque<String> prefix = Lists.newLinkedList();
		constructNgramSet(root, prefix, ngrams);

		return ngrams;
	}

	private void constructNgramSet(final NgramNode node, final Deque<String> prefix, final Set<List<String>> ngrams) {
		final List<NgramNode> children = node.getChildren();
		for (final NgramNode child : children) {
			final String token = child.getToken();
			prefix.add(token);
			final List<String> ngram = Lists.newArrayList(prefix);
			ngrams.add(ngram);

			constructNgramSet(child, prefix, ngrams);

			prefix.removeLast();
		}

	}

	public void refresh() {
		final NgramNode tree = new NgramNode(null);

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> allHistory = historyDb.getAllHistory();
		for (final HistoryEntry history : allHistory) {
			final String[] tokens = MessageUtils.extractTokens(history);
			addNgrams(tree, tokens);
		}

		final int count = tree.getCount();
		final int minSupport = (int) (count * MIN_SUPPORT);

		final List<NgramNode> children = tree.getChildren();
		for (final NgramNode child : children) {
			pruneTree(child, minSupport);
		}

		this.root = tree;
	}

	private void addNgrams(final NgramNode tree, final String[] tokens) {
		for (int start = 0; start < tokens.length; start++) {
			NgramNode parent = tree;
			for (int i = start; i < tokens.length; i++) {
				final String token = tokens[i];
				parent = parent.addChild(token);
			}
		}

		if (tokens.length > 0) {
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
		private final String token;
		private final List<NgramNode> children;
		private int count;

		public NgramNode(final String token) {
			this.token = token;
			this.children = Lists.newLinkedList();
			this.count = 1;
		}

		public String getToken() {
			return token;
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

		public NgramNode addChild(final String token) {
			NgramNode node = null;
			for (final NgramNode child : children) {
				final String childToken = child.getToken();
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
			return "{token: " + token + ", count: " + count + ", children: " + children + "}";
		}
	}
}
