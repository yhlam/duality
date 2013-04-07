package com.duality.server.openfirePlugin.prediction.impl.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeaturesManager;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

public class FPStore {
	private static double MIN_SUPPORT = 0.2;

	private static final FPStore INSTANCE = new FPStore();

	public static FPStore singleton() {
		return INSTANCE;
	}

	private volatile Set<Set<AtomicFeature<?>>> fp;

	private FPStore() {
		refresh();
	}

	public void refresh() {
		final FPNode tree = new FPNode(null, null, 0);

		final HistoryDatabaseAdapter historyDb = HistoryDatabaseAdapter.singleton();
		final List<HistoryEntry> allHistory = historyDb.getAllHistory();
		final AtomicFeaturesManager atomicFeaturesManager = AtomicFeaturesManager.singleton();

		final int totalSize = allHistory.size();
		final List<List<AtomicFeature<?>>> featureList = Lists.newArrayListWithCapacity(totalSize);
		final Multiset<AtomicFeature<?>> featureCount = HashMultiset.create();

		// Count the frequency of features
		for (final HistoryEntry history : allHistory) {
			final List<AtomicFeature<?>> features = atomicFeaturesManager.getFeatures(history);
			featureList.add(features);
			featureCount.addAll(features);
		}

		// Remove the non-frequent features
		final int minSupport = (int) (totalSize * MIN_SUPPORT);
		final Set<Entry<AtomicFeature<?>>> counts = featureCount.entrySet();
		final Set<AtomicFeature<?>> frequentNode = FluentIterable.from(counts).transform(new Function<Entry<AtomicFeature<?>>, AtomicFeature<?>>() {
			@Override
			public AtomicFeature<?> apply(final Entry<AtomicFeature<?>> input) {
				return input.getElement();
			}
		}).toSet();

		// Build the FP-Tree
		final Multimap<AtomicFeature<?>, FPNode> nodeTable = LinkedListMultimap.create();
		for (final List<AtomicFeature<?>> features : featureList) {
			final ImmutableList<AtomicFeature<?>> nodeFeatures = FluentIterable.from(features).filter(new Predicate<AtomicFeature<?>>() {
				@Override
				public boolean apply(final AtomicFeature<?> input) {
					return frequentNode.contains(input);
				}
			}).toSortedList(new Comparator<AtomicFeature<?>>() {

				@Override
				public int compare(final AtomicFeature<?> o1, final AtomicFeature<?> o2) {
					final int count1 = featureCount.count(o1);
					final int count2 = featureCount.count(o2);
					return count2 - count1;
				}
			});

			if(!nodeFeatures.isEmpty()) {
				tree.incrementCount();
			}

			FPNode node = tree;
			for (final AtomicFeature<?> feature : nodeFeatures) {
				final Collection<FPNode> nodeList = nodeTable.get(feature);
				node = node.addChild(feature, nodeList);
			}
		}

		final Set<Set<AtomicFeature<?>>> fp = Sets.<Set<AtomicFeature<?>>>newHashSet();
		
		final Set<AtomicFeature<?>> allAtomicFeatures = nodeTable.keySet();
		for (AtomicFeature<?> feature : allAtomicFeatures) {
			fp.add(Collections.<AtomicFeature<?>>singleton(feature));
		}
		
		final Comparator<AtomicFeature<?>> comparator = new Comparator<AtomicFeature<?>>() {

			@Override
			public int compare(final AtomicFeature<?> o1, final AtomicFeature<?> o2) {
				final int count1 = featureCount.count(o1);
				final int count2 = featureCount.count(o2);
				return count1 - count2;
			}
		};
		fpGrowth(tree, minSupport, comparator, nodeTable, Collections.<AtomicFeature<?>> emptySet(), fp);

		this.fp = fp;
	}

	public Set<Set<AtomicFeature<?>>> getFrequetPatterns() {
		return fp;
	}

	private void fpGrowth(final FPNode tree, final int minSupport, final Comparator<AtomicFeature<?>> comparator, final Multimap<AtomicFeature<?>, FPNode> nodeTable,
			final Set<AtomicFeature<?>> prefix, final Set<Set<AtomicFeature<?>>> fp) {
		final ImmutableList<AtomicFeature<?>> leastFreqNode = FluentIterable.from(nodeTable.keySet()).toSortedList(comparator);

		for (final AtomicFeature<?> leafFeature : leastFreqNode) {
			final Collection<FPNode> nodeList = nodeTable.get(leafFeature);
			final Map<FPNode, FPNode> conditionalNodes = Maps.newIdentityHashMap();
			final Multimap<AtomicFeature<?>, FPNode> conditionalNodeTable = LinkedListMultimap.create();
			for (final FPNode leaf : nodeList) {
				final int count = leaf.getCount();
				getConditionalNode(leaf, count, conditionalNodes, conditionalNodeTable);
			}

			final List<AtomicFeature<?>> toRemove = Lists.newLinkedList();
			toRemove.add(leafFeature);

			final Set<AtomicFeature<?>> features = conditionalNodeTable.keySet();
			for (final AtomicFeature<?> feature : features) {
				if (!feature.equals(leafFeature)) {
					final Collection<FPNode> nodes = conditionalNodeTable.get(feature);
					int count = 0;
					for (final FPNode node : nodes) {
						count += node.getCount();
					}
					if (count < minSupport) {
						toRemove.add(feature);
					}
				}
			}

			for (final AtomicFeature<?> feature : toRemove) {
				final Collection<FPNode> nodes = conditionalNodeTable.get(feature);
				if(nodes != null) {
					for (final FPNode node : nodes) {
						final FPNode parent = node.getParent();
						final List<FPNode> parentChildren = parent.getChildren();
						final List<FPNode> children = node.getChildren();
						for (final FPNode child : children) {
							child.setParent(parent);
							parentChildren.add(child);
						}
						parentChildren.remove(node);
					}
					conditionalNodeTable.removeAll(feature);
				}
			}

			final FPNode condRoot = conditionalNodes.get(tree);
			final Set<AtomicFeature<?>> freqFeatures = conditionalNodeTable.keySet();
			if (!freqFeatures.isEmpty()) {
				boolean singlePath = true;
				for (final AtomicFeature<?> feature : freqFeatures) {
					final Collection<FPNode> nodes = conditionalNodeTable.get(feature);
					if (nodes.size() > 1) {
						singlePath = false;
						break;
					}
				}

				if (singlePath) {
					final List<AtomicFeature<?>> finalPath = Lists.newArrayList();
					List<FPNode> childen = condRoot.getChildren();
					while (!childen.isEmpty()) {
						final FPNode child = childen.get(0);
						final AtomicFeature<?> feature = child.getFeature();
						finalPath.add(feature);
						childen = child.getChildren();
					}

					final Set<Set<AtomicFeature<?>>> combinations = TfIdfUtils.combinations(finalPath);
					for (Set<AtomicFeature<?>> comb : combinations) {
						comb.addAll(prefix);
						comb.add(leafFeature);
						fp.add(comb);
					}
				} else {
					for (final AtomicFeature<?> feature : freqFeatures) {
						final Set<AtomicFeature<?>> newFp = Sets.newHashSet();
						newFp.addAll(prefix);
						newFp.add(leafFeature);
						newFp.add(feature);
						fp.add(newFp);
					}
					fpGrowth(condRoot, minSupport, comparator, conditionalNodeTable, Collections.<AtomicFeature<?>>singleton(leafFeature), fp);
				}
			}
		}
	}

	private FPNode getConditionalNode(final FPNode node, final int count, final Map<FPNode, FPNode> conditionalNodes, final Multimap<AtomicFeature<?>, FPNode> nodeTable) {
		final FPNode condNode = conditionalNodes.get(node);
		if (condNode != null) {
			return condNode;
		}

		final FPNode parent = node.getParent();
		FPNode condParent;
		if (parent == null) {
			condParent = null;
		} else {
			condParent = getConditionalNode(parent, 0, conditionalNodes, nodeTable);
		}

		final AtomicFeature<?> feature = node.getFeature();
		final FPNode newNode = new FPNode(condParent, feature, count);
		if (condParent != null) {
			condParent.addChild(newNode);
		}
		if (feature != null) {
			nodeTable.put(feature, newNode);
		}
		conditionalNodes.put(node, newNode);

		return newNode;
	}

	private static class FPNode {
		private FPNode parent;
		private final AtomicFeature<?> feature;
		private final List<FPNode> children;
		private int count;

		public FPNode(final FPNode parent, final AtomicFeature<?> feature) {
			this(parent, feature, 1);
		}

		public FPNode(final FPNode parent, final AtomicFeature<?> feature, final int count) {
			this.parent = parent;
			this.feature = feature;
			this.children = Lists.newLinkedList();
			this.count = count;
		}

		public void setParent(final FPNode parent) {
			this.parent = parent;
		}

		public FPNode getParent() {
			return parent;
		}

		public AtomicFeature<?> getFeature() {
			return feature;
		}

		public List<FPNode> getChildren() {
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

		public FPNode addChild(final AtomicFeature<?> token, final Collection<FPNode> nodeList) {
			FPNode node = null;
			for (final FPNode child : children) {
				final AtomicFeature<?> childToken = child.getFeature();
				if (childToken.equals(token)) {
					node = child;
					break;
				}
			}

			if (node == null) {
				node = new FPNode(this, token);
				children.add(node);
				nodeList.add(node);
			} else {
				node.incrementCount();
			}

			return node;
		}

		public void addChild(final FPNode node) {
			children.add(node);
			final int count = node.getCount();
			FPNode parentNode = this;
			while (parentNode != null) {
				parentNode.incrementCount(count);
				parentNode = parentNode.getParent();
			}
		}

		@Override
		public String toString() {
			final StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("{feature: ").append(feature);
			stringBuilder.append(", count: ").append(count);
			stringBuilder.append(", children: [");
			for (FPNode child : children) {
				final AtomicFeature<?> childFeature = child.getFeature();
				stringBuilder.append(childFeature).append(", ");
			};
			stringBuilder.append("]}");
			return  stringBuilder.toString();
		}
	}
}
