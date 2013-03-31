package com.duality.server.openfirePlugin.prediction.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;
import com.google.common.collect.Sets;

public class TfIdfUtils {
	public static <T> void addAtomicFeature(List<AtomicFeature<?>> terms, FeatureType type, T value) {
		if(value != null) {
			final AtomicFeature<T> term = new AtomicFeature<T>(type, value);
			terms.add(term);
		}
	}
	
    public static <T> Set<Set<T>> combinations(List<T> features) {
    	final Set<Set<T>> comb = Sets.newHashSet();
    	combinations(Collections.<T>emptySet(), features, comb);
    	return comb;
    }
    
    private static <T> void combinations(Set<T> prefix, List<T> features, Set<Set<T>> comb) {
    	if(!prefix.isEmpty()) {
    		comb.add(prefix);
    	}
    	
    	final int size = features.size();
    	int i = 0;
    	for (T feature : features) {
    		final Set<T> newPrefix = Sets.newHashSetWithExpectedSize(prefix.size() + 1);
    		newPrefix.addAll(prefix);
    		newPrefix.add(feature);
    		combinations(newPrefix, features.subList(i+1, size), comb);
			i++;
		}
    }
}
