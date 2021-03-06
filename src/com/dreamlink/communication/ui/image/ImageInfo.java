package com.dreamlink.communication.ui.image;

import java.io.File;

import android.os.Parcel;
import android.os.Parcelable;

import com.dreamlink.communication.ui.DreamUtil;

public class ImageInfo implements Parcelable{

	/**sdcard中图片在数据库中保存的id*/
	private long image_id;
	/**image path*/
	private String path;
	/**bucket dispaly name*/
	private String bucketDisplayName;
	/**width*/
	private long width;
	/**height*/
	private long height;
	
	public static final String FILTER_MIMETYPE = "image/png";
	
	public static final Parcelable.Creator<ImageInfo> CREATOR = new Parcelable.Creator<ImageInfo>() {

		@Override
		public ImageInfo createFromParcel(Parcel source) {
			return new ImageInfo(source);
		}

		@Override
		public ImageInfo[] newArray(int size) {
			return new ImageInfo[size];
		}
	};
	public ImageInfo(){}
	
	public ImageInfo(long id){
		this.image_id = id;
	}
	
	private ImageInfo(Parcel in) {
		readFromParcel(in);
	}
	
	public long getImage_id() {
		return image_id;
	}
	public void setImage_id(long image_id) {
		this.image_id = image_id;
	}
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getBucketDisplayName() {
		return bucketDisplayName;
	}
	public void setBucketDisplayName(String bucketDisplayName) {
		this.bucketDisplayName = bucketDisplayName;
	}
	
	public long getWidth(){
		return width;
	}
	
	public void setWidth(long width){
		this.width = width;
	}
	
	public long getHeight(){
		return height;
	}
	
	public void setHeight(long height){
		this.height = height;
	}
	
	public String getName(){
		File file = new File(path);
		return file.getName();
	}
	
	public long getDate(){
		File file = new File(path);
		return file.lastModified();
	}
	
	public String getFormatDate(){
		return DreamUtil.getFormatDate(getDate());
	}
	
	public String getFormatSize(){
		long size = new File(path).length();
		return DreamUtil.getFormatSize(size);
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeLong(image_id);
		dest.writeString(path);
		dest.writeString(bucketDisplayName);
		dest.writeLong(width);
		dest.writeLong(height);
	}
	
	public void readFromParcel(Parcel in){
		image_id = in.readLong();
		path = in.readString();
		bucketDisplayName = in.readString();
		width = in.readLong();
		height = in.readLong();
	}
	
}
