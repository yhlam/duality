package com.duality.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import com.duality.client.model.ChatDataSQL;
import com.duality.client.model.XMPPManager;

public class MainService extends Service {

	SQLiteDatabase mDb;
	ChatDataSQL mHelper;
	private String dbName = "ChatDatabase";
	private String messagesTable = "Messages";
	private static final int NOTIFICATION_ID = 1;
	private NotificationManager mNtfMgr;
	private Timer mTimer;
	private Location mLocation;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mHelper = new ChatDataSQL(this, dbName);
		mNtfMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mLocation = getLocation(this);
		
		mTimer = new Timer(true);
		// Sending out Presence Packet in a 10 seconds interval
		mTimer.scheduleAtFixedRate(new TimedUpdater(), 10000, 10000);
	}
	
	public Location getLocation(Context context){
		LocationManager locMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Location loc = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(loc == null){
			loc = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		return loc;
	}

	private class TimedUpdater extends TimerTask{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			IQ iq = new IQ(){
				@Override
				public String getChildElementXML() {
					// TODO Auto-generated method stub
					return "<query xmlns='duality'>" +
					"<longitude>" + mLocation.getLongitude() + "</longitude> " +
					"<altitude>" + mLocation.getAltitude() + "</altitude> " +
					"<latitude>" + mLocation.getLatitude() + "</latitude> " +
					"<time>" + mLocation.getTime() + "</time> " +
					"</query>" +
					"</iq>";
				}
			};
			iq.setType(IQ.Type.SET);
			iq.setTo(XMPPManager.singleton().getDomain());
			iq.setFrom(XMPPManager.singleton().getUsername());
			XMPPManager.singleton().getXMPPConnection().sendPacket(iq);
			
//			Updating GPS via Presence
//			final double longitude = mLocation.getLongitude();
//			final double altitude = mLocation.getAltitude();
//			final double latitude = mLocation.getLatitude();
//			final long time = mLocation.getTime();
//			GPSPresence packet = new GPSPresence(altitude, longitude, latitude, time, Presence.Type.available);
//			XMPPManager.singleton().getXMPPConnection().sendPacket(packet);
		}

	}


	public int onStartCommand(Intent intent, int flags, int startId){

		super.onStartCommand(intent, flags, startId);

		if (XMPPManager.singleton().getXMPPConnection() != null) {
			PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
			XMPPManager.singleton().getXMPPConnection().addPacketListener(new PacketListener() {

				@Override
				public void processPacket(Packet packet) {

					Message message = (Message) packet;
					if (message.getBody() != null) {
						String senderUsername = StringUtils.parseBareAddress(message.getFrom());
						String text = message.getBody();
						ContentValues c = new ContentValues();
						c.put("sender", senderUsername);
						c.put("recipent", XMPPManager.singleton().getUsername());
						c.put("message", text);
						SimpleDateFormat dateTimeFormatter =  new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
						c.put("sendtime", dateTimeFormatter.format(new Date()));

						mDb = mHelper.getWritableDatabase();
						long isInserted = mDb.insert(messagesTable, "", c);
						if(isInserted == -1){

						}else{
							showNotification(getName(senderUsername), text);
							String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
							Intent i = new Intent(MESSAGE_RECEIVED);
							Bundle bundle = new Bundle();
							bundle.putString("text", text);
							bundle.putString("sender", senderUsername);
							i.putExtras(bundle);
							sendBroadcast(i);

						}

					}
				}
			}, filter);
		}
		return 0;

	}

	// Repeated Function, need to build a class to handle this
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

	private void showNotification(String senderUsername, String text){

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		Notification notification = new Notification.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle(senderUsername)
		.setContentText(text)
		.setTicker(senderUsername + ": " + text)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.getNotification();
		mNtfMgr.notify(NOTIFICATION_ID, notification);

	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		mDb.close();
		XMPPManager.singleton().getXMPPConnection().disconnect();
	}
}
