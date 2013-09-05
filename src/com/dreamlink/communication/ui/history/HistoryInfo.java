package com.dreamlink.communication.ui.history;

import android.os.Parcel;
import android.os.Parcelable;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.file.FileInfo;

public class HistoryInfo implements Parcelable{
	/**
	 * true,is send message </br>
	 * false, is receive message
	 * */
	private boolean isSendMsg;
	
	/**send or receive time*/
	private long date;
	
	/**send user*/
	private User sendUser;
	
	/**send or receive file info*/
	private FileInfo fileInfo;
	
	/***/
	private double prev = 0;
	
	/**progress*/
	private double progress = 0;
	private double max = 0;
	
	public HistoryInfo(){
	}
	
	public HistoryInfo(boolean isSendMsg, long date, User user, FileInfo fileInfo){
		this.isSendMsg = isSendMsg;
		this.date = date;
		this.sendUser = user;
		this.fileInfo = fileInfo;
	}
	
	public boolean getMsgType(){
		return isSendMsg;
	}
	
	public void setMsgType(boolean type){
		isSendMsg = type;
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
	
	public User getUser(){
		return sendUser;
	}
	
	public void setUser(User user){
		this.sendUser = user;
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
