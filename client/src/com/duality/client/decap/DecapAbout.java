package com.duality.client.decap;

import com.duality.client.R;
import com.duality.client.R.layout;
import com.duality.client.R.menu;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class DecapAbout extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_about);

	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_layout, menu);
		return true;
	}

}
