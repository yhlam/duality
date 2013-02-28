package com.duality.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

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

public class Login_Activity extends Activity {
	private Handler mThreadHandler;
	private HandlerThread mThread;
	private XMPPConnection xmpp;
	private ConnectionConfiguration connect;
	private EditText username;
	private EditText pwd;
	private Button signIn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		username = (EditText) this.findViewById(R.id.username_text);
		pwd = (EditText) this.findViewById(R.id.password_text);
		signIn = (Button) this.findViewById(R.id.sign_in_button);
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

	private Runnable connection = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			connect = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
			xmpp = new XMPPConnection(connect);
			try{
				xmpp.connect();
				xmpp.login(username.getText().toString(), pwd.getText().toString());
				Presence presence = new Presence(Presence.Type.available);
				xmpp.sendPacket(presence);
				startActivity(new Intent(Login_Activity.this, Main2_Activity.class));
			}catch(XMPPException e){
				Toast.makeText(getApplicationContext(),
						"Connection Failed", Toast.LENGTH_LONG)
						.show();
				e.printStackTrace();
			}
		}

	};


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

}
