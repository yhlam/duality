package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.wltea.analyzer.lucene.IKTokenizer;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;
import com.google.common.collect.Lists;

public class TokenFeaturesProvider implements AtomicFeaturesProvider {

	private static final Map<String, String> emoji = getEmojiMapping();

	private static Map<String, String> getEmojiMapping() {
		final Map<String, String> emoji = new HashMap<String, String>();
		// New emoji
		// FIXME: They should have a \u0001 before them, don't want to handle this now LOL
		emoji.put("\uF60B", " smile ");
		emoji.put("\uF60E", " cool ");
		emoji.put("\uF605", " smile ");
		emoji.put("\uF606", " laugh ");
		emoji.put("\uF608", " evil ");
		emoji.put("\uF634", " sleepy ");

		// Old emoji
		emoji.put("\uE40B", " shocked ");
		emoji.put("\uE40C", " sick ");
		emoji.put("\uE40D", " surprised ");
		emoji.put("\uE40E", " dislike ");
		emoji.put("\uE056", " smile ");
		emoji.put("\uE106", " love ");
		emoji.put("\uE107", " shocked ");
		emoji.put("\uE402", " tease ");
		emoji.put("\uE403", " sad ");
		emoji.put("\uE404", " smile ");
		emoji.put("\uE405", " winked ");
		emoji.put("\uE411", " cry ");
		emoji.put("\uE412", " laugh ");
		emoji.put("\uE416", " angry ");
		emoji.put("\uE418", " kiss ");
		return emoji;
	}

	public static List<String> extractTokens(final String message) {

		final StringBuilder msgBuilder = new StringBuilder();
		final String[] characters = message.split("");

		for (final String character : characters) {
			final String replacement = emoji.get(character);
			if (replacement != null) {
				msgBuilder.append(replacement);
			} else {
				msgBuilder.append(character);
			}
		}

		final String emojiFreeMsg = msgBuilder.toString();
		final StringReader reader = new StringReader(emojiFreeMsg);
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
