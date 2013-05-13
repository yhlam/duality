package com.duality.client;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.VCard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.QuickContactBadge;
import android.widget.Toast;

import com.duality.client.model.XMPPManager;

public class ProfileActivity extends Activity {

	private QuickContactBadge avatar;
	private VCard profile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_profile);
		
		avatar = (QuickContactBadge) findViewById(R.id.profile_avatar);

		final XMPPConnection conn = XMPPManager.singleton().getXMPPConnection();
		profile = new VCard();
		try {
			profile.load(conn);
		} catch (XMPPException e) {
			Toast.makeText(this, "Failed to load profile infromation", Toast.LENGTH_SHORT);
			e.printStackTrace();
		}
		
		final byte[] avatarBytes = profile.getAvatar();
		if(avatarBytes != null) {
			final Bitmap bitmap = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
			avatar.setImageBitmap(bitmap);
		}
	}
}
