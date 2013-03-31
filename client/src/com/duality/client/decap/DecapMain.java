package com.duality.client.decap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.duality.client.R;

public class DecapMain extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_layout);
		Button about = (Button)findViewById(R.id.about_us);
		about.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				startActivity(new Intent(DecapMain.this, DecapAbout.class));
			} 

		});

		Button profile = (Button)findViewById(R.id.my_profile);
		profile.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				startActivity(new Intent(DecapMain.this, DecapProfile.class));
			}

		});
		ListView testView = (ListView)findViewById(R.id.contact_list);
		String[] values = new String[]{"A","B","C","D"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,android.R.id.text1,values);
		testView.setAdapter(adapter);

		testView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Toast.makeText(getApplicationContext(),
						"Click ListItem Number " + arg2, Toast.LENGTH_LONG)
						.show();
			}

		});


		Button test = (Button)findViewById(R.id.beta);
		test.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				startActivity(new Intent(DecapMain.this, DecapGTalk.class));
			}

		});
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_layout, menu);
		return true;
	}


}
