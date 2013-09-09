package com.dreamlink.communication.fileshare;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.ui.file.FileListAdapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class LocalFileFragment extends Fragment implements OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "LocalFileFragment";

	private Context mContext = null;

	private FileListAdapter mAdapter = null;

	//show file list
	private ListView mListView = null;
	//show tip msg
	private TextView mTipView = null;

	//All Folder & File ArrayList
	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	//Folder list
	private ArrayList<FileInfo> mFolderList = new ArrayList<FileInfo>();
	//Fil list
	private ArrayList<FileInfo> mFileList = new ArrayList<FileInfo>();
	//Hidden Folder list
	private ArrayList<FileInfo> mHiddenFolderList = new ArrayList<FileInfo>();
	//Hidden File list
	private ArrayList<FileInfo> mHiddenFileList = new ArrayList<FileInfo>();

	private Notice mNotice = null;
	
	private static final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	
	//Current directory
	private File mCurrentFile = new File(DEFAULT_PATH);
	public static String mCurrentPath = DEFAULT_PATH;
	//Parent directory
	private File mParentFile = null;
	
	//first show ui
	private boolean isFirst = true;
	
	private ActionMode mActionMode = null;
	private ActionModeCallBack mActionModeCallBack = new ActionModeCallBack();
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.filelist_2, container, false);
		
		mContext = getActivity();	
		
		mListView = (ListView) rootView.findViewById(R.id.file_listview);
		mTipView = (TextView) rootView.findViewById(R.id.tip_text);
		
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		
		mNotice = new Notice(mContext);
		
		browserTo(new File(DEFAULT_PATH));

		return rootView;
	}
	
	public void browserTo(File file){
		if (file.isDirectory()) {
			mCurrentPath = file.getAbsolutePath();
			
			clearList();
			
			fillList(file.listFiles());
			
			//sort
			Collections.sort(mFolderList);
			Collections.sort(mFileList);
			
			mList.addAll(mFolderList);
			mList.addAll(mFileList);
			
			if (isFirst) {
				isFirst = false;
				
				mAdapter = new FileListAdapter(mContext, mList);
				mListView.setAdapter(mAdapter);
			}else {
				mAdapter.notifyDataSetChanged();
			}
		}
	}
	
	private void clearList(){
		mList.clear();
		mFolderList.clear();
		mFileList.clear();
		mHiddenFolderList.clear();
		mHiddenFileList.clear();
	}
	
	//Fill
	private void fillList(File[] file){
		for (File currentFile : file) {
			FileInfo fileInfo = null;
			fileInfo = new FileInfo(currentFile.getName());
			fileInfo.fileDate = currentFile.lastModified();
			fileInfo.filePath = currentFile.getAbsolutePath();
			if (currentFile.isDirectory()) {
				fileInfo.isDir = true;
				fileInfo.fileSize = 0;
				if (currentFile.isHidden()) {
					mHiddenFolderList.add(fileInfo);
				}else {
					mFolderList.add(fileInfo);
				}
			}else {
				fileInfo.isDir = false;
				fileInfo.fileSize = currentFile.length();
				if (currentFile.isHidden()) {
					mHiddenFileList.add(fileInfo);
				}else {
					mFileList.add(fileInfo);
				}
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
			long id) {
//		mListView.startActionMode(mActionModeCallBack);
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == 0) {
			//user clicked back to parent
			if (DEFAULT_PATH.equals(mCurrentPath)) {
				//it is root now
			}else {
				browserTo(new File(mCurrentPath).getParentFile());
			}
		}else {
			position = position -1;
			File file = new File(mList.get(position).filePath);
			browserTo(file);
		}
	}
	
	protected class ActionModeCallBack implements Callback{
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.file_paste:
				//start copy
				mNotice.showToast("You click paste menu");
				break;
				
			case R.id.menu_cancel:
				mode.finish();
				break;
				
			default:
				break;
			}
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.file_menu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// TODO Auto-generated method stub
			return true;
		}
	}
}
