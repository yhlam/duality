package com.chatlogimporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatlogParser {
	private File[] directory;
	private List<MessageModel> list;

	public ChatlogParser(File[] dir){
		directory = dir;
		list = new ArrayList<MessageModel>();
	}

	public List<MessageModel> start(){
		BufferedReader reader = null;
		for (int i = 0; i<directory.length; i++){
			File file = directory[i];
			try {
				String temp = "";
				reader = new BufferedReader(new FileReader(file));
				temp = reader.readLine();

				while (temp != null){
					if(!(temp.equals(""))){
						MessageModel model = parse(temp); 
						list.add(model);
					}
					temp = reader.readLine();
				}
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
		return list;
	}
	
	public String SQLParse(String s){
		String temp = s.replaceAll("'", "''");
		return temp;
	}
	

	public MessageModel parse(String s){
		MessageModel model = null;
		String re = "[0-9/: ]+[AP]M";
		Pattern pattern = Pattern.compile(re);
		Matcher matcher = pattern.matcher(s);
		boolean isFound = matcher.find();
		if(isFound){
			int end = matcher.end();
			String groupStr = matcher.group(0);	
			String[] datetimes = groupStr.split(" ");
			String date = datetimes[0];
			String time = datetimes[1];
			String ampm = datetimes[2];

			String nameText = s.substring(end).trim();
			int nameEnd = nameText.indexOf(": ");
			nameText = nameText.substring(nameEnd+1);
			nameEnd = nameText.indexOf(": ");
			String name = SQLParse(nameText.substring(0, nameEnd).trim());
			String text = SQLParse(nameText.substring(nameEnd+1).trim());

			model = new MessageModel(name, text, date, time, ampm);
		}
		return model;
	}
}
