package com.duality.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class GTalk_Activity extends Activity {
	private Handler mThreadHandler;
	private HandlerThread mThread;
	private XMPPConnection xmpp;
	private ConnectionConfiguration connect;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gtalk_layout);

		Button setting = (Button) findViewById(R.id.setting);
		setting.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mThread = new HandlerThread("name");
				mThread.start();
				mThreadHandler = new Handler(mThread.getLooper());
				mThreadHandler.post(connection);
			}

		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_layout, menu);
		return true;
	}

	//For Connection setup
	private Runnable connection = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			connect = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
			xmpp = new XMPPConnection(connect);
			try{
				xmpp.connect();
				xmpp.login("FreshGraduateCareer@gmail.com", "wishyouallgood");
				Presence presence = new Presence(Presence.Type.available);
				xmpp.sendPacket(presence);
				Toast.makeText(getApplicationContext(),
						"Connection Completed", Toast.LENGTH_LONG)
						.show();
			}catch(XMPPException e){
				Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connect.getHost());
				e.printStackTrace();
			}
		}

	};
}