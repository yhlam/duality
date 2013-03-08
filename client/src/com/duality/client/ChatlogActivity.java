package com.duality.client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.jivesoftware.smack.packet.Message;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.duality.client.model.ChatDataSQL;
import com.duality.client.model.XMPPManager;

public class ChatlogActivity extends Activity {

	private String recipentName;
	private String recipentUsername;
	private String dbName = "ChatDatabase";
	//	private String recipentTable = "Recipents";
	private String messagesTable = "Messages";

	private ArrayList<String> mMessages = new ArrayList<String>();
	private ArrayAdapter<String> adapter;
	private ListView mMessageList;
	SQLiteDatabase mDb;
	ChatDataSQL mHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_chatlog);

		Intent intent = getIntent();
		recipentName = (String) intent.getExtras().getString("name");

		TextView contactName = (TextView) findViewById(R.id.chatlog_recipentName);
		Button sendMessage = (Button) this.findViewById(R.id.chatlog_send);
		final EditText message = (EditText) this.findViewById(R.id.chatlog_typeMessage);
		mMessageList = (ListView) this.findViewById(R.id.chatlog_chatlog);

		contactName.setText(recipentName);
		setListAdapter();

		sendMessage.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String to = recipentUsername;
				String text = message.getText().toString();
				Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				try{
					XMPPManager.singleton().getXMPPConnection().sendPacket(msg);
					//Save to database
					mDb = mHelper.getWritableDatabase();
					ContentValues c = new ContentValues();
					c.put("recipent	", recipentUsername);
					c.put("message", text);
					c.put("sender", XMPPManager.singleton().getUsername());
					SimpleDateFormat dateTimeFormatter =  new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss");
					c.put("sendtime", dateTimeFormatter.format(new Date()).toString());
					long isInserted = mDb.insert(messagesTable, "", c);
					if(isInserted == -1){

					}else{
						mMessages.add(XMPPManager.singleton().getXMPPConnection().getUser() + ":" );
						mMessages.add(text);
						adapter.notifyDataSetChanged();
					}
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		});
		//Establish Database to read the real content
		mHelper = new ChatDataSQL(this, dbName);
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select username from Recipents WHERE name=?", new String[]{String.valueOf(recipentName)});
		int resultCount = cursor.getCount();
		if(resultCount!=0){
			cursor.moveToFirst();
			recipentUsername = cursor.getString(0);
		}
		cursor.close();
		
		String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
		IntentFilter filter = new IntentFilter(MESSAGE_RECEIVED);
		ChatlogReceiver receiver = new ChatlogReceiver();
		registerReceiver(receiver, filter);
	}	

	public void onStart(){
		super.onStart();
		mMessages = showContactHistory();
		setListAdapter();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main_layout, menu);
		return true;
	}

	public void onStop(){
		super.onStop();
		mDb.close();
	}

	private void setListAdapter() {
		adapter = new ArrayAdapter<String>(this, R.layout.multi_line_list_item, R.id.contact_history_text, mMessages); 
		mMessageList.setAdapter(adapter);
	}

	private ArrayList<String> showContactHistory(){
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select message from Messages WHERE ((recipent=? AND sender=?) OR (recipent=? AND sender=?)) ORDER BY _ID, datetime(sendtime)", new String[]{String.valueOf(recipentUsername), String.valueOf(XMPPManager.singleton().getUsername()), String.valueOf(XMPPManager.singleton().getUsername()),String.valueOf(recipentUsername)});
		ArrayList<String> temp = new ArrayList<String>();
		int row_count = cursor.getCount();
		if(row_count != 0){
			cursor.moveToFirst();
			for(int i = 0; i<row_count;i++){
				String string = cursor.getString(0);
				temp.add(string);
				cursor.moveToNext();
			}
		}
		cursor.close();
		return temp;
	}
	private class ChatlogReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Bundle bundle = intent.getExtras();
			mMessages.add(bundle.getString("text"));
			adapter.notifyDataSetChanged();
		}
		
	}

}
