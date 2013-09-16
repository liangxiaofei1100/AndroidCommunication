package com.dreamlink.communication.ui.image;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.R;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.media.MediaInfoManager;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ImageFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnScrollListener, OnClickListener {
	private static final String TAG = "ImageFragment";
	protected GridView mGridview;
	private TextView mEmptyView;
	private ProgressBar mLoadingBar;

	// title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	private ImageAdapter mAdapter;

	private Context mContext;
	// 用来保存GridView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();

	private FileInfoManager mFileInfoManager;

	public static List<ImageInfo> mCamearLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mGalleryLists = new ArrayList<ImageInfo>();
	public static List<ImageInfo> mImageList = new ArrayList<ImageInfo>();

	private static final String CAMERA_FOLDER = "Camera";
	public static final String PICTURE_ACTION = "picture.action";
	private int mAppId;
	private GetImagesTask task = null;

	/**
	 * Create a new instance of ImageFragment, providing "appid" as an
	 * argument.
	 */
	public static ImageFragment newInstance(int appid) {
		ImageFragment f = new ImageFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
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
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	}

	private void getTitleVIews(View view) {
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_image);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.gallery);
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("");
		mRefreshView.setOnClickListener(this);
		mHistoryView.setOnClickListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.ui_picture, container, false);
		mEmptyView = (TextView) rootView.findViewById(R.id.picture_empty_textview);
		mGridview = (GridView) rootView.findViewById(R.id.picture_gridview);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_loading_image);

		getTitleVIews(rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, "onActivityCreated");
		mContext = getActivity();

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
		mAdapter = new ImageAdapter(mContext, mImageList);
		mGridview.setAdapter(mAdapter);
	}

	/**
	 * get images from the images db
	 */
	public class GetImagesTask extends AsyncTask<Void, String, Integer> {
		private MediaInfoManager mediaScan = new MediaInfoManager(mContext);

		@Override
		protected Integer doInBackground(Void... params) {
			mImageList.clear();
			mImageList = mediaScan.getImageInfo();
			// sort
			Collections.sort(mImageList, DATE_COMPARATOR);
			return mImageList.size();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mLoadingBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Log.d(TAG, "onPostExecute.size=" + result);
			mLoadingBar.setVisibility(View.GONE);
			if (result <= 0) {
				mEmptyView.setVisibility(View.VISIBLE);
			}else {
				setAdapter();
			}
			
			updateUI(result);
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
		// 注释：firstVisibleItem为第一个可见的Item的position，从0开始，随着拖动会改变
		// visibleItemCount为当前页面总共可见的Item的项数
		// totalItemCount为当前总共已经出现的Item的项数
		recycleBitmapCaches(0, firstVisibleItem);
		recycleBitmapCaches(firstVisibleItem + visibleItemCount, totalItemCount);
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

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final ImageInfo imageInfo = mImageList.get(position);
		new AlertDialog.Builder(mContext)
			.setTitle(imageInfo.getName())
			.setItems(R.array.picture_menu, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						//open
						startPagerActivityByPosition(position, mImageList);
						break;
					case 1:
						//send
						FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(imageInfo.getPath()));
						FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
						fileSendUtil.sendFile(fileTransferInfo);
						break;
					case 2:
						//delete
						showDeleteDialog(position, imageInfo.getPath());
						break;
					case 3:
						//info
						String info = getImageInfo(imageInfo);
						DreamUtil.showInfoDialog(mContext, info);
						break;

					default:
						break;
					}
				}
			}).create().show();
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		startPagerActivityByPosition(position, mImageList);
	}
	
	private String getImageInfo(ImageInfo imageInfo){
		String result = "";
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			result = "名称:" + imageInfo.getName() + DreamConstant.ENTER
					+ "类型:" + "图片" + DreamConstant.ENTER
					+ "位置:" + imageInfo.getPath() + DreamConstant.ENTER
					+ "大小:" + imageInfo.getFormatSize()+ DreamConstant.ENTER
					+ "宽度:" +  imageInfo.getWidth() + DreamConstant.ENTER
					+ "高度:" + imageInfo.getHeight() + DreamConstant.ENTER
					+ "修改日期:" + imageInfo.getFormatDate();
		}else {
			result = "名称:" + imageInfo.getName() + DreamConstant.ENTER
					+ "类型:" + "图片" + DreamConstant.ENTER
					+ "位置:" + imageInfo.getPath() + DreamConstant.ENTER
					+ "大小:" + imageInfo.getFormatSize()+ DreamConstant.ENTER
					+ "修改日期:" + imageInfo.getFormatDate();
		}
		return result;
	}
	
	private void startPagerActivityByPosition(int position, List<ImageInfo> list){
		Intent intent = new Intent(mContext, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		intent.putParcelableArrayListExtra(Extra.IMAGE_INFO, (ArrayList<? extends Parcelable>) list);
		startActivity(intent);
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog(final int pos, final String path) {
    	//do not use dialogfragment
		final FileDeleteDialog deleteDialog = new FileDeleteDialog(mContext, R.style.TransferDialog, path);
		deleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					doDelete(pos, path);
					break;
				default:
					break;
				}
			}
		});
		deleteDialog.show();
    }
    
	private void doDelete(int position, String path) {
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI, path);
		if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}else {
			mImageList.remove(position);
			mAdapter.notifyDataSetChanged();
			updateUI(mImageList.size());
		}
		
		Intent intent = new Intent(PICTURE_ACTION);
		mContext.sendBroadcast(intent);
	}
	
	public void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}

	// 释放图片
	private void recycleBitmapCaches(int fromPosition, int toPosition) {
		Bitmap delBitmap = null;
		for (int del = fromPosition; del < toPosition; del++) {
			delBitmap = bitmapCaches.get(mImageList.get(del));
			if (delBitmap != null) {
				// 如果非空则表示有缓存的bitmap，需要清理
				Log.d(TAG, "release position:" + del);
				// 从缓存中移除该del->bitmap的映射
				bitmapCaches.remove(mImageList.get(del));
				delBitmap.recycle();
				delBitmap = null;
			}
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_refresh:
			
			break;
			
		case R.id.iv_history:
			MainFragmentActivity.instance.goToHistory();
//			Intent intent = new Intent();
//			intent.putExtra(Extra.APP_ID, mAppId);
//			intent.setClass(getActivity(), HistoryActivity.class);
//			startActivity(intent);
			break;

		default:
			break;
		}
	}
}
