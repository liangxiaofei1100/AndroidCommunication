package com.dreamlink.communication.ui.file;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *description a file infos 
 */
public class FileInfo implements Parcelable,Comparable{
	
	public boolean isDir = false;
	//File name
	public String fileName = "";
	//File Size,(bytes)
	public double fileSize = 0;
	//File Last modifed date
	public long fileDate;
	//absoulte path
	public String filePath;
	//icon
	public Drawable icon;
	//default 0:neither image nor apk; 1-->image; 2-->apk;
	public int type;
	
	public Object obj;
	//media file's play total time
	public long time;
	
	public FileInfo(String filename){
		this.fileName = filename;
	}
	
	private FileInfo(Parcel in){
		readFromParcel(in);
	}
	
	/**
	 * bytes convert
	 */
	public String getFormatFileSize(){
		if (fileSize > 1024 * 1024) {
			Double dsize = (double) (fileSize / (1024 * 1024));
			return new DecimalFormat("#.00").format(dsize) + "MB";
		}else if (fileSize > 1024) {
			Double dsize = (double)fileSize / (1024);
			return new DecimalFormat("#.00").format(dsize) + "KB";
		}else {
			return String.valueOf((int)fileSize) + " Bytes";
		}
	}
	
	/**
	 * date format
	 */
	public String getFormateDate(){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = format.format(new Date(fileDate));
		
		return dateString;
	}
	
	public static final Parcelable.Creator<FileInfo> CREATOR  = new Parcelable.Creator<FileInfo>() {

		@Override
		public FileInfo createFromParcel(Parcel source) {
			return new FileInfo(source);
		}

		@Override
		public FileInfo[] newArray(int size) {
			return new FileInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(isDir ? 1 : 0);
		dest.writeString(fileName);
		dest.writeDouble(fileSize);
		dest.writeLong(fileDate);
	}
	
	public void readFromParcel(Parcel in){
		isDir = in.readInt() == 1 ? true : false;
		fileName = in.readString();
		fileSize = in.readDouble();
		fileDate = in.readLong();
	}

	//make it can sort
	@Override
	public int compareTo(Object another) {
		FileInfo tmp = (FileInfo) another;
		if (fileName.compareToIgnoreCase(tmp.fileName) < 0) {
			return -1 ;
		}else if (fileName.compareToIgnoreCase(tmp.fileName) > 0) {
			return 1;
		}
		return 0;
	}
}
