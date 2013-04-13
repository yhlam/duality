package com.duality.client.model;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;

import com.duality.api.PredictionMessageInfo;

public class PredictionResultModel extends IQ{

	private List<String> mPredictionList;

	public PredictionResultModel(){
		mPredictionList = new ArrayList<String>();
	}

	public void push(String prediction){
		mPredictionList.add(prediction);
	}
	
	public List<String> getList(){
		return mPredictionList;
	}
	
	@Override
	public String getChildElementXML() {
		int size = mPredictionList.size();
		String xml = "<" + PredictionMessageInfo.ELEMENT_NAME + " xmlns='" + PredictionMessageInfo.NAMESPACE + "'>";
		for(int i = 0; i<size; i++){
			xml += "<" + PredictionMessageInfo.PREDICTION + ">" + mPredictionList.get(i) + "</" + PredictionMessageInfo.PREDICTION + "> ";
		}
		xml += "</" + PredictionMessageInfo.ELEMENT_NAME + ">";
		return xml;
	}
}
