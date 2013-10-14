package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R.id;


//表示一个图片文件夹和文件夹里面的图片信息
public class PictureFolderInfo {

	/**
	 * image folder name
	 */
	private String bucket_display_name;
	
	/***
	 * save the image ids that in a same folder
	 */
	private List<Long> idList;
	
	public PictureFolderInfo(){
		idList = new ArrayList<Long>();
	}
	
	public void setBucketDisplayName(String name){
		this.bucket_display_name = name;
	}
	
	public String getBucketDisplayName(){
		return bucket_display_name;
	}
	
	public void addIdToList(long image_id){
		idList.add(image_id);
	}
	
	public List<Long> getIdList(){
		return idList;
	}
}
