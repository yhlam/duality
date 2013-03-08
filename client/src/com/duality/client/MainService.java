package com.duality.client;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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
						// Put into database
						mDb = mHelper.getWritableDatabase();
						long isInserted = mDb.insert(messagesTable, "", c);
						if(isInserted == -1){

						}else{
							showNotification(senderUsername, text);
							String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
							Intent i = new Intent(MESSAGE_RECEIVED);
							Bundle bundle = new Bundle();
							bundle.putString("text", text);
							i.putExtras(bundle);
							sendBroadcast(i);
							
						}

					}
				}
			}, filter);
		}
		return 0;

	}

	private void showNotification(String senderUsername, String text){

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		Notification notification = new Notification.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle(senderUsername)
		.setContentText(text)
		//		.setContentInfo("信息"), should be time
		.setTicker(senderUsername + ": " + text)
		//		.setLights(0xFFFFFFFF, 1000, 1000)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.getNotification();
		mNtfMgr.notify(NOTIFICATION_ID, notification);

	}

	public void onDestory(){
		super.onDestroy();
		mDb.close();
	}
}
