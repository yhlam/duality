package com.duality.uniqueness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.tartarus.snowball.ext.EnglishStemmer;

public class DistSelfUniqueness {

	private final int groupSize;
	private final Map<List<String>, Integer> distribution;

	/**
	 * @param filename File to analysis
	 * @param n Number of token as a group
	 * @throws IOException
	 */
	public DistSelfUniqueness(final int n) throws IOException {
		this.groupSize = n;
		this.distribution = new HashMap<List<String>, Integer>();
	}

	public void provide(final String filename) throws FileNotFoundException, IOException {
		final FileReader fileReader = new FileReader(filename);
		final BufferedReader reader = new BufferedReader(fileReader);

		final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		final EnglishStemmer stemer = new EnglishStemmer();

		String line;
		while ((line = reader.readLine()) != null) {
			final String[] tokens = tokenizer.tokenize(line);
			for (int i = 0; i < tokens.length; i++) {
				final String token = tokens[i];
				stemer.setCurrent(token);
				stemer.stem();
				final String stemmed = stemer.getCurrent();
				tokens[i] = stemmed;
			}

			final int maxIndex = tokens.length - groupSize;

			for (int i = 0; i <= maxIndex; i++) {
				final String[] group = new String[groupSize];
				System.arraycopy(tokens, i, group, 0, groupSize);
				final List<String> key = Arrays.asList(group);
				final Integer value = distribution.get(key);
				if(value != null) {
					distribution.put(key, value + 1);
				}
				else {
					distribution.put(key, 1);
				}
			}
		}

		reader.close();
	}
	
	public Map<List<String>, Integer> getDistribution() {
		return distribution;
	}

	public int getTokenNumInGroup() {
		return groupSize;
	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Invalid args.\nUsage: selfUniquness [dataSetDir] [tokenNumInGroup] [output]");
			return;
		}

		final String dataSetDir = args[0];
		final File dataSet = new File(dataSetDir);
		if (!dataSet.isDirectory()) {
			System.out.println(dataSetDir + " is not a directory!");
			return;
		}

		final String tokenNumStr = args[1];
		final int tokenNum;
		try {
			tokenNum = Integer.parseInt(tokenNumStr);
		} catch (final NumberFormatException e) {
			System.out.println(tokenNumStr + " is not a number!");
			return;
		}

		final String[] filenames = dataSet.list();
		final FileWriter fileWriter = new FileWriter(args[2]);
		fileWriter.write("Count, Frequency\n");

		final DistSelfUniqueness uniqueness = new DistSelfUniqueness(tokenNum);
		for (final String filename : filenames) {
			uniqueness.provide(dataSetDir + "/" + filename);
		}
		
		final Map<List<String>, Integer> distribution = uniqueness.getDistribution();
		final Set<Entry<List<String>, Integer>> entries = distribution.entrySet();
		final Map<Integer, Integer> hist = new TreeMap<Integer, Integer>();
		for (Entry<List<String>, Integer> entry : entries) {
			final Integer count = entry.getValue();
			final Integer value = hist.get(count);
			if(value != null) {
				hist.put(count, value + 1);
			}
			else {
				hist.put(count, 1);
			}
		}
		
		final Set<Entry<Integer, Integer>> histEntries = hist.entrySet();
		for (Entry<Integer, Integer> entry : histEntries) {
			final Integer groupNum = entry.getKey();
			final Integer count = entry.getValue();
			fileWriter.write("\"" + groupNum + "\", " + count + "\n");
		}
		
		fileWriter.close();
	}
}
