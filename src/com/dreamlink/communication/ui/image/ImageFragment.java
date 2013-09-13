package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.history.HistoryManager;
import com.dreamlink.communication.ui.image.BaseImageFragment.GalleryReceiver;
import com.dreamlink.communication.ui.image.ImageFragmentActivity.GetImagesTask;
import com.dreamlink.communication.ui.media.MediaInfoManager;
import com.dreamlink.communication.util.Log;

import android.R.integer;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ImageFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnScrollListener, OnClickListener {
	private static final String TAG = "ImageFragment";
	protected GridView mGridview;
	private TextView mEmptyView;

	// title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	private GalleryReceiver mGalleryReceiver;
	private ImageAdapter mAdapter;

	private Context mContext;
	// 用来保存GridView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();

	private FileInfoManager mFileInfoManager;
	private List<ImageInfo> mList = new ArrayList<ImageInfo>();

	public static List<ImageInfo> mCamearLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mGalleryLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mImageInfos = new ArrayList<ImageInfo>();

	private static final String CAMERA_FOLDER = "Camera";
	private int mNum;
	private GetImagesTask task = null;

	/**
	 * Create a new instance of CountingFragment, providing "num" as an
	 * argument.
	 */
	public static ImageFragment newInstance(int num) {
		ImageFragment f = new ImageFragment();

		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putInt("num", num);
		f.setArguments(args);

		return f;
	}
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				mTitleNum.setText("(" + size + ")");
				break;
			default:
				break;
			}
		};
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mNum = getArguments() != null ? getArguments().getInt("num") : 1;
	}

	private void getTitleVIews(View view) {
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_image);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("图库");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("(N)");
		mRefreshView.setOnClickListener(this);
		mHistoryView.setOnClickListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.ui_picture, container, false);
		mEmptyView = (TextView) rootView.findViewById(R.id.picture_empty_textview);
		mGridview = (GridView) rootView.findViewById(R.id.picture_gridview);
		mGridview.setEmptyView(mEmptyView);

		getTitleVIews(rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, "onActivityCreated");
		mContext = getActivity();

		// mGalleryReceiver = new GalleryReceiver();
		// IntentFilter filter = new IntentFilter();
		// filter.addAction(ImageFragmentActivity.PICTURE_ACTION);
		// mContext.registerReceiver(mGalleryReceiver, filter);
		if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
			return;
		}
		task = new GetImagesTask();
		task.execute();

		mGridview.setOnItemClickListener(this);
		mGridview.setOnItemLongClickListener(this);
		mGridview.setOnScrollListener(this);

		mFileInfoManager = new FileInfoManager(mContext);
	}

	public void setAdapter() {
		// if (null == mAdapter) {
		// // Log.d(TAG, "nulllll");
		// mAdapter = new ImageAdapter(mContext, mImageInfos);
		// mGridview.setAdapter(mAdapter);
		// }else {
		// Log.d(TAG, "not nulllll");
		// // mAdapter.notifyDataSetChanged();
		// mAdapter.notifyDataSetInvalidated();
		// }

		// 使用上面的方法，再次刷新时无法显示数据
		mAdapter = new ImageAdapter(mContext, mImageInfos);
		mGridview.setAdapter(mAdapter);
	}

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
			Collections.sort(mImageInfos, ImageFragmentActivity.DATE_COMPARATOR);
			// if (mImageInfos.size() <= 0) {
			// return 0;
			// } else {
			// for (int i = 0; i < mImageInfos.size(); i++) {
			// String folder = mImageInfos.get(i).getBucketDisplayName();
			// if (CAMERA_FOLDER.equals(folder)) {
			// mCamearLists.add(mImageInfos.get(i));
			// } else {
			// mGalleryLists.add(mImageInfos.get(i));
			// }
			// }
			// Log.d(TAG, mCamearLists.size() + "");
			// Log.d(TAG, mGalleryLists.size() + "");
			// return 1;
			// }
			return mImageInfos.size();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Log.d(TAG, "onPostExecute.size=" + result);
			setAdapter();
			
			Message message = mHandler.obtainMessage();
			message.arg1 = mImageInfos.size();
			message.what = MSG_UPDATE_UI;
			message.sendToTarget();
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
			mAdapter.setIdleFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			mAdapter.setIdleFlag(true);
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			mAdapter.setIdleFlag(false);
			break;

		default:
			break;
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		// 注释：firstVisibleItem为第一个可见的Item的position，从0开始，随着拖动会改变
		// visibleItemCount为当前页面总共可见的Item的项数
		// totalItemCount为当前总共已经出现的Item的项数
		recycleBitmapCaches(0, firstVisibleItem);
		recycleBitmapCaches(firstVisibleItem + visibleItemCount, totalItemCount);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub

	}

	// 释放图片
	private void recycleBitmapCaches(int fromPosition, int toPosition) {
		Bitmap delBitmap = null;
		for (int del = fromPosition; del < toPosition; del++) {
			delBitmap = bitmapCaches.get(mImageInfos.get(del));
			if (delBitmap != null) {
				// 如果非空则表示有缓存的bitmap，需要清理
				Log.d(TAG, "release position:" + del);
				// 从缓存中移除该del->bitmap的映射
				bitmapCaches.remove(mImageInfos.get(del));
				delBitmap.recycle();
				delBitmap = null;
			}
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
}
