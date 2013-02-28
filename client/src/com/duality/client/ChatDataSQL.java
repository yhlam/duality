package com.duality.client;

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
				"create table HIH("
						+ "_ID INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
						+ "date VARCHAR,"
						+ "note VARCHAR,"
						+ "pw VARCHAR,"
						+ "reminder INT,"
						+ "type VARCHAR,"
						+ "memo VARCHAR"
						+ ")";

		db.execSQL(DATABASE_CREATE_TABLE);
	}
	public void createTable(SQLiteDatabase db, String tableName){
		String DATABASE_CREATE_TABLE =
				"create table " +  tableName + "("
						+ "_ID INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
						+ "date VARCHAR,"
						+ "note VARCHAR,"
						+ "pw VARCHAR,"
						+ "reminder INT,"
						+ "type VARCHAR,"
						+ "memo VARCHAR"
						+ ")";

		db.execSQL(DATABASE_CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		 
	}
	@Override
	public void onOpen(SQLiteDatabase db) {     
		super.onOpen(db);       
		// TODO 每次成功打開數據庫後首先被執行     
	} 

	@Override
	public synchronized void close() {
		super.close();
	}

}
