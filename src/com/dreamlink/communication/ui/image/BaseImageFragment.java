package com.dreamlink.communication.ui.image;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DeleteDialog;
import com.dreamlink.communication.ui.FileInfoDialog;
import com.dreamlink.communication.ui.DeleteDialog.ConfirmListener;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.media.MediaInfo;
import com.dreamlink.communication.util.Log;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class BaseImageFragment extends BaseFragment implements OnItemClickListener, ConfirmListener {
	private static final String TAG = "BaseImageFragment";
	private DisplayImageOptions options;
	protected GridView mGridview;
	private TextView mTextView;
	
	private GalleryReceiver mGalleryReceiver;
	private ImageAdapter mAdapter;
	
	private Context mContext;
	private int mCurrentPosition = -1;
	
	private FileInfoManager mFileInfoManager;
	private List<ImageInfo> mList = new ArrayList<ImageInfo>();
	public class GalleryReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ImageFragmentActivity.PICTURE_ACTION.equals(action)) {
					mAdapter.notifyDataSetChanged();
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_picture, container, false);
		mTextView = (TextView) rootView.findViewById(R.id.picture_empty_textview);
		
		options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.zapya_data_photo_l)
				.showImageForEmptyUri(R.drawable.zapya_data_photo_l)
				.showImageOnFail(R.drawable.ic_picture_error)
				.cacheInMemory(true)
				.cacheOnDisc(true)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.build();
		
		mGridview = (GridView) rootView.findViewById(R.id.picture_gridview);
		mGridview.setEmptyView(rootView.findViewById(R.id.picture_empty_textview));
//		//注册一个广播，用于接收图片获取完毕
		mGalleryReceiver = new GalleryReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ImageFragmentActivity.PICTURE_ACTION);
		mContext.registerReceiver(mGalleryReceiver, filter);

		mAdapter = new ImageAdapter(inflater, mList, imageLoader, options);
		mGridview.setAdapter(mAdapter);
		mGridview.setOnItemClickListener(this);
		setContextMenu();
		applyScrollListener();
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	protected void setImageList(List<ImageInfo> list){
		mList = list;
	}
	
	protected void setContextMenu() {
		mGridview.setOnCreateContextMenuListener(new ListContextMenu("Menu", ListContextMenu.MENU_TYPE_IMAGE));
	}
	
	private void applyScrollListener(){
		mGridview.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, false));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		startPagerActivityByPosition(position, mList);
	}
	
	private void startPagerActivityByPosition(int position, List<ImageInfo> list){
		Intent intent = new Intent(mContext, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		intent.putParcelableArrayListExtra(Extra.IMAGE_INFO, (ArrayList<? extends Parcelable>) list);
		startActivity(intent);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(mGalleryReceiver);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
//		for (int i = 0; i < mList.size(); i++) {
//			System.out.println(mList.get(i).getPath());
//		}
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		int position = menuInfo.position;
		ImageInfo imageInfo = mList.get(position);
		switch (item.getItemId()) {
		case ListContextMenu.MENU_OPEN:
			startPagerActivityByPosition(position, mList);
			break;
		case ListContextMenu.MENU_SEND:
			break;
		case ListContextMenu.MENU_DELETE:
			showDeleteDialog(imageInfo.getPath());
			break;
		case ListContextMenu.MENU_INFO:
			FileInfoDialog fragment = FileInfoDialog.newInstance(imageInfo, FileInfoDialog.IMAGE_INFO);
			fragment.show(getFragmentManager(), "Info");
			break;
		default:
			break;
		}
		return true;
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog(String path) {
        DeleteDialog fragment = DeleteDialog.newInstance(path);
        fragment.setConfirmListener(this);
        fragment.show(getFragmentManager(), "Delete");
    }

	@Override
	public void confirm(String path) {
		File file = new File(path);
		if (!file.exists()) {
			Log.e("Yuri", path + " is not exist");
		}else {
			boolean ret = file.delete();
			if (!ret) {
				Log.e("Yuri", path + " delete failed");
			}else {
				mList.remove(mCurrentPosition);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

}
