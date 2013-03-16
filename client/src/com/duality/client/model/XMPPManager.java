package com.duality.client.model;

import org.jivesoftware.smack.XMPPConnection;

public class XMPPManager {

	private XMPPConnection mXmpp;
	private String mUsername;
	private String mDomain;
	private String mServerIP;
	private static final XMPPManager INSTANCE = new XMPPManager();
	
	private XMPPManager(){
		
	}
	
	public static XMPPManager singleton(){
		return INSTANCE;
	}
	
	public void setXMPPConnection(XMPPConnection xmpp){
		mXmpp = xmpp;
	}

	public void setUsername(String username){
		mUsername = username;
	}
	
	public void setDomain(String domain){
		mDomain = domain;
	}
	
	public void setServerIP(String ip){
		mServerIP = ip;
	}
	
	public XMPPConnection getXMPPConnection(){
		return mXmpp;
	}
	
	public String getUsername(){
		return mUsername;
	}
	
	public String getDomain(){
		return mDomain;
	}
	
	public String getServerIP(){
		return mServerIP;
	}
}
