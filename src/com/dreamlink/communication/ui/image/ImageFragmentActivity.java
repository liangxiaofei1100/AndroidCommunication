package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.media.MediaInfoManager;
import com.dreamlink.communication.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

public class ImageFragmentActivity extends FragmentActivity implements
		TabContentFactory, OnTabChangeListener {
	private static final String TAG = "ImageFragmentActivity";

	private TabHost mTabHost;
	private LayoutInflater layoutInflater;

	private static final String TAG_ONE = "相机";
	private static final String TAG_TWO = "图库";
	private static final String[] TAB_TAG = {TAG_ONE, TAG_TWO};
	private TextView mTextView1;
	private TextView mTextView2;

	private Context mContext;

	public static List<ImageInfo> mCamearLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mGalleryLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mImageInfos = new ArrayList<ImageInfo>();

	private static final String CAMERA_FOLDER = "Camera";
	public static final String PICTURE_ACTION = "picture.action";

	private static String CAMERA_TITLE;
	private static String GALLERY_TITLE;

	private BroadcastReceiver imageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			int cameraSize = intent.getIntExtra(Extra.CAMERA_SIZE,
					mCamearLists.size());
			int gallerySize = intent.getIntExtra(Extra.GALLERY_SIZE,
					mGalleryLists.size());
			mTextView1.setText(CAMERA_TITLE + "(" + cameraSize + ")");
			mTextView2.setText(GALLERY_TITLE + "(" + gallerySize + ")");
		}

	};

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_picture_tab);
		mContext = this;

		CAMERA_TITLE = getResources().getString(R.string.camera);
		GALLERY_TITLE = getResources().getString(R.string.gallery);

		IntentFilter filter = new IntentFilter(PICTURE_ACTION);
		registerReceiver(imageReceiver, filter);

		GetImagesTask getImagesTask = new GetImagesTask();
		getImagesTask.execute();

		if (arg0 != null) {
			mTabHost.setCurrentTabByTag(arg0.getString("picture_tab"));
		}

		initViews();
		
		//register image contentObserver,when image db change,update ui
		ImageContent imageContent = new ImageContent(new Handler());
		this.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imageContent);
		Log.d(TAG, "onCreate end");
	}

	public void initViews() {
		//实例化布局对象
		layoutInflater = LayoutInflater.from(this);
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();
		
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG[0]).setIndicator(getTabItemView(0))
				.setContent(R.id.picture_frag1));
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG[1])
				.setIndicator(getTabItemView(1)).setContent(R.id.picture_frag2));

		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
		mTextView1 = (TextView) mTabHost.getTabWidget().getChildAt(0)
				.findViewById(R.id.textview);
		mTextView2 = (TextView) mTabHost.getTabWidget().getChildAt(1)
				.findViewById(R.id.textview);
	}
	
	/**
	 * 给Tab按钮设置图标和文字
	 */
	private static final int[] VIS = {View.VISIBLE, View.GONE};
	private static final int[] VIS2 = {View.GONE, View.VISIBLE};
	private static final int[] COLORs = {0xff33b5e5, Color.GRAY};
	private View getTabItemView(int index){
		View view = layoutInflater.inflate(R.layout.tab_view, null);
	
		ImageView imageView = (ImageView) view.findViewById(R.id.cursor1);
		ImageView imageView2 = (ImageView) view.findViewById(R.id.cursor2);
		imageView.setVisibility(VIS[index]);
		imageView2.setVisibility(VIS2[index]);
		
		TextView textView = (TextView) view.findViewById(R.id.textview);		
		textView.setText(TAB_TAG[index]);
		textView.setTextColor(COLORs[index]);
		return view;
	}

	@Override
	public void onTabChanged(String tabId) {
		for (int j = 0; j < TAB_TAG.length; j++) {
			TextView textView = (TextView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.textview);
			ImageView imageView = (ImageView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.cursor1);
			ImageView imageView2 = (ImageView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.cursor2);
			if (tabId.equals(TAB_TAG[j])) {
				textView.setTextColor(0xff33b5e5);
				imageView.setVisibility(View.VISIBLE);
				imageView2.setVisibility(View.GONE);
			}else {
				textView.setTextColor(Color.GRAY);
				imageView.setVisibility(View.GONE);
				imageView2.setVisibility(View.VISIBLE);
			}
		}
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
			} else if (date1 == date2) {
				return 0;
			} else {
				return 1;
			}
		}
	};

	/**
	 * get images from the images db
	 */
	public class GetImagesTask extends AsyncTask<Void, String, Integer> {
		private MediaInfoManager mediaScan = new MediaInfoManager(mContext);

		@Override
		protected Integer doInBackground(Void... params) {
			mImageInfos.clear();
			mCamearLists.clear();
			mGalleryLists.clear();

			mImageInfos = mediaScan.getImageInfo();
			// mImageInfos = mediaScan.getImageThumbnail();
			// sort
			Collections.sort(mImageInfos, DATE_COMPARATOR);
			if (mImageInfos.size() <= 0) {
				return 0;
			} else {
				for (int i = 0; i < mImageInfos.size(); i++) {
					String folder = mImageInfos.get(i).getBucketDisplayName();
					if (CAMERA_FOLDER.equals(folder)) {
						mCamearLists.add(mImageInfos.get(i));
					} else {
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
			mTextView1.setText(CAMERA_TITLE + "(" + mCamearLists.size() + ")");
			mTextView2
					.setText(GALLERY_TITLE + "(" + mGalleryLists.size() + ")");
		}

	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
		sendBroadcast(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(imageReceiver);
	}
	
	//image contentObserver listener
	class ImageContent extends ContentObserver{
		public ImageContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			GetImagesTask getImagesTask = new GetImagesTask();
			getImagesTask.execute();
		}
	}
}
