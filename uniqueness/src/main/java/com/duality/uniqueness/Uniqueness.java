package com.duality.uniqueness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.tartarus.snowball.ext.EnglishStemmer;

public class Uniqueness {

	private final Set<List<String>> corpus;
	private final int n;

	public Uniqueness(final String filename, final int n) throws IOException {
		this.n = n;

		final FileReader fileReader = new FileReader(filename);
		final BufferedReader reader = new BufferedReader(fileReader);

		final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		final EnglishStemmer stemer = new EnglishStemmer();

		corpus = new HashSet<List<String>>();

		String line;
		while ((line = reader.readLine()) != null) {
			final String[] tokens = tokenizer.tokenize(line);
			for (int i = 0; i < tokens.length; i++) {
				stemer.setCurrent(tokens[i]);
				tokens[i] = stemer.getCurrent();
			}

			final int maxIndex = tokens.length - n;

			for (int i = 0; i < maxIndex; i++) {
				final String[] group = new String[n];
				System.arraycopy(tokens, i, group, 0, n);
				corpus.add(Arrays.asList(group));
			}
		}

		reader.close();
	}

	public double score(final String filename) throws IOException {
		final FileReader fileReader = new FileReader(filename);
		final BufferedReader reader = new BufferedReader(fileReader);

		final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

		int repeatedCount = 0;
		int size = 0;
		String line;
		while ((line = reader.readLine()) != null) {
			final String[] tokens = tokenizer.tokenize(line);
			final int maxIndex = tokens.length - n;

			for (int i = 0; i < maxIndex; i++) {
				final String[] group = new String[n];
				System.arraycopy(tokens, i, group, 0, n);
				final boolean repeated = corpus.contains(Arrays.asList(group));
				if (repeated) {
					repeatedCount++;
				}

				size++;
			}
		}

		reader.close();

		final double uniquness = (double) repeatedCount / (double) size;
		return uniquness;
	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("Invalid args.\nUsage: uniquness [trainingSet] [dataSetDir] [tokenNumInGroup]");
			return;
		}

		final String dataSetDir = args[1];
		final File dataSet = new File(dataSetDir);
		if (!dataSet.isDirectory()) {
			System.out.println(dataSetDir + " is not a directory!");
			return;
		}

		final String tokenNumStr = args[2];
		final int tokenNum;
		try {
			tokenNum = Integer.parseInt(tokenNumStr);
		} catch (final NumberFormatException e) {
			System.out.println(tokenNumStr + " is not a number!");
			return;
		}

		final Uniqueness uniqueness = new Uniqueness(args[0], tokenNum);
		final String[] filenames = dataSet.list();
		for (final String filename : filenames) {
			final double score = uniqueness.score(dataSetDir + "/" + filename);
			System.out.println(filename + ": " + score);
		}
	}
}
