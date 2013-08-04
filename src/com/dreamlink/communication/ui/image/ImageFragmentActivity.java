package com.dreamlink.communication.ui.image;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.MainUIFrame;
import com.dreamlink.communication.ui.media.MediaInfo;
import com.dreamlink.communication.ui.media.MediaInfoManager;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

public class ImageFragmentActivity extends FragmentActivity implements TabContentFactory, OnTabChangeListener{
	private static final String TAG = "ImageFragmentActivity";
	
	private TabHost mTabHost;
	
	private static final String TAG_ONE = "one";
	private static final String TAG_TWO = "two";
	private TextView mTextView1;
	private TextView mTextView2;
	
	private Context mContext;
	
	public static List<ImageInfo> mCamearLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mGalleryLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mImageInfos = new ArrayList<ImageInfo>();
	
	private static final String CAMERA_FOLDER = "Camera";
	
	public static final String PICTURE_ACTION = "picture.action";
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_picture_tab);
		mContext = this;

		GetImagesTask getImagesTask = new GetImagesTask();
		getImagesTask.execute("");
		
		if (arg0 != null){
			mTabHost.setCurrentTabByTag(arg0.getString("picture_tab"));
		}
		
		initViews();
		Log.d(TAG, "onCreate end");
	}
	
	public void initViews(){
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(TAG_ONE).setIndicator("相机").setContent(R.id.picture_frag1));
		mTabHost.addTab(mTabHost.newTabSpec(TAG_TWO).setIndicator("图库").setContent(R.id.picture_frag2));
		
		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
		mTextView1 = (TextView) mTabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
		mTextView2 = (TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title);
	}

	@Override
	public void onTabChanged(String tabId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public View createTabContent(String tag) {
		// TODO Auto-generated method stub
		return null;
	}
	
	 /**
     * 按修改日期排序
     */
    public static final Comparator<ImageInfo> DATE_COMPARATOR = new Comparator<ImageInfo>() {
        @Override
        public int compare(ImageInfo object1, ImageInfo object2) {
        	long date1 = object1.getDate();
        	long date2 = object2.getDate();
        	if (date1 > date2) {
				return -1;
			}else if (date1 == date2) {
				return 0;
			}else {
				return 1;
			}
        }
    };
	
    /**
     * get images from the images db
     */
	public class GetImagesTask extends AsyncTask<String, String, Integer>{
		private MediaInfoManager mediaScan = new MediaInfoManager(mContext);
		@Override
		protected Integer doInBackground(String... params) {
			mImageInfos.clear();
			mCamearLists.clear();
			mGalleryLists.clear();
			
			mImageInfos = mediaScan.getImageInfo();
//			mImageInfos = mediaScan.getImageThumbnail();
			//sort
			Collections.sort(mImageInfos, DATE_COMPARATOR);
			if (mImageInfos.size() <= 0) {
				return 0;
			}else {
				for (int i = 0; i < mImageInfos.size(); i++) {
					String folder = mImageInfos.get(i).getBucketDisplayName();
					if (CAMERA_FOLDER.equals(folder)) {
						mCamearLists.add(mImageInfos.get(i));
					}else {
						mGalleryLists.add(mImageInfos.get(i));
					}
				}
				Log.d(TAG, mCamearLists.size() + "");
				Log.d(TAG, mGalleryLists.size() + "");
				return 1;
			}
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Intent intent = new Intent(PICTURE_ACTION);
			sendBroadcast(intent);
			mTextView1.setText("相机(" + mCamearLists.size() +")");
			mTextView2.setText("图库(" + mGalleryLists.size() +")");
		}
		
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		Intent intent = new Intent(MainUIFrame.EXIT_ACTION);
		sendBroadcast(intent);
	}
	
}
