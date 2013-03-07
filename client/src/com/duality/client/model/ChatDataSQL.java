package com.duality.client.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class ChatDataSQL extends SQLiteOpenHelper {

	private static final int VERSION = 1;

	public ChatDataSQL(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	public ChatDataSQL(Context context,String name) { 
		this(context, name, null, VERSION); 
	} 

	public ChatDataSQL(Context context, String name, int version) {  
		this(context, name, null, version);  
	} 

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		String DATABASE_CREATE_TABLE =
				"create table Messages("
						+ "_ID INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
						+ "sender VARCHAR,"
						+ "recipent VARCHAR,"
						+ "message VARCHAR"
						+ ")";

		db.execSQL(DATABASE_CREATE_TABLE);
		
		String DATABASE_CREATE_TABLE2 =
				"create table Recipents("
						+ "_ID INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
						+ "name VARCHAR,"
						+ "username VARCHAR"
						+ ")";

		db.execSQL(DATABASE_CREATE_TABLE2);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		 db.execSQL("DROP TABLE IF EXISTS Messages");
		 db.execSQL("DROP TABLE IF EXISTS Recipents");
		 onCreate(db);
	}
	@Override
	public void onOpen(SQLiteDatabase db) {     
		super.onOpen(db);       
	} 

	@Override
	public synchronized void close() {
		super.close();
	}

}
