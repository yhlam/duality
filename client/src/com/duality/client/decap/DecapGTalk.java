package com.duality.client.decap;

import java.util.ArrayList;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import com.duality.client.R;
import com.duality.client.R.id;
import com.duality.client.R.layout;
import com.duality.client.R.menu;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class DecapGTalk extends Activity {
	private Handler mThreadHandler;
	private HandlerThread mThread;
	private XMPPConnection xmpp;
	private ConnectionConfiguration connect;

	private EditText receiver;
	private EditText message;
	private ListView sentMessage;
	private ArrayList<String> messages = new ArrayList();
	private Handler mHandler = new Handler();
	private EditText username;
	private EditText password;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gtalk_layout);

		receiver = (EditText) this.findViewById(R.id.receiver);
		message = (EditText) this.findViewById(R.id.message);
		sentMessage = (ListView)this.findViewById(R.id.sentText);

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

		Button send = (Button) this.findViewById(R.id.send);
		send.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String to = receiver.getText().toString();
				String text = message.getText().toString();
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				try{
					xmpp.sendPacket(msg);
					messages.add(xmpp.getUser() + ":" );
					messages.add(text);
					setListAdapter();
				}catch (Exception e){
					e.printStackTrace();
				}
			}

		});
	}

	private void setListAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.multi_line_list_item, messages); 
		sentMessage.setAdapter(adapter);
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
			username = (EditText)findViewById(R.id.username);
			password = (EditText)findViewById(R.id.password);
			try{
				xmpp.connect();
				xmpp.login(username.getText().toString(), password.getText().toString());
				Presence presence = new Presence(Presence.Type.available);
				xmpp.sendPacket(presence);
				Toast.makeText(getApplicationContext(),
						"Connection Completed", Toast.LENGTH_LONG)
						.show();
				if (xmpp != null) {
				    // Add a packet listener to get messages sent to us
				    PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
				    xmpp.addPacketListener(new PacketListener() {
				        public void processPacket(Packet packet) {
				            Message message = (Message) packet;
				            if (message.getBody() != null) {
				                String fromName = StringUtils.parseBareAddress(message.getFrom());
				                messages.add(fromName + ":");
				                messages.add(message.getBody());
				                // Add the incoming message to the list view
				                mHandler.post(new Runnable() {
				                    public void run() {
				                        setListAdapter();
				                    }
				                });
				            }
				        }
				    }, filter);
				}
			}catch(XMPPException e){
				e.printStackTrace();
			}
		}

	};
}


