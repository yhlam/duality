package com.duality.prototype.ifIdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.tartarus.snowball.ext.EnglishStemmer;

import com.duality.prototype.ifIdf.IfIdfModel.Context.TokenTurn;

/**
 * User: hei 
 * Date: 9/2/12 
 * Time: 8:27 PM
 */
public class IfIdfModel {
	private final Map<String, Integer> _utteranceCount;
	private final int _perviousTurnNum;
	private final List<Context> _contexts;

	public IfIdfModel(final String corpusFileName, final int perviousTurnNum) {
		_perviousTurnNum = perviousTurnNum;
		final List<String> corpus = new LinkedList<String>();
		final File file = new File(corpusFileName);
		try {
			final Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				final String line = scanner.nextLine();
				corpus.add(line);
			}
			scanner.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		final int corpusSize = corpus.size();
		_utteranceCount = new HashMap<String, Integer>();
		final List<Map<String, Integer>> utteranceTokenCounts = new ArrayList<Map<String, Integer>>(corpusSize);
		final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		final EnglishStemmer stemmer = new EnglishStemmer();
		for (final String utterance : corpus) {
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

			final Set<String> tokenSet = tokenCount.keySet();
			for (final String token : tokenSet) {
				final Integer currentCount = _utteranceCount.get(token);
				if (currentCount == null) {
					_utteranceCount.put(token, 1);
				} else {
					_utteranceCount.put(token, currentCount + 1);
				}
			}

			utteranceTokenCounts.add(tokenCount);
		}

		_contexts = new ArrayList<Context>(corpusSize);

		int utteranceIndex = 0;
		final Queue<String> history = new LinkedList<String>();
		for (final String utterance : corpus) {
			final Map<TokenTurn, Double> weights = new HashMap<TokenTurn, Double>();

			for (int i = 1; i <= _perviousTurnNum; i++) {
				final int tokenCountIndex = utteranceIndex - i;
				if (tokenCountIndex >= 0) {
					final Map<String, Integer> tokenCount = utteranceTokenCounts.get(tokenCountIndex);
					final Collection<Integer> counts = tokenCount.values();
					int maxCount = 0;
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

						final Integer utteranceCount = _utteranceCount.get(token);
						final double invertDocFreq = Math.log(corpusSize / utteranceCount.doubleValue());

						final double turnAdj = Math.exp(-i * i / 2.0);

						final double weight = termFreq * invertDocFreq * turnAdj;

						final TokenTurn tokenTurn = new TokenTurn(i, token);
						weights.put(tokenTurn, weight);
					}
				}
			}

			final Context context = new Context(utterance, weights, new ArrayList<String>(history));
			_contexts.add(context);

			history.add(utterance);
			while (history.size() > perviousTurnNum) {
				history.poll();
			}

			utteranceIndex++;
		}
	}

	public Context getContextAt(final int index) {
		return _contexts.get(index);
	}

	public List<Context> getContexts() {
		return _contexts;
	}

	public int size() {
		return _contexts.size();
	}

	public Set<String> getAllTokens() {
		return Collections.unmodifiableSet(_utteranceCount.keySet());
	}

	public int getPerviousTurnNum() {
		return _perviousTurnNum;
	}

	public int getUtteranceCount(final String token) {
		final Integer count = _utteranceCount.get(token);
		return count == null ? 0 : count;
	}

	public static class Context {
		private final String _utterance;
		private final Map<TokenTurn, Double> _weight;
		private final List<String> _history;

		public Context(final String utterance, final Map<TokenTurn, Double> weight, final List<String> history) {
			_utterance = utterance;
			_weight = weight;
			_history = history;
		}

		public String getUtterance() {
			return _utterance;
		}

		public Map<TokenTurn, Double> getWeight() {
			return _weight;
		}

		public List<String> getHistory() {
			return _history;
		}
		
		@Override
		public String toString() {
			return _utterance + ": " + _weight;
		}

		public static class TokenTurn {
			private final int _turn;
			private final String _token;
			private final int _hashCode;

			public TokenTurn(final int turn, final String token) {
				_turn = turn;
				_token = token;
				_hashCode = _turn + _token.hashCode() * 31;
			}

			public String getToken() {
				return _token;
			}

			public int getTurn() {
				return _turn;
			}

			@Override
			public boolean equals(final Object obj) {
				if (this == obj) {
					return true;
				}

				if (obj instanceof TokenTurn) {
					final TokenTurn that = (TokenTurn) obj;
					return _turn == that._turn && _token.equals(that._token);
				}

				return false;
			}

			@Override
			public int hashCode() {
				return _hashCode;
			}

			@Override
			public String toString() {
				return "(" + _token + ", " + _turn + ")";
			}
		}
	}
}
