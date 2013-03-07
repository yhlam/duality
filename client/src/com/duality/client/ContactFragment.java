package com.duality.client;

import com.duality.client.model.ChatDataSQL;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContactFragment extends Fragment {
	SQLiteDatabase mDb;
	ChatDataSQL mHelper;
	
	public String db_name = "ChatDatabase";
	public String recipents_table_name = "Recipents";
	public String messages_table_name = "Messages";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHelper = new ChatDataSQL(getActivity(), db_name);
	}

	private static class ContactItemAdapter extends BaseAdapter{
		private final Context mContext;
		private final String[] mString;
		private final LayoutInflater mInflater;

		public ContactItemAdapter(Context context, String[] string){
			mContext = context;
			mString = string;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			final int size = mString.length;
			return size;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mString[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ContactTag contactTag = null;
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.contact_item_view, null);
				TextView text = (TextView) convertView.findViewById(R.id.contact_item_view_nameText);
				contactTag = new ContactTag(text);
				convertView.setTag(contactTag);
			}else{
				contactTag = (ContactTag) convertView.getTag();
			}
			final String name = mString[position];
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

	public String[] getContact(){
		mDb = mHelper.getReadableDatabase();
		Cursor cursor = mDb.rawQuery("select name from Recipents ORDER BY _ID DESC", null);
		String[] result = new String[cursor.getCount()];
		int rows_num = cursor.getCount();
		if(rows_num != 0) {
			cursor.moveToFirst();
			for(int i=0; i<rows_num; i++) {
				String strCr = cursor.getString(0);
				result[i]=strCr;
				cursor.moveToNext();
			}
		}
		cursor.close();
		return result;
	}


	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.act_contact, container, false);
		Button addButton = (Button) v.findViewById(R.id.contact_add);
		final String[] contactList = getContact();
		final ListView list = (ListView) v.findViewById(R.id.contact_contactList);
		final ContactItemAdapter adapter = new ContactItemAdapter(getActivity(), contactList);
		list.setAdapter(adapter);
		addButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View w) {
				mDb = mHelper.getWritableDatabase();
				EditText recipentName = (EditText) v.findViewById(R.id.contact_inputName);
				EditText recipentUserName = (EditText) v.findViewById(R.id.contact_inputUsername);
				Toast.makeText(getActivity(), recipentName.getText().toString(), Toast.LENGTH_SHORT)  
				.show(); 
				ContentValues c = new ContentValues();
				c.put("name", recipentName.getText().toString());
				c.put("username", recipentUserName.getText().toString());
				long isInserted = mDb.insert(recipents_table_name, "", c);
				if(isInserted == -1){
					Toast.makeText(getActivity(), "Insertion failed", Toast.LENGTH_SHORT)  
					.show(); 
				}else{
					//adapter.notifyDataSetChanged();
				}
			}
		});

		return v;
	}

	public void onStop(){
		super.onStop();
	}

	public void onDestory(){
		super.onDestroy();
		mDb.close();
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


