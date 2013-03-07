package com.duality.client;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import com.duality.client.model.XMPPManager;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		
		Tab profileTab = actionBar.newTab().setText("Profile");
		ActionBarTabListener profileListener = new ActionBarTabListener(new ProfileFragment());
		profileTab.setTabListener(profileListener);
		
		Tab contactTab = actionBar.newTab().setText("Contact");
		ActionBarTabListener contactListener = new ActionBarTabListener(new ContactFragment());
		contactTab.setTabListener(contactListener);
		
		Tab aboutTab = actionBar.newTab().setText("About");
		ActionBarTabListener aboutListener = new ActionBarTabListener(new AboutFragment());
		aboutTab.setTabListener(aboutListener);
		
		actionBar.addTab(profileTab);
		actionBar.addTab(contactTab);
		actionBar.addTab(aboutTab);
		
		XMPPConnection xmpp = XMPPManager.singleton().getXMPPConnection();
		if (xmpp != null) {
		    PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
		    xmpp.addPacketListener(new PacketListener() {
		        public void processPacket(Packet packet) {
		            Message message = (Message) packet;
		            if (message.getBody() != null) {
//		                String fromName = StringUtils.parseBareAddress(message.getFrom());
		                // Put into database
		                /*messages.add(fromName + ":");
		                messages.add(message.getBody());
		                // Add the incoming message to the list view
		                mHandler.post(new Runnable() {
		                    public void run() {
		                        setListAdapter();
		                    }
		                });
		                */
		            }
		        }
		    }, filter);
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	class ActionBarTabListener implements TabListener{
		private Fragment mFragment;
		
		public ActionBarTabListener(Fragment frag){
			mFragment = frag;
		}
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			ft.add(R.id.context, mFragment, null);
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			ft.remove(mFragment);
		}
		
	}
}
