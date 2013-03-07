package com.duality.client;


import com.duality.client.decap.DecapGTalk;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ProfileFragment extends Fragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_profile_layout, container, false);
		Button gtalk  = (Button) v.findViewById(R.id.buttonChangeDisplayName);
		gtalk.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(getActivity(), DecapGTalk.class);
				startActivity(intent);
			}

		});
		return v;
	}

	public void onStop(){
		super.onStop();
	}
}

