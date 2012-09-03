package com.duality.prototype.ifIdf;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.tartarus.snowball.ext.EnglishStemmer;

import com.duality.prototype.ifIdf.IfIdfModel.Context;
import com.duality.prototype.ifIdf.IfIdfModel.Context.TokenTurn;

/**
 * User: hei 
 * Date: 9/2/12 
 * Time: 8:25 PM
 */
public class IfIdfChatBot {
	private final IfIdfModel _model;

	public IfIdfChatBot(final IfIdfModel model) {
		_model = model;
	}

	public String guess(final List<String> chatLog) {
		final int perviousTurnNum = _model.getPerviousTurnNum();
		if (perviousTurnNum < chatLog.size()) {
			throw new RuntimeException("The size of chat log should not be larger than " + perviousTurnNum);
		}

		final Map<TokenTurn, Double> context = new HashMap<TokenTurn, Double>();

		final int corpusSize = _model.size();
		int turn = chatLog.size();

		final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		final EnglishStemmer stemmer = new EnglishStemmer();

		for (final String utterance : chatLog) {
			final Map<String, Integer> tokenCount = new HashMap<String, Integer>();
			final String[] tokens = tokenizer.tokenize(utterance);
			for (final String token : tokens) {
				final String lowerToken = token.toLowerCase();
				stemmer.setCurrent(lowerToken);
				stemmer.stem();
				final String stemmedToken = stemmer.getCurrent();
				final Integer currentCount = tokenCount.get(stemmedToken);
				if (currentCount == null) {
					tokenCount.put(stemmedToken, 1);
				} else {
					tokenCount.put(stemmedToken, currentCount + 1);
				}
			}

			int maxCount = 0;
			final Collection<Integer> counts = tokenCount.values();
			for (final Integer count : counts) {
				if (count > maxCount) {
					maxCount = count;
				}
			}

			final Set<Entry<String, Integer>> tokenCountSet = tokenCount.entrySet();
			for (final Entry<String, Integer> tokenCountEntry : tokenCountSet) {
				final String token = tokenCountEntry.getKey();
				final Integer count = tokenCountEntry.getValue();

				final double termFreq = 1 + Math.log(count.doubleValue() / maxCount);

				final int utteranceCount = _model.getUtteranceCount(token) + 1;
				final double invertDocFreq = Math.log(corpusSize / utteranceCount);

				final double turnAdj = Math.exp(-turn * turn / 2.0);

				final double weight = termFreq * invertDocFreq * turnAdj;

				final TokenTurn tokenTurn = new TokenTurn(turn, token);
				context.put(tokenTurn, weight);
			}
			turn--;
		}

		double maxValue = 0;
		final PriorityQueue<Response> matachUtterance = new PriorityQueue<Response>();

		final List<Context> contexts = _model.getContexts();
		for (final Context modelContext : contexts) {
			final Map<TokenTurn, Double> weightMap = modelContext.getWeight();
			final Collection<Double> modelWeightValue = weightMap.values();
			double norm = 0;
			for (final Double weight : modelWeightValue) {
				norm += weight * weight;
			}
			norm = Math.sqrt(norm);

			if (norm == 0) {
				continue;
			}

			final Set<TokenTurn> commonKeys = new HashSet<TokenTurn>(weightMap.keySet());
			commonKeys.retainAll(context.keySet());

			double dotProduct = 0;
			for (final TokenTurn tokenTurn : commonKeys) {
				final Double weight = context.get(tokenTurn);
				final Double modelWeight = weightMap.get(tokenTurn);
				dotProduct += weight * modelWeight;
			}

			final double value = dotProduct / norm;

			if (value > maxValue) {
				maxValue = value;
				final double bound = maxValue * 0.95;
				while (!matachUtterance.isEmpty() && matachUtterance.peek().getScore() < bound) {
					matachUtterance.poll();
				}
			}

			if (value >= maxValue * 0.95 && value > 0) {
				final String thisUtterance = modelContext.getUtterance();
				final StringBuilder sb = new StringBuilder(thisUtterance);
				sb.append("\n\n");
				final List<String> history = modelContext.getHistory();
				int i = perviousTurnNum;
				for (final String historyUtterance : history) {
					sb.append("n-").append(i).append("> ");
					sb.append(historyUtterance).append("\n");
					i--;
				}
				final Response response = new Response(sb.toString(), value);
				matachUtterance.add(response);
			}
		}

		final int matchNum = matachUtterance.size();
		if (matchNum == 0) {
			return "I don't know";
		} else {
			final int index = new Random().nextInt(matchNum);
			int i = 0;
			for (final Response response : matachUtterance) {
				if (i == index) {
					return response.getResponse();
				}

				i++;
			}

			throw new IndexOutOfBoundsException();
		}
	}

	private static class Response implements Comparable<Response> {
		private final String _response;
		private final double _score;

		public Response(final String response, final double score) {
			_response = response;
			_score = score;
		}

		public String getResponse() {
			return _response;
		}

		public double getScore() {
			return _score;
		}

		public int compareTo(final Response o) {
			if (o == null) {
				return 1;
			}

			return Double.compare(this._score, o._score);
		}

	}

	public static void main(final String[] args) {
		final IfIdfModel model = new IfIdfModel("corpus", 2);
		final IfIdfChatBot chatbot = new IfIdfChatBot(model);

		final Scanner scanner = new Scanner(System.in);
		String lastLine = "";
		try {
			while (true) {
				System.out.print("You: ");
				final String line = scanner.nextLine();
				final String response = chatbot.guess(Arrays.asList(lastLine, line));
				System.out.println("Bot: " + response);
				lastLine = response;
			}
		} finally {
			scanner.close();
		}
	}
}
