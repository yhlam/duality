package com.duality.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.duality.client.model.GPSPresence;
import com.duality.client.model.XMPPManager;

public class LoginActivity extends Activity {
	
	private String SERVERIP = "143.89.168.103";
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
		Button signIn = (Button) this.findViewById(R.id.login_signin);
		Button serverSet = (Button) this.findViewById(R.id.login_server);
		final EditText domain = (EditText) this.findViewById(R.id.login_domain);
		final EditText ip = (EditText) this.findViewById(R.id.login_IP);

		signIn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mUserForm.setVisibility(View.GONE);
				mStatus.setVisibility(View.VISIBLE);
				new XMPPConnectionSetting().execute();
			}
		});
		
		serverSet.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				SERVERIP = ip.getText().toString();
				DOMAIN = domain.getText().toString();
			}
			
		});
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
			// TODO Auto-generated method stub
			mConnect = new ConnectionConfiguration(SERVERIP, 5222, DOMAIN);
			mXmpp = new XMPPConnection(mConnect);
			try{
				mXmpp.connect();
				mXmpp.login(mUsername.getText().toString(), mPwd.getText().toString());
				mXmpp.sendPacket(new GPSPresence(Presence.Type.available));
				XMPPManager.singleton().setXMPPConnection(mXmpp);
				XMPPManager.singleton().setUsername(mUsername.getText().toString() + "@" + DOMAIN);
				XMPPManager.singleton().setServerIP(SERVERIP);
				XMPPManager.singleton().setDomain(DOMAIN);
				return 1;
			}catch(XMPPException e){
				return -1;

			}
		}

		@Override
		protected void onPostExecute(Integer result){
			switch(result){
			case 1:
				startActivity(new Intent(LoginActivity.this, MainActivity.class));
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


