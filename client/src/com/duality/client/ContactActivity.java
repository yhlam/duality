package com.duality.client;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.packet.VCard;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.duality.client.model.ChatDataSQL;
import com.duality.client.model.XMPPManager;

public class ContactActivity extends Activity{
	
	private final String DB_NAME = "ChatDatabase";
	private final String RECIPENT_TABLE = "Recipents";
	
	private SQLiteDatabase mDb;
	private ChatDataSQL mHelper;
	private List<String> mContactList;
	private ContactItemAdapter mAdapter;
	private VCard mVCard;
	private XMPPConnection mXmpp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_contact);
		Intent intent = new Intent(ContactActivity.this, ContactService.class);
		startService(intent);
	}
	
	
	@Override
	public void onStart(){
		super.onStart();
		mXmpp = XMPPManager.singleton().getXMPPConnection();
		ProviderManager.getInstance().addIQProvider("vCard", "vcard-temp", new org.jivesoftware.smackx.provider.VCardProvider());
		mHelper = new ChatDataSQL(this, DB_NAME);
		mContactList = getContact();
		final ListView list = (ListView) this.findViewById(R.id.contact_contactList);
		final Button addButton = (Button) this.findViewById(R.id.contact_add);
		final Button profileButton = (Button) this.findViewById(R.id.contact_profile);
		mAdapter = new ContactItemAdapter(this, mContactList);
		list.setAdapter(mAdapter);

		// Display avatar
		mVCard = new VCard();
		try {
			String userDomain = XMPPManager.singleton().getUsername();
			mVCard.load(mXmpp, userDomain);
			if(mVCard.getAvatar() != null){
				byte[] picStream = mVCard.getAvatar();
				Bitmap bitmap = BitmapFactory.decodeByteArray(picStream, 0, picStream.length);
				ImageView img = (ImageView) this.findViewById(R.id.contact_image);
				img.setImageBitmap(bitmap);
			}
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
		addButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View w) {
				mDb = mHelper.getWritableDatabase();
				final EditText recipentName = (EditText) ContactActivity.this.findViewById(R.id.contact_inputName);
				final EditText recipentUserName = (EditText) ContactActivity.this.findViewById(R.id.contact_inputUsername);
				ContentValues c = new ContentValues();
				final String sender = XMPPManager.singleton().getUsername();
				final String name = recipentName.getText().toString();
				final String username = recipentUserName.getText().toString();
				c.put("sender", sender);
				c.put("name", name);
				c.put("username", username);
				long isInserted = mDb.insert(RECIPENT_TABLE, "", c);
				if(isInserted == -1){
					Toast.makeText(ContactActivity.this, "Insertion failed", Toast.LENGTH_SHORT)  
					.show(); 
				}else{
					Toast.makeText(ContactActivity.this, recipentName.getText().toString(), Toast.LENGTH_SHORT)  
					.show();
					mContactList.add(0, name);
					mAdapter.notifyDataSetChanged();
				}
			}
		});
		
		profileButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, 1);
			}
		});
		
	}
	
	//TODO: Change the code syntax
	public byte[] getBytesFromBitmap(Bitmap bitmap) {
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    bitmap.compress(CompressFormat.JPEG, 70, stream);
	    return stream.toByteArray();
	}
	
	//TODO: Change the code syntax
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
	    super.onActivityResult(requestCode, resultCode, data);
	    if (resultCode == RESULT_OK)
	    {
	        Uri chosenImageUri = data.getData();

	        Bitmap mBitmap = null;
	        try {
				mBitmap = Media.getBitmap(this.getContentResolver(), chosenImageUri);
				ByteArrayOutputStream stream=new ByteArrayOutputStream();
			    mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
			    byte[] result=stream.toByteArray();

				VCard vCard = new VCard();
				vCard.setAvatar(result);
				try {
					vCard.save(XMPPManager.singleton().getXMPPConnection());
				} catch (XMPPException e) {
					e.printStackTrace();
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	        }
	}
	
	@Override
	public void onStop(){
		super.onStop();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(mDb != null)
			if(mDb.isOpen())
				mDb.close();
		mHelper.close();
		Intent intent = new Intent(ContactActivity.this, ContactService.class);
		try {
			stopService(intent);
		} catch (Exception e){
			
		}
	}
	
	public List<String> getContact(){
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select name from Recipents WHERE sender=? ORDER BY _ID DESC", new String[]{String.valueOf(XMPPManager.singleton().getUsername())});
		List<String> result = new ArrayList<String>();
		final int rows_num = cursor.getCount();
		if(rows_num != 0) {
			cursor.moveToFirst();
			for(int i=0; i<rows_num; i++) {
				String strCr = cursor.getString(0);
				result.add(strCr);
				cursor.moveToNext();
			}
		}
		cursor.close();
		return result;
	}
	
	private static class ContactItemAdapter extends BaseAdapter{
		private final Context mContext;
		private final List<String> mString;
		private final LayoutInflater mInflater;

		public ContactItemAdapter(Context context, List<String> string){
			mContext = context;
			mString = string;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			final int size = mString.size();
			return size;
		}

		@Override
		public Object getItem(int position) {
			return mString.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ContactTag contactTag = null;
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.contact_item_view, null);
				TextView text = (TextView) convertView.findViewById(R.id.contact_item_view_nameText);
				contactTag = new ContactTag(text);
				convertView.setTag(contactTag);
			}else{
				contactTag = (ContactTag) convertView.getTag();
			}
			final String name = mString.get(position);
			final TextView textView = contactTag.getTextView();
			textView.setText(name);
			textView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final Intent intent = new Intent(mContext, ChatlogActivity.class);
					intent.putExtra("name", name);
					mContext.startActivity(intent);
				}
			});    
			
			return convertView;
		}

	}
	
	private static class ContactTag{
		TextView mName;
		public ContactTag(TextView textView){
			mName = textView;
		}
		public TextView getTextView(){
			return mName;
		}
	}
}
