package com.dreamlink.communication.ui.image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.R;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.TextView;

public class BaseImageFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnScrollListener {
	private static final String TAG = "BaseImageFragment";
	protected GridView mGridview;
	private TextView mEmptyView;
	
	private GalleryReceiver mGalleryReceiver;
	private ImageAdapter mAdapter;
	
	private Notice mNotice;
	
	private Context mContext;
	private int mCurrentPosition = -1;
	//用来保存GridView中每个Item的图片，以便释放
	public static Map<String,Bitmap> bitmapCaches = new HashMap<String,Bitmap>();
	
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
		Log.d(TAG, "onCreateView begin");
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_picture, container, false);
		mEmptyView = (TextView) rootView.findViewById(R.id.picture_empty_textview);
		
		mNotice = new Notice(mContext);
		
		mGridview = (GridView) rootView.findViewById(R.id.picture_gridview);
		mGridview.setEmptyView(mEmptyView);

		mGalleryReceiver = new GalleryReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ImageFragmentActivity.PICTURE_ACTION);
		mContext.registerReceiver(mGalleryReceiver, filter);

		mAdapter = new ImageAdapter(mContext, mList);
		mGridview.setAdapter(mAdapter);
		mGridview.setOnItemClickListener(this);
		mGridview.setOnItemLongClickListener(this);
		mGridview.setOnScrollListener(this);
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		Log.d(TAG, "onCreateView end");
		return rootView;
	}
	
	protected void setImageList(List<ImageInfo> list){
		mList = list;
	}
	
	protected void setEmptyViewText(int emptyTip){
		mEmptyView.setText(emptyTip);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//use custom activity to show picture
		startPagerActivityByPosition(position, mList);
		//use gallery show picture
//		Intent intent = FileInfoManager.getImageFileIntent(mList.get(position).getPath());
//		startActivity(intent);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final ImageInfo imageInfo = mList.get(position);
		new AlertDialog.Builder(mContext)
			.setTitle(imageInfo.getName())
			.setItems(R.array.picture_menu, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					switch (which) {
					case 0:
						//open
						startPagerActivityByPosition(position, mList);
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
//						FileInfoDialog fragment = FileInfoDialog.newInstance(imageInfo, FileInfoDialog.IMAGE_INFO);
//						fragment.show(getFragmentManager(), "Info");
						break;

					default:
						break;
					}
				}
			}).create().show();
		return true;
	}
	
	private void startPagerActivityByPosition(int position, List<ImageInfo> list){
		Intent intent = new Intent(mContext, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		intent.putParcelableArrayListExtra(Extra.IMAGE_INFO, (ArrayList<? extends Parcelable>) list);
		startActivity(intent);
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
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(mGalleryReceiver);
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
			mList.remove(position);
//			mAdapter.notifyDataSetChanged();
		}
		
		Intent intent = new Intent(ImageFragmentActivity.PICTURE_ACTION);
		mContext.sendBroadcast(intent);
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
	
	 //释放图片
  	private void recycleBitmapCaches(int fromPosition,int toPosition){		
  		Bitmap delBitmap = null;
  		for(int del=fromPosition;del<toPosition;del++){
  			delBitmap = bitmapCaches.get(mList.get(del));	
  			if(delBitmap != null){	
  				//如果非空则表示有缓存的bitmap，需要清理	
  				Log.d(TAG, "release position:"+ del);		
  				//从缓存中移除该del->bitmap的映射		
  				bitmapCaches.remove(mList.get(del));		
  				delBitmap.recycle();	
  				delBitmap = null;
  			}
  		}		
  	}

}
