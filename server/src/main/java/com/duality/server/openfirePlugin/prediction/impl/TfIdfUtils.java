package com.duality.server.openfirePlugin.prediction.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;
import com.google.common.collect.Sets;

public class TfIdfUtils {
	public static <T> void addAtomicFeature(List<AtomicFeature<?>> terms, FeatureType type, T value) {
		final AtomicFeature<T> term = new AtomicFeature<T>(type, value);
		terms.add(term);
	}
	
    public static Set<Set<AtomicFeature<?>>> combinations(List<AtomicFeature<?>> features) {
    	final Set<Set<AtomicFeature<?>>> comb = Sets.newHashSet();
    	combinations(Collections.<AtomicFeature<?>>emptySet(), features, comb);
    	return comb;
    }
    
    private static void combinations(Set<AtomicFeature<?>> prefix, List<AtomicFeature<?>> features, Set<Set<AtomicFeature<?>>> comb) {
    	if(!prefix.isEmpty()) {
    		comb.add(prefix);
    	}
    	
    	final int size = features.size();
    	int i = 0;
    	for (AtomicFeature<?> feature : features) {
    		final Set<AtomicFeature<?>> newPrefix = Sets.newHashSetWithExpectedSize(prefix.size() + 1);
    		newPrefix.addAll(prefix);
    		newPrefix.add(feature);
    		combinations(newPrefix, features.subList(i+1, size), comb);
			i++;
		}
    }  
}
