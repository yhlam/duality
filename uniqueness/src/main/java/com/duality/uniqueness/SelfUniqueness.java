package com.duality.uniqueness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.tartarus.snowball.ext.EnglishStemmer;

public class SelfUniqueness {

	private final int tokenNum;
	private final double score;
	private final double groupNum;
	private final double avgLength;

	/**
	 * @param filename
	 *            File to analysis
	 * @param n
	 *            Number of token as a group
	 * @throws IOException
	 */
	public SelfUniqueness(final String filename, final int n) throws IOException {
		this.tokenNum = n;

		final FileReader fileReader = new FileReader(filename);
		final BufferedReader reader = new BufferedReader(fileReader);

		final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
		final EnglishStemmer stemer = new EnglishStemmer();
		final Set<List<String>> coreSet = new HashSet<List<String>>();

		double tokenNum = 0;
		int lineNum = 0;
		int groupNum = 0;
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

			final int maxIndex = tokens.length - n;
			tokenNum += tokens.length;

			for (int i = 0; i < maxIndex; i++) {
				final String[] group = new String[n];
				System.arraycopy(tokens, i, group, 0, n);
				coreSet.add(Arrays.asList(group));
				groupNum++;
			}
			lineNum++;
		}

		reader.close();

		score = (double) coreSet.size() / groupNum;
		this.groupNum = groupNum;
		this.avgLength = tokenNum / lineNum;
	}

	public double getScore() {
		return score;
	}

	public int getTokenNumInGroup() {
		return tokenNum;
	}

	public double getGroupNum() {
		return groupNum;
	}

	public double getAvgLength() {
		return avgLength;
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
		fileWriter.write("filename, score, groupNum, avgLength\n");

		for (final String filename : filenames) {
			final SelfUniqueness uniqueness = new SelfUniqueness(dataSetDir + "/" + filename, tokenNum);
			final double score = uniqueness.getScore();
			final double groupNum = uniqueness.getGroupNum();
			final double avgLength = uniqueness.getAvgLength();
			fileWriter.write(filename + ", " + score + ", " + groupNum + ", " + avgLength + "\n");
		}
		fileWriter.close();
	}
}
