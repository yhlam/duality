package com.duality.client.decap;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.duality.client.R;

public class DecapProfile extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_profile);
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main_layout, menu);
		return true;
	}

}
