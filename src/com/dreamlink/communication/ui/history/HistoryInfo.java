package com.dreamlink.communication.ui.history;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.DreamUtil;

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
	private FileTransferInfo fileInfo;
	
	/**current transfer file size(bytes)*/
	private double progress = 0;
	private double max = 0;
	
	/**this file's status:sending,receiving,send ok,send fail and so on*/
	private int status;
	
	/**icon*/
	private Drawable icon;
	/**file type,image,apk,video and so on*/
	private int fileType;
	
	/**for speed*/
	private long startTime;
	private long nowTime;
	
	private Uri uri;
	
	public HistoryInfo(){
	}
	
	public HistoryInfo(int  msgType, long date, User user, FileTransferInfo fileInfo){
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
	
	public FileTransferInfo getFileInfo(){
		return fileInfo;
	}
	
	public void setFileInfo(FileTransferInfo fileInfo){
		this.fileInfo = fileInfo;
	}
	
	public double getMax(){
		return fileInfo.getFileSize();
	}
	
	public double getProgress(){
		return progress;
	}
	
	public void setProgress(double progress){
		this.progress = progress;
	}
	
	public int getStatus(){
		return status;
	}
	
	public void setStatus(int status){
		this.status = status;
	}
	
	public Drawable getIcon(){
		return icon;
	}
	
	public void setIcon(Drawable icon){
		this.icon = icon;
	}
	
	public int getFileType(){
		return fileType;
	}
	
	public void setFileType(int type){
		this.fileType = type;
	}
	
	public void setStartTime(long time){
		this.startTime = time;
	}
	
	public void setNowTime(long time){
		this.nowTime = time;
	}
	
	public String getSpeed(){
		long duration = nowTime  - startTime;
		return DreamUtil.getFormatSize((progress / (duration / 1000)));
	}
	
	public Uri getUri(){
		return uri;
	}
	
	public void setUri(Uri uri){
		this.uri = uri;
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
