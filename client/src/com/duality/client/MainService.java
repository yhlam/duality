package com.duality.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;

import com.duality.client.model.ChatDataSQL;
import com.duality.client.model.XMPPManager;

public class MainService extends Service {

	SQLiteDatabase mDb;
	ChatDataSQL mHelper;
	public String dbName = "ChatDatabase";
	public String recipentTable = "Recipents";
	public String messagesTable = "Messages";

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mHelper = new ChatDataSQL(this, dbName);
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
						String senderName = StringUtils.parseBareAddress(message.getFrom());
						String text = message.getBody();
						ContentValues c = new ContentValues();
						c.put("sender", senderName);
						c.put("recipent", XMPPManager.singleton().getUsername());
						c.put("message", text);
						// Put into database
						mDb = mHelper.getWritableDatabase();
						long isInserted = mDb.insert(messagesTable, "", c);
						if(isInserted == -1){

						}else{

						}

					}
				}
			}, filter);
		}
		return 0;

	}
}
