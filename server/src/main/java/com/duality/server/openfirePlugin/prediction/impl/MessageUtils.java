package com.duality.server.openfirePlugin.prediction.impl;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;

public class MessageUtils {
	public static String[] extractTokens(final HistoryEntry history) {
		final String message = history.getMessage();

		final String[] tokens = message.split("\\W+");
		for (int i = 0; i < tokens.length; i++) {
			final String token = tokens[i];
			final String lowerCase = token.toLowerCase();
			tokens[i] = lowerCase;
		}

		return tokens;
	}
}
