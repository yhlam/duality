package com.chatlogimporter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ChatlogImporter {

	private ChatlogParser parser;
	private List<MessageModel> list;

	public ChatlogImporter(String dir){
		File file = new File(dir);
		parser = new ChatlogParser(file);
		list = parser.start();
	}


	public List<MessageModel> getList() {
		return list;
	}


	public void setList(List<MessageModel> list) {
		this.list = list;
	}


	public static void main (String[] args) throws ClassNotFoundException{

		ChatlogImporter importer = new ChatlogImporter("logs");

		Class.forName("org.sqlite.JDBC");
		String tempDb = "chatlog.db";
		String jdbc = "jdbc:sqlite";
		String dbUrl = jdbc + ":" + tempDb;
		int timeOut = 30;
		// SQL Statements
		String dropTable = "DROP TABLE if exists chatlog";
		String makeTable = "CREATE TABLE chatlog (id numeric, datetime text, senderName text, recipentName text, message text)";

		// Making Connection
		Connection connection = null;
		try{
			connection = DriverManager.getConnection(dbUrl);
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(timeOut);

			statement.executeUpdate(dropTable);
			statement.executeUpdate(makeTable);

			for (int i = 0; i < importer.getList().size(); i++){
				MessageModel model = importer.getList().get(i); 
				if(model!=null){
					String insert = "INSERT INTO chatlog VALUES(" + (i+1) + ", '" + model.getDateTime() + "', '"  + model.getSenderName() + "', '" + model.getRecipentName()  + "', '" + model.getMessage() + "')";
					statement.executeUpdate(insert);
				}
			}

			String query = "SELECT * FROM chatlog";
			ResultSet rs = statement.executeQuery(query);
			while(rs.next()){
				System.out.print("id = " + rs.getInt("id"));
				System.out.print(" dateTime = " + rs.getString("dateTime"));
				System.out.print(" senderName= " + rs.getString("senderName"));
				System.out.print(" recipentName= " + rs.getString("recipentName"));
				System.out.println(" message= " + rs.getString("message"));
			}
		} catch(SQLException e){
			e.printStackTrace();
		} finally {
			try {
				if(connection != null)
					connection.close();
			} catch(SQLException e){
				e.printStackTrace();
			}
		}
	}
}
