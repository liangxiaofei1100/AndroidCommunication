package com.dreamlink.communication.protocol;

import java.io.File;
import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class FileTransferInfo implements Serializable, Parcelable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6587172190406939461L;
	public String mFilePath;
	public String mFileName;
	public long mFileSize;

	public FileTransferInfo(){
	}
	
	public FileTransferInfo(File file) {
		mFilePath = file.getAbsolutePath();
		mFileName = file.getName();
		mFileSize = file.length();
	}
	
	private FileTransferInfo(Parcel in){
		readFromParcel(in);
	}

	public String getFilePath(){
		return mFilePath;
	}
	
	public void setFilePath(String path){
		this.mFilePath = path;
	}
	
	/**
	 * @return the FileName
	 */
	public String getFileName() {
		return mFileName;
	}
	
	public void setFileName(String name){
		this.mFileName = name;
	}

	/**
	 * @return the FileSize
	 */
	public long getFileSize() {
		return mFileSize;
	}
	
	public void setFileSize(long size){
		this.mFileSize = size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FileInfo [mFileName=" + mFileName + ", mFileSize=" + mFileSize
				+ "]";
	}
	
	public static final Parcelable.Creator<FileTransferInfo> CREATOR  = new Parcelable.Creator<FileTransferInfo>() {

		@Override
		public FileTransferInfo createFromParcel(Parcel source) {
			return new FileTransferInfo(source);
		}

		@Override
		public FileTransferInfo[] newArray(int size) {
			return new FileTransferInfo[size];
		}
	};

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(mFilePath);
		dest.writeString(mFileName);
		dest.writeLong(mFileSize);
	}
	
	public void readFromParcel(Parcel in){
		mFilePath  = in.readString();
		mFileName = in.readString();
		mFileSize = in.readLong();
	}
}
