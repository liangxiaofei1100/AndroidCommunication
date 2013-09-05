package com.dreamlink.communication.ui.history;

import com.dreamlink.communication.ui.DreamUtil;

public class HistoryInfo {
	/**
	 * true,is send message </br>
	 * false, is receive message
	 * */
	private boolean isSendMsg;
	
	/**send or receive time*/
	private long date;
	
	public HistoryInfo(){
	}
	
	public HistoryInfo(boolean isSendMsg, long date){
		this.isSendMsg = isSendMsg;
		this.date = date;
	}
	
	public boolean getMsgType(){
		return isSendMsg;
	}
	
	public void setMsgType(boolean type){
		isSendMsg = type;
	}
	
	public String getFormatDate(){
		return DreamUtil.getFormatDate(date);
	}
	
	public void setDate(long date){
		this.date = date;
	}
}
