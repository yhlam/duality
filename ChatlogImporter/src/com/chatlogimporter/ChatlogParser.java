package com.chatlogimporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatlogParser {
	private File directory;
	private List<MessageModel> list;
	private String language;
	private int lastMonth;

	public ChatlogParser(File dir){
		directory = dir;
		list = new ArrayList<MessageModel>();
		lastMonth = -1;
	}

	public String getRecipent(String dir){
		String temp = "";
		if(dir.contains("WhatsApp")){
			if(dir.contains("的對話")){
				language = "Chinese";
				temp = dir.replaceAll(".*WhatsApp 與", "");
				temp = temp.replaceAll("的對話.*", "");
			}else{
				language = "English";
				temp = dir.replaceAll(".*WhatsApp Chat with ", "");
				temp = temp.replaceAll("\\..*", "");
			}
		}
		return temp;
	}

	public List<MessageModel> start(){
		return read(directory, "");
	}

	public List<MessageModel> read(File file, String parent){
		List<MessageModel> list = new ArrayList<MessageModel>();
		File[] files = file.listFiles();
		String[] fileName;
		for (int i = 0; i < files.length; i++){
			if(files[i].isDirectory()) {
				fileName = files[i].toString().split("\\\\");
				list.addAll(read(files[i], fileName[fileName.length-1]));
			} else {
				BufferedReader reader = null;
				try {
					String temp = "";
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i]),"UTF8"));
					while ((temp = reader.readLine()) != null){
						if(!(temp.equals(""))){
							fileName = files[i].toString().split("\\\\");
							MessageModel model = parse(temp, parent, getRecipent(fileName[fileName.length-1]));
							list.add(model);
						}
					}
					lastMonth = -1;
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try{
						if(reader != null)
							reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return list;
	}

	public String SQLParse(String s){
		String temp = s.replaceAll("'", "''");
		return temp;
	}


	@SuppressWarnings("deprecation")
	public MessageModel parse(String s, String owner, String recipent){
		MessageModel model = null;
		String re;
		Pattern pattern;
		Matcher matcher;
		Boolean isFound;
		if(language == "English"){
			re = "[0-9/: ]+[AP]M";
			pattern = Pattern.compile(re);
			matcher = pattern.matcher(s);
			isFound = matcher.find();
			if(isFound){
				int end = matcher.end();
				String dateTime = matcher.group(0);	

				String nameText = s.substring(end).trim();
				int nameEnd = nameText.indexOf(": ");
				nameText = nameText.substring(nameEnd+1);
				nameEnd = nameText.indexOf(": ");
				String senderName = SQLParse(nameText.substring(0, nameEnd).trim());
				String recipentName = "";
				if (senderName.equals(owner))
					recipentName = recipent;
				else if (senderName.equals(recipent))
					recipentName = owner;
				String text = SQLParse(nameText.substring(nameEnd+1).trim());
				model = new MessageModel(senderName, recipentName, text, dateTime);
			}
		} else if(language == "Chinese"){
			re = ".*- ";
			pattern = Pattern.compile(re);
			matcher = pattern.matcher(s);
			isFound = matcher.find();
			if(isFound){
				int end = matcher.end();
				String dateTimeString = matcher.group(0);
				SimpleDateFormat format = new SimpleDateFormat("M '月' d '日' a h:mm", Locale.TRADITIONAL_CHINESE);
				SimpleDateFormat format2 = new SimpleDateFormat("H:mm, M '月' d", Locale.TRADITIONAL_CHINESE);
				Date parsed = null;
				try {
					parsed = format.parse(dateTimeString);
				} catch (ParseException e) {
					try {
						parsed = format2.parse(dateTimeString);
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
					
				}
				
				int month = parsed.getMonth();
				if (lastMonth <0){
					lastMonth = month;
				} else if (lastMonth > month){
					parsed.setYear(parsed.getYear()+1);
				}
				
				SimpleDateFormat toFormat = new SimpleDateFormat("d/M/yy h:mm:ss a", Locale.ENGLISH);
				String engFormatTime = toFormat.format(parsed);
				
				String nameText = s.substring(end).trim();
				int nameEnd = nameText.indexOf("- ");
				nameText = nameText.substring(nameEnd+1);
				nameEnd = nameText.indexOf(": ");
				String senderName = SQLParse(nameText.substring(0, nameEnd).trim());
				String recipentName = "";
				if (senderName.equals(owner))
					recipentName = recipent;
				else if (senderName.equals(recipent))
					recipentName = owner;
				String text = SQLParse(nameText.substring(nameEnd+1).trim());
				model = new MessageModel(senderName, recipentName, text, engFormatTime);
			}
		}

		return model;
	}
}
