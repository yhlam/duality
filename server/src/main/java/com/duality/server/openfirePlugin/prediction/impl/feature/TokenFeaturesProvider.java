package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;
import com.google.common.collect.Lists;

public class TokenFeaturesProvider implements AtomicFeaturesProvider {
	public static List<String> extractTokens(final String message) {
		final StringReader reader = new StringReader(message);
		final IKAnalyzer ikAnalyzer = new IKAnalyzer();
		final TokenStream tokenStream = ikAnalyzer.tokenStream("", reader);
		final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		final EnglishStemmer stemmer = new EnglishStemmer();
		final List<String> tokens = Lists.newLinkedList();
		try {
			while (tokenStream.incrementToken()) {
				final String term = charTermAttribute.toString();
				stemmer.setCurrent(term);
				stemmer.stem();
				final String stemmed = stemmer.getCurrent();
				tokens.add(stemmed);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		ikAnalyzer.close();

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
