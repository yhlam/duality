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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
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
	private ChatlogReceiver receiver;
	private Spinner mPrediction;
	private String[] mPredictionList;
	SQLiteDatabase mDb;
	ChatDataSQL mHelper;

	private int check = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_chatlog);

		Intent intent = getIntent();
		recipentName = (String) intent.getExtras().getString("name");

		final EditText message = (EditText) this.findViewById(R.id.chatlog_typeMessage);
		TextView contactName = (TextView) findViewById(R.id.chatlog_recipentName);
		Button sendMessage = (Button) this.findViewById(R.id.chatlog_send);
		mMessageList = (ListView) this.findViewById(R.id.chatlog_chatlog);
		mPrediction = (Spinner) this.findViewById(R.id.chatlog_prediction);

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
						mMessages.add("You: " + text);
						adapter.notifyDataSetChanged();
					}
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		});

		/*		message.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				final String text = s.toString();
				IQ iq = new IQ(){
					@Override
					public String getChildElementXML() {
						// TODO Auto-generated method stub
						return "<query xmlns='duality'>" +
						"<text>" + text.toString() + "</text> " +
						"<recipent>" +  String.valueOf(recipentName) + "</recipent>" +
						"</query>" +
						"</iq>";
					}
				};
				iq.setType(IQ.Type.SET);
				iq.setTo(XMPPManager.singleton().getDomain());
				iq.setFrom(XMPPManager.singleton().getUsername());
				XMPPManager.singleton().getXMPPConnection().sendPacket(iq);
			}

		});

		// Add PacketListener to IQ Packets
		PacketFilter pFilter = new IQTypeFilter(IQ.Type.RESULT);
		XMPPManager.singleton().getXMPPConnection().addPacketListener(new PacketListener(){

			@Override
			public void processPacket(Packet arg0) {
				// TODO Auto-generated method stub
				String queryXML = arg0.toXML();
				mPredictionList = getPrediction(queryXML);
			}

			private String[] getPrediction(String input){
				ArrayList<String> temp  = new ArrayList<String>();
				DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
				DocumentBuilder b = null;
				try {
					b = f.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Document doc = null;
				try {
					doc = b.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				NodeList queries = doc.getElementsByTagName("query");
				for (int i = 0; i < queries.getLength(); i++) {
					Element query = (Element) queries.item(i);
					Node prediction = query.getElementsByTagName("prediction").item(0);
					temp.add(prediction.getTextContent());
				}
				return (String[])temp.toArray();
			}

		}, pFilter);
		 */

		mPredictionList = new String[]{"A","B","C"};
		ArrayAdapter<String> predictionAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mPredictionList);
		predictionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPrediction.setAdapter(predictionAdapter);
		mPrediction.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View arg1,
					int pos, long id) {
				// TODO Auto-generated method stub
				check++;
				if(check>1){
					message.setText(parent.getSelectedItem().toString());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

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
		receiver = new ChatlogReceiver();
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

	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(receiver);
		Intent intent = new Intent(ChatlogActivity.this, MainService.class);
		stopService(intent);
	}

	private void setListAdapter() {
		adapter = new ArrayAdapter<String>(this, R.layout.multi_line_list_item, R.id.contact_history_text, mMessages); 
		mMessageList.setAdapter(adapter);
		mMessageList.setSelection(mMessages.size());
	}

	private String getName(String username){
		String temp = "";
		if(!(username.equals(XMPPManager.singleton().getUsername()))){
			mDb = mHelper.getReadableDatabase();
			Cursor cursor = mDb.rawQuery("select name from Recipents WHERE username=?", new String[]{String.valueOf(username)});
			int row_count = cursor.getCount();
			if(row_count != 0){
				cursor.moveToFirst();
				for(int i = 0; i<row_count;i++){
					temp = cursor.getString(0);
					cursor.moveToNext();
				}
			}
			cursor.close();
		}else{
			temp = "You";
		}
		return temp;
	}

	private ArrayList<String> showContactHistory(){
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select sender, message from Messages WHERE ((recipent=? AND sender=?) OR (recipent=? AND sender=?)) ORDER BY _ID, datetime(sendtime)", new String[]{String.valueOf(recipentUsername), String.valueOf(XMPPManager.singleton().getUsername()), String.valueOf(XMPPManager.singleton().getUsername()),String.valueOf(recipentUsername)});
		ArrayList<String> temp = new ArrayList<String>();
		int row_count = cursor.getCount();
		if(row_count != 0){
			cursor.moveToFirst();
			for(int i = 0; i<row_count;i++){
				String sender = getName(cursor.getString(0));
				String string = sender + ": " + cursor.getString(1);
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
			mMessages.add(getName(bundle.getString("sender")) + ": " + bundle.getString("text"));
			adapter.notifyDataSetChanged();
		}

	}

}
