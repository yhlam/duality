package com.duality.client;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Tab2Fragment extends Fragment {
	SQLiteDatabase db;
	private String db_name = "chatDB";
	private String table_name;
	//getActivity returns null
	ChatDataSQL helper = new ChatDataSQL(getActivity(), db_name);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		db = helper.getReadableDatabase();
	}


	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_contact_layout, container, false);
		return v;
	}
}

