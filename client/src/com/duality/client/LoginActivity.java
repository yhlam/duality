package com.duality.client;

import java.io.File;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.duality.client.model.XMPPManager;

public class LoginActivity extends Activity {

	private String SERVERIP = "192.168.0.198";
	private String DOMAIN = "fyp";
	private XMPPConnection mXmpp;
	private ConnectionConfiguration mConnect;
	private EditText mUsername;
	private EditText mPwd;
	private LinearLayout mUserForm;
	private LinearLayout mStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_login);
		
		mUsername = (EditText) this.findViewById(R.id.login_username);
		mPwd = (EditText) this.findViewById(R.id.login_password);
		mUserForm = (LinearLayout) this.findViewById(R.id.login_form);
		mStatus = (LinearLayout) this.findViewById(R.id.login_status);
		
		final Button signIn = (Button) this.findViewById(R.id.login_signin);
		final Button serverSet = (Button) this.findViewById(R.id.login_server);
		final EditText domain = (EditText) this.findViewById(R.id.login_domain);
		final EditText ip = (EditText) this.findViewById(R.id.login_IP);

		signIn.setOnClickListener(new Button.OnClickListener(){
			
			@Override
			public void onClick(View v) {
				mUserForm.setVisibility(View.GONE);
				mStatus.setVisibility(View.VISIBLE);
				new XMPPConnectionSetting().execute();
			}
		});

		serverSet.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				SERVERIP = ip.getText().toString();
				DOMAIN = domain.getText().toString();
			}

		});
	}
	
	@Override
	public void onPause(){
		super.onPause();
	}

	@Override
	public void onStart(){
		super.onStart();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}
	
	@Override
	public void onStop(){
		super.onStop();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

	private class XMPPConnectionSetting extends AsyncTask<Void, Void, Integer>{

		@Override
		protected Integer doInBackground(Void... params) {
			mConnect = new ConnectionConfiguration(SERVERIP, 5222, DOMAIN);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mConnect.setTruststoreType("AndroidCAStore");
			    mConnect.setTruststorePassword(null);
			    mConnect.setTruststorePath(null);
			} else {
				mConnect.setTruststoreType("BKS");
			    String path = System.getProperty("javax.net.ssl.trustStore");
			    if (path == null) {
			        path = System.getProperty("java.home") + File.separator + "etc"
			            + File.separator + "security" + File.separator
			            + "cacerts.bks";
			    }
			    mConnect.setTruststorePath(path);
			}
			
			// Setting up XMPP Connection and updating the connection to the XMPPMaganer Singleton
			mXmpp = new XMPPConnection(mConnect);
			try{
				mXmpp.connect();
				mXmpp.login(mUsername.getText().toString(), mPwd.getText().toString());
				final XMPPManager xmppManager = XMPPManager.singleton();
				xmppManager.setXMPPConnection(mXmpp);
				final String username = mUsername.getText().toString() + "@" + DOMAIN;
				xmppManager.setUsername(username);
				xmppManager.setServerIP(SERVERIP);
				xmppManager.setDomain(DOMAIN);
				return 1;
			}catch(XMPPException e){
				Log.e(LoginActivity.class.getName(), "Login Connection Error", e);
				return -1;
			}
		}

		@Override
		protected void onPostExecute(Integer result){
			switch(result){
			case 1:
				startActivity(new Intent(LoginActivity.this, ContactActivity.class));
				break;
			case -1:
				Toast.makeText(getApplicationContext(),
						"Connection Failed", Toast.LENGTH_LONG)
						.show();

				break;
			default:
				Toast.makeText(getApplicationContext(),
						"Connection Failed", Toast.LENGTH_LONG)
						.show();
				break;
			}
			super.onPostExecute(result);
			mUserForm.setVisibility(View.VISIBLE);
			mStatus.setVisibility(View.GONE);
			mUsername.setText("");
			mPwd.setText("");
		}
	}
}


