package com.duality.client;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

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
import com.duality.client.model.PredictionResultModel;
import com.duality.client.model.XMPPManager;

public class ChatlogActivity extends Activity {
	private static final String TAG = "ChatlogActivity";
	private final String DB_NAME = "ChatDatabase";
	private final String MESSAGE_TABLE = "Messages";

	private String mRecipentName;
	private String mRecipentUsername;
	private List<String> mMessages = new ArrayList<String>();
	private ArrayAdapter<String> mAdapter;
	private ListView mMessageList;
	private ChatlogReceiver mReceiver;
	private Spinner mPrediction;
	private List<String> mPredictionList;
	private ArrayAdapter<String> mPredictionAdapter;
	private SQLiteDatabase mDb;
	private ChatDataSQL mHelper;
	private XMPPConnection mConn;
	private int check = 0;

	@Override
	public void onStart(){
		super.onStart();
		mMessages = showContactHistory();
		setListAdapter();
		final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
		final IntentFilter filter = new IntentFilter(MESSAGE_RECEIVED);
		mReceiver = new ChatlogReceiver();
		registerReceiver(mReceiver, filter);
	}

	@Override
	public void onPause(){
		super.onPause();
	}

	@Override
	public void onStop(){
		super.onStop();
		unregisterReceiver(mReceiver);
		mHelper.close();
		if(mDb != null && !mDb.isOpen())
			mDb.close();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_chatlog);

		Intent intent = getIntent();
		mRecipentName = (String) intent.getExtras().getString("name");
		mMessageList = (ListView) this.findViewById(R.id.chatlog_chatlog);
		mPrediction = (Spinner) this.findViewById(R.id.chatlog_prediction);
		final EditText message = (EditText) this.findViewById(R.id.chatlog_typeMessage);
		final TextView contactName = (TextView) findViewById(R.id.chatlog_recipentName);
		final Button sendMessage = (Button) this.findViewById(R.id.chatlog_send);
		mConn = XMPPManager.singleton().getXMPPConnection();

		contactName.setText(mRecipentName);
		mPredictionList = new ArrayList<String>();
		setListAdapter();
		setPredictionAdapter();


		ProviderManager.getInstance().addIQProvider(PredictionMessageInfo.ELEMENT_NAME, PredictionMessageInfo.NAMESPACE, new IQProvider(){
			@Override
			public IQ parseIQ(XmlPullParser arg0) throws Exception {
				int eventType = arg0.getEventType();
				boolean inQuery = false;
				boolean inPrediction = false;
				PredictionResultModel iq = new PredictionResultModel();
				while(true){
					if(eventType == XmlPullParser.START_TAG){
						String tagname = arg0.getName();
						if(tagname.equals(PredictionMessageInfo.ELEMENT_NAME)) {
							inQuery = true;
						}
						else if (tagname.equals(PredictionMessageInfo.PREDICTION)) {
							inPrediction = true;
						}
					} else if(eventType == XmlPullParser.TEXT){
						if(inQuery && inPrediction){
							String prediction = arg0.getText();
							iq.push(prediction);
						}
					} else if(eventType == XmlPullParser.END_TAG){
						String tagname = arg0.getName();
						if(tagname.equals(PredictionMessageInfo.ELEMENT_NAME)) {
							break;						}
						else if (tagname.equals(PredictionMessageInfo.PREDICTION)) {
							inPrediction = false;
						}
					}
					eventType = arg0.next();
				}
				return iq;
			}
		});

		message.addTextChangedListener(new TextWatcher(){
			PacketCollector collector;

			@Override
			public void afterTextChanged(Editable arg0) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String username = XMPPManager.singleton().getUsername();
				String domain = XMPPManager.singleton().getDomain();
				String text = s.toString();
				if(!text.equals("")){
					PredictionMessage iq = new PredictionMessage(username, domain, text, mRecipentName + "@" + domain);
					String packetID = iq.getPacketID();
					PacketFilter filter = new PacketIDFilter(packetID);
					collector = XMPPManager.singleton().getXMPPConnection().createPacketCollector(filter);
					XMPPManager.singleton().getXMPPConnection().sendPacket(iq);
					PredictionResultModel prediction = (PredictionResultModel) collector.nextResult(10000);
					if(prediction != null){
						mPredictionList = prediction.getList();
						setPredictionAdapter();
					}else{
						collector.cancel();
					}
				}
			}
		});

		sendMessage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				final String to = mRecipentUsername;
				final String text = message.getText().toString();
				final Message msg = new Message(to, Message.Type.chat);
				msg.setBody(text);
				try{
					mConn.sendPacket(msg);
					mDb = mHelper.getWritableDatabase();
					final ContentValues c = new ContentValues();
					final SimpleDateFormat dateTimeFormatter =  new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss");
					c.put("recipent	", mRecipentUsername);
					c.put("message", text);
					c.put("sender", XMPPManager.singleton().getUsername());
					c.put("sendtime", dateTimeFormatter.format(new Date()).toString());
					final long isInserted = mDb.insert(MESSAGE_TABLE, "", c);
					if(isInserted == -1){
						throw new Exception();
					}else{
						mMessages.add("You: " + text);
						mAdapter.notifyDataSetChanged();
						message.setText("");
						mPredictionList.clear();
						setPredictionAdapter();
					}
				}catch (Exception e1){
					Log.e(TAG, "Failed to send message", e1);
				}
			}
		});

		mPrediction.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View arg1,
					int pos, long id) {
				check++;
				if(check>1){
					message.setText(parent.getSelectedItem().toString());
					check = 0;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});

		//Establish Database to read the real content
		mHelper = new ChatDataSQL(this, DB_NAME);
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select username from Recipents WHERE name=?", new String[]{String.valueOf(mRecipentName)});
		int resultCount = cursor.getCount();
		if(resultCount != 0){
			cursor.moveToFirst();
			mRecipentUsername = cursor.getString(0);
		}
		cursor.close();
		// PacketCollector to replace PacketListener


		// Add PacketListener to IQ Packets
		//		PacketFilter pFilter = new IQTypeFilter(IQ.Type.RESULT);
		//		mConn.addPacketListener(new PacketListener(){
		//
		//			@Override
		//			public void processPacket(Packet arg0) {
		//				//				final PacketExtension extension = arg0.getExtension(PredictionMessageInfo.ELEMENT_NAME, PredictionMessageInfo.NAMESPACE);
		//				String queryXML = ((IQ)arg0).getChildElementXML();
		//				mPredictionList = getPrediction(queryXML);
		//				//				mPredictionList.add("A");
		//				predictionAdapter.notifyDataSetChanged();
		//			}
		//
		//			private ArrayList<String> getPrediction(String input){
		//				ArrayList<String> temp  = new ArrayList<String>();
		//				DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		//				DocumentBuilder b = null;
		//				try {
		//					b = f.newDocumentBuilder();
		//				} catch (ParserConfigurationException e) {
		//					Log.e(TAG, "Failed to create a document builder", e);
		//				}
		//				Document doc = null;
		//				try {
		//					doc = b.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
		//				} catch (UnsupportedEncodingException e) {
		//					Log.e(TAG, "Divice does not support UTF-8", e);
		//				} catch (SAXException e) {
		//					Log.e(TAG, "Failed to parse the prediction result packet", e);
		//				} catch (IOException e) {
		//					Log.e(TAG, "IOException during parsing of prediction result packet", e);
		//				}
		//				NodeList queries = doc.getElementsByTagName(PredictionMessageInfo.ELEMENT_NAME);
		//				for (int i = 0; i < queries.getLength(); i++) {
		//					Element query = (Element) queries.item(i);
		//					Node prediction = query.getElementsByTagName(PredictionMessageInfo.PREDICTION).item(0);
		//					temp.add(prediction.getTextContent());
		//				}
		//				//				return temp;
		//				ArrayList<String> test = new ArrayList<String>();
		//				test.add("a");
		//				test.add("b");
		//				return test;
		//			}
		//
		//		}, pFilter);

	}	

	private void setPredictionAdapter(){
		mPredictionAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mPredictionList);
		mPredictionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPrediction.setAdapter(mPredictionAdapter);
	}

	private void setListAdapter() {
		mAdapter = new ArrayAdapter<String>(this, R.layout.multi_line_list_item, R.id.contact_history_text, mMessages); 
		mMessageList.setAdapter(mAdapter);
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
			mDb.close();
		}else{
			temp = "You";
		}
		return temp;
	}

	private ArrayList<String> showContactHistory(){
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select sender, message from Messages WHERE ((recipent=? AND sender=?) OR (recipent=? AND sender=?)) ORDER BY _ID, datetime(sendtime)", new String[]{String.valueOf(mRecipentUsername), String.valueOf(XMPPManager.singleton().getUsername()), String.valueOf(XMPPManager.singleton().getUsername()),String.valueOf(mRecipentUsername)});
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
		mDb.close();
		return temp;
	}
	private class ChatlogReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			mMessages.add(getName(bundle.getString("sender")) + ": " + bundle.getString("text"));
			mAdapter.notifyDataSetChanged();
		}

	}
}
