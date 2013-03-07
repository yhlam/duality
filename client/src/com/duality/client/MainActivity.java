package com.duality.client;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_main);
		
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		
		Tab profileTab = actionBar.newTab().setText("Profile");
		ActionBarTabListener profileListener = new ActionBarTabListener(new ProfileFragment());
		profileTab.setTabListener(profileListener);
		
		Tab contactTab = actionBar.newTab().setText("Contact");
		ActionBarTabListener contactListener = new ActionBarTabListener(new ContactFragment());
		contactTab.setTabListener(contactListener);
		
		Tab aboutTab = actionBar.newTab().setText("About");
		ActionBarTabListener aboutListener = new ActionBarTabListener(new AboutFragment());
		aboutTab.setTabListener(aboutListener);
		
		actionBar.addTab(profileTab);
		actionBar.addTab(contactTab);
		actionBar.addTab(aboutTab);
		
		Intent intent = new Intent(MainActivity.this, MainService.class);
		startService(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	class ActionBarTabListener implements TabListener{
		private Fragment mFragment;
		
		public ActionBarTabListener(Fragment frag){
			mFragment = frag;
		}
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			ft.add(R.id.context, mFragment, null);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			ft.remove(mFragment);
		}
		
	}
}
