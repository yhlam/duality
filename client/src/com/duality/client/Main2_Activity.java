package com.duality.client;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.widget.TabHost;

public class Main2_Activity extends FragmentActivity {

	private TabHost mTabHost;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();
		mTabHost.addTab(mTabHost.newTabSpec("Profile").setIndicator("Profile").setContent(R.id.tab1));
		mTabHost.addTab(mTabHost.newTabSpec("Contact").setIndicator("Contact").setContent(R.id.tab2)); 
		mTabHost.addTab(mTabHost.newTabSpec("About").setIndicator("About").setContent(R.id.tab3));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
