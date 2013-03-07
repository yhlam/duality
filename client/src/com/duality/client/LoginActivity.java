package com.duality.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import com.duality.client.model.XMPPManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private Handler mThreadHandler;
	private HandlerThread mThread;
	private XMPPConnection mXmpp;
	private ConnectionConfiguration mConnect;
	private EditText mUsername;
	private EditText mPwd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_login);
		// Initialized Views
		mUsername = (EditText) this.findViewById(R.id.login_username);
		mPwd = (EditText) this.findViewById(R.id.login_password);

		Button signIn = (Button) this.findViewById(R.id.login_signin);
		signIn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mThread = new HandlerThread("serverConnection");
				mThread.start();
				mThreadHandler = new Handler(mThread.getLooper());
				mThreadHandler.post(connection);
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

	private Runnable connection = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mConnect = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
			mXmpp = new XMPPConnection(mConnect);
			try{
				mXmpp.connect();
				mXmpp.login(mUsername.getText().toString(), mPwd.getText().toString());
				mXmpp.sendPacket(new Presence(Presence.Type.available));
				XMPPManager.singleton().setXMPPConnection(mXmpp);
				XMPPManager.singleton().setUsername(mUsername.getText().toString());
				startActivity(new Intent(LoginActivity.this, MainActivity.class));
			}catch(XMPPException e){
				Toast.makeText(getApplicationContext(),
						"Connection Failed", Toast.LENGTH_LONG)
						.show();
				e.printStackTrace();
			}
		}

	};

}
