package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.FileInfoDialog;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MediaInfo mediaInfo = mLists.get(position);
		mFileInfoManager.openFile(mediaInfo.getUrl());
	}
	
}
