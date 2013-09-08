package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.image.ImageFragmentActivity;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
	private Notice mNotice;
	private List<MediaInfo> mLists = new ArrayList<MediaInfo>();
	
	private MediaInfoManager mScan;
	//save current click position
	private int mCurrentPosition = -1;
	
	private FileInfoManager mFileInfoManager;
	private Context mContext;
	
	//video contentObserver listener
	class VideoContent extends ContentObserver{
		public VideoContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
//			GetVideosTask getVideosTask = new GetVideosTask();
//			getVideosTask.execute();
		}
	}
	
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
		mNotice = new Notice(mContext);
		
		GetVideosTask getVideosTask = new GetVideosTask();
		getVideosTask.execute();
		
		mFileInfoManager = new FileInfoManager(mContext);
				
		VideoContent videoContent  = new VideoContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(mScan.videoUri, true, videoContent);
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	public  class GetVideosTask extends AsyncTask<Void, String, String>{

		@Override
		protected String doInBackground(Void... params) {
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
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI, path);
		if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}else {
			mLists.remove(position);
			mAdapter.notifyDataSetChanged();
		}
		
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
