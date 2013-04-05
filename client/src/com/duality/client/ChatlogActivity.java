package com.duality.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.duality.api.PredictionMessageInfo;
import com.duality.client.model.ChatDataSQL;
import com.duality.client.model.PredictionMessage;
import com.duality.client.model.XMPPManager;

public class ChatlogActivity extends Activity {
	private static final String TAG = "ChatlogActivity";

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
	private List<String> mPredictionList;
	private ArrayAdapter<String> predictionAdapter;
	SQLiteDatabase mDb;
	ChatDataSQL mHelper;

	private int check = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPredictionList = new ArrayList<String>();
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
		
		predictionAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mPredictionList);
		predictionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPrediction.setAdapter(predictionAdapter);
		
		sendMessage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
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
						message.setText("");
					}
				}catch (Exception e){
					Log.e(TAG, "Failed to send message", e);
				}
			}
		});

		message.addTextChangedListener(new TextWatcher(){

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
				final XMPPManager xmppManager = XMPPManager.singleton();
				final String username = xmppManager.getUsername();
				final String domain = xmppManager.getDomain();
				final String text = s.toString();
				final PredictionMessage iq = new PredictionMessage(username, domain, text, recipentName + "@" + XMPPManager.singleton().getDomain());
				final XMPPConnection xmppConnection = xmppManager.getXMPPConnection();
				xmppConnection.sendPacket(iq);
			}

		});

		// Add PacketListener to IQ Packets
		PacketFilter pFilter = new IQTypeFilter(IQ.Type.RESULT);
		XMPPManager.singleton().getXMPPConnection().addPacketListener(new PacketListener(){

			@Override
			public void processPacket(Packet arg0) {
//				final PacketExtension extension = arg0.getExtension(PredictionMessageInfo.ELEMENT_NAME, PredictionMessageInfo.NAMESPACE);
				String queryXML = ((IQ)arg0).getChildElementXML();
				mPredictionList = getPrediction(queryXML);
//				mPredictionList.add("A");
				predictionAdapter.notifyDataSetChanged();
			}

			private ArrayList<String> getPrediction(String input){
				ArrayList<String> temp  = new ArrayList<String>();
				DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
				DocumentBuilder b = null;
				try {
					b = f.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					Log.e(TAG, "Failed to create a document builder", e);
				}
				Document doc = null;
				try {
					doc = b.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "Divice does not support UTF-8", e);
				} catch (SAXException e) {
					Log.e(TAG, "Failed to parse the prediction result packet", e);
				} catch (IOException e) {
					Log.e(TAG, "IOException during parsing of prediction result packet", e);
				}
				NodeList queries = doc.getElementsByTagName(PredictionMessageInfo.ELEMENT_NAME);
				for (int i = 0; i < queries.getLength(); i++) {
					Element query = (Element) queries.item(i);
					Node prediction = query.getElementsByTagName(PredictionMessageInfo.PREDICTION).item(0);
					temp.add(prediction.getTextContent());
				}
//				return temp;
				ArrayList<String> test = new ArrayList<String>();
				test.add("a");
				test.add("b");
				return test;
			}

		}, pFilter);
		
		mPrediction.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View arg1,
					int pos, long id) {
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
			Bundle bundle = intent.getExtras();
			mMessages.add(getName(bundle.getString("sender")) + ": " + bundle.getString("text"));
			adapter.notifyDataSetChanged();
		}

	}

}
