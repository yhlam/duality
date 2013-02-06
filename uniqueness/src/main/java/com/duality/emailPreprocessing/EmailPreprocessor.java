package com.duality.emailPreprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class EmailPreprocessor {
	/*
	 * For the source of the data, please visit http://www.cs.cmu.edu/~enron/
	 */
	public static void main(String[] args) {
		System.out.println("Started");
		final File directory = new File(
				"C:\\Users\\K\\Documents\\enron_mail_20110402\\maildir");
		final File[] files = directory.listFiles();

		// Print out the name of files in the directory
		/*
		 * for(int fileIndex = 0; fileIndex < files.length; fileIndex++){ File
		 * subDirectory = new File(files[fileIndex].toString()+"\\sent_items");
		 * if(subDirectory.exists()){ File[] subFiles =
		 * subDirectory.listFiles();
		 * 
		 * for(int i = 0; i < subFiles.length; i++){
		 * System.out.println(subFiles[i].toString()); } } }
		 */

		for (int fileIndex = 0; fileIndex < files.length; fileIndex++) {
			final File subDirectory = new File(files[fileIndex].toString()
					+ "\\sent_items");
			if (subDirectory.exists()) {
				final File[] subFiles = subDirectory.listFiles();
				FileOutputStream outputStream = null;
				PrintWriter outPrintWriter = null;
				final File newFile = new File("Emails/A" + fileIndex);

				try {
					outputStream = new FileOutputStream(newFile);
				} catch (final FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				outPrintWriter = new PrintWriter(outputStream);

				final int max = subFiles.length / 10;
				for (int i = 0; i < max; i++) {
					// System.out.println(subFiles[i].toString());
					Boolean isMessage = false;
					FileReader trainingFileReader = null;
					try {
						if (subFiles[i].isFile()) {
							trainingFileReader = new FileReader(subFiles[i]);
						} else {
							continue;
						}
					} catch (final FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					}
					final BufferedReader trainingBufferedReader = new BufferedReader(
							trainingFileReader);
					try {
						String trainingOutputString;

						while ((trainingOutputString = trainingBufferedReader
								.readLine()) != null) {
							trainingOutputString = trainingOutputString.trim();

							if (trainingOutputString.equals("")) {
								isMessage = true;
							}
							if (trainingOutputString.contains("-----Original")
									|| trainingOutputString
									.contains("---------------------- Forwarded")||
									trainingOutputString.contains("<OMNI>") ) {
								break;
							}
							/*
							 *  New Codes to improve data quality, 13-1-2013 By Kenny Tam
							 *  <OMNI>
							 *	<OMNINotes></OMNINotes>
							 *	<OMNIPAB></OMNIPAB>
							 *	<OMNIToDos></OMNIToDos>
							 *	<OMNICalendarEntries>
							 *	</OMNICalendarEntries>
							 *	</OMNI>
							 */
							

							// End of New codes
							if (isMessage == true) {
								if (!trainingOutputString.isEmpty()) {
									outPrintWriter
									.println(trainingOutputString);
									outPrintWriter.flush();
								}
							}

						}
					} catch (final IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (trainingBufferedReader != null) {
						try {
							trainingBufferedReader.close();
						} catch (final IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (trainingFileReader != null) {
						try {
							trainingFileReader.close();
						} catch (final IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (final IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println("Completed");
	}

}
