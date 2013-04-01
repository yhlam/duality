package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.wltea.analyzer.lucene.IKTokenizer;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;
import com.google.common.collect.Lists;

public class TokenFeaturesProvider implements AtomicFeaturesProvider {
	public static List<String> extractTokens(final String message) {
		final StringReader reader = new StringReader(message);
		final IKTokenizer ikTokenizer = new IKTokenizer(reader, false);
		final CharTermAttribute charTermAttribute = ikTokenizer.addAttribute(CharTermAttribute.class);
		final EnglishStemmer stemmer = new EnglishStemmer();
		final List<String> tokens = Lists.newLinkedList();
		try {
			while (ikTokenizer.incrementToken()) {
				final String term = charTermAttribute.toString();
				stemmer.setCurrent(term);
				stemmer.stem();
				final String stemmed = stemmer.getCurrent();
				tokens.add(stemmed);
			}
			ikTokenizer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		return tokens;
	}

	@Override
	public void constructFeatures(final HistoryEntry history, final List<AtomicFeature<?>> features) {
		final String message = history.getMessage();
		final List<String> tokens = extractTokens(message);
		for (final String token : tokens) {
			TfIdfUtils.addAtomicFeature(features, FeatureType.TOKEN, token);
		}
	}
}
