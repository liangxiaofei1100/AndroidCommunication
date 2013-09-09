package com.dreamlink.communication.ui.history;

import android.os.Parcel;
import android.os.Parcelable;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.file.FileInfo;

public class HistoryInfo implements Parcelable{
	/**
	 * 0, send message </br>
	 * 1, receive message
	 * */
	private int msgType;
	
	/**send or receive time*/
	private long date;
	
	/**receive message user*/
	private User receiveUser;
	
	/**send user name*/
	private String sendUserName;
	
	/**send or receive file info*/
	private FileInfo fileInfo;
	
	/***/
	private double prev = 0;
	
	/**progress*/
	private double progress = 0;
	private double max = 0;
	
	public HistoryInfo(){
	}
	
	public HistoryInfo(int  msgType, long date, User user, FileInfo fileInfo){
		this.msgType = msgType;
		this.date = date;
		this.receiveUser = user;
		this.fileInfo = fileInfo;
	}
	
	public int getMsgType(){
		return msgType;
	}
	
	public void setMsgType(int type){
		msgType = type;
	}
	
	public long getDate(){
		return date;
	}
	
	public String getFormatDate(){
		return DreamUtil.getFormatDate(date);
	}
	
	public void setDate(long date){
		this.date = date;
	}
	
	public User getReceiveUser(){
		return receiveUser;
	}
	
	public void setReceiveUser(User user){
		this.receiveUser = user;
	}
	
	public String getSendUserName(){
		return sendUserName;
	}
	
	public void setSendUserName(String name){
		this.sendUserName = name;
	}
	
	public FileInfo getFileInfo(){
		return fileInfo;
	}
	
	public void setFileInfo(FileInfo fileInfo){
		this.fileInfo = fileInfo;
	}
	
	public double getMax(){
		return fileInfo.fileSize;
	}
	
	public double getProgress(){
		return progress;
	}
	
	public void setProgress(double progress){
		this.progress = progress;
	}
	
	public double getPrev(){
		return prev;
	}

	public void setPrev(double prev){
		this.prev = prev;
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
	}
}
