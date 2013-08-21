package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.app.AppNormalFragment;
import com.dreamlink.communication.ui.dialog.FileInfoDialog;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.image.ImageFragmentActivity;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

public class MediaVideoFragment extends Fragment implements OnItemLongClickListener, OnItemClickListener {
	private static final String TAG = "MediaVideoFragment";
	private GridView mGridView;
	
	private MediaVideoAdapter mAdapter;
	
	private List<MediaInfo> mLists = new ArrayList<MediaInfo>();
	
	private MediaInfoManager mScan;
	//save current click position
	private int mCurrentPosition = -1;
	
	private FileInfoManager mFileInfoManager;
	private Context mContext;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_media_video, container, false);
		mGridView = (GridView) rootView.findViewById(R.id.video_gridview);
		mGridView.setEmptyView(rootView.findViewById(R.id.video_empty));
		mGridView.setOnItemClickListener(this);
//		mGridView.setOnItemLongClickListener(this);
		mGridView.setOnCreateContextMenuListener(new ListContextMenu(ListContextMenu.MENU_TYPE_VIDEO));
		
		mScan = new MediaInfoManager(mContext);
		
		GetVideosTask getVideosTask = new GetVideosTask();
		getVideosTask.execute("");
		
		mFileInfoManager = new FileInfoManager(mContext);
				
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	public  class GetVideosTask extends AsyncTask<String, String, String>{

		@Override
		protected String doInBackground(String... params) {
			mLists = mScan.getVideoInfo();
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mAdapter = new MediaVideoAdapter(mContext, mLists);
			mGridView.setAdapter(mAdapter);
			
			Intent intent = new Intent(DreamConstant.MEDIA_VIDEO_ACTION);
			intent.putExtra(Extra.VIDEO_SIZE, mLists.size());
			mContext.sendBroadcast(intent);
		}
		
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		mCurrentPosition = position;
		new ListContextMenu("Menu", ListContextMenu.MENU_TYPE_FILE);
		return true;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		int position = menuInfo.position;
		MediaInfo mediaInfo = mLists.get(position);
		switch (item.getItemId()) {
		case ListContextMenu.MENU_OPEN:
			mFileInfoManager.openFile(mediaInfo.getUrl());
			break;
		case ListContextMenu.MENU_SEND:
			break;
		case ListContextMenu.MENU_DELETE:
			showDeleteDialog(position, mediaInfo.getUrl());
			break;
		case ListContextMenu.MENU_INFO:
			String info = getVideoInfo(mediaInfo);
			DreamUtil.showInfoDialog(mContext, info);
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MediaInfo mediaInfo = mLists.get(position);
		mFileInfoManager.openFile(mediaInfo.getUrl());
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog(final int pos, final String path) {
    	new AlertDialog.Builder(mContext)
    		.setIcon(android.R.drawable.ic_delete)
    		.setTitle(R.string.menu_delete)
    		.setMessage(getResources().getString(R.string.confirm_msg, path))
    		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					doDelete(pos, path);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
    }
    
    private void doDelete(int position, String path) {
		mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI, path);
		mLists.remove(position);
		mAdapter.notifyDataSetChanged();
		
		Intent intent = new Intent(ImageFragmentActivity.PICTURE_ACTION);
		mContext.sendBroadcast(intent);
	}
    
    public String getVideoInfo(MediaInfo mediaInfo){
    	String result = "";
		result = "名称:" + mediaInfo.getDisplayName()+ DreamConstant.ENTER
				+ "类型:" + "视频" + DreamConstant.ENTER
				+ "位置:" + mediaInfo.getUrl() + DreamConstant.ENTER
				+ "大小:" + mediaInfo.getFormatSize()+ DreamConstant.ENTER
				+ "修改日期:" + mediaInfo.getFormatDate();
		return result;
    }
	
}
