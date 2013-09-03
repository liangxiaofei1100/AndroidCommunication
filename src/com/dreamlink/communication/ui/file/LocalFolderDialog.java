package com.dreamlink.communication.ui.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.MountManager;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class LocalFolderDialog extends Activity implements OnItemClickListener, OnClickListener {

	private ListView mListView = null;
	private Button mCreateBtn, mOkBtn, mCancelBtn;
	private ImageButton mHomeBtn,mUpBtn;
	private LinearLayout mMenuLayout;

	private TextView mEmptyView = null;

	private FileListAdapter mAdapter = null;

	// All Folder & File ArrayList
	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	// Folder list
	private ArrayList<FileInfo> mFolderList = new ArrayList<FileInfo>();
	// Fil list
	private ArrayList<FileInfo> mFileList = new ArrayList<FileInfo>();
	// Hidden Folder list
	private ArrayList<FileInfo> mHiddenFolderList = new ArrayList<FileInfo>();
	// Hidden File list
	private ArrayList<FileInfo> mHiddenFileList = new ArrayList<FileInfo>();

	private Notice mNotice = null;

	// first show ui
	private boolean isFirst = true;

	public static String mCurrentPath;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_localfolder);
		setTitle("文件拷贝");

		mNotice = new Notice(getApplicationContext());

		mListView = (ListView) findViewById(R.id.listview);
		mCreateBtn = (Button) findViewById(R.id.create_button);
		mOkBtn = (Button) findViewById(R.id.ok_button);
		mCancelBtn = (Button) findViewById(R.id.cancel_button);
		mEmptyView = (TextView) findViewById(R.id.empty_view);
		mHomeBtn = (ImageButton) findViewById(R.id.root_view);
		mUpBtn = (ImageButton) findViewById(R.id.up_view);
		
		mMenuLayout = (LinearLayout) findViewById(R.id.menulayout);

		mCreateBtn.setOnClickListener(this);
		mOkBtn.setOnClickListener(this);
		mCancelBtn.setOnClickListener(this);
		mHomeBtn.setOnClickListener(this);
		mUpBtn.setOnClickListener(this);

		mListView.setEmptyView(mEmptyView);
		mListView.setOnItemClickListener(this);

		if (MountManager.NO_EXTERNAL_SDCARD.equals(MountManager.SDCARD_PATH)
				&& MountManager.NO_INTERNAL_SDCARD.equals(MountManager.INTERNAL_PATH)) {
			mEmptyView.setText("不存在SD卡");
			mMenuLayout.setVisibility(View.INVISIBLE);
		} else if (MountManager.NO_EXTERNAL_SDCARD.equals(MountManager.SDCARD_PATH)) {
			mCurrentPath = MountManager.INTERNAL_PATH;
			browserTo(new File(MountManager.INTERNAL_PATH));
			mMenuLayout.setVisibility(View.VISIBLE);
		} else {
			mCurrentPath = MountManager.SDCARD_PATH;
			browserTo(new File(MountManager.SDCARD_PATH));
			mMenuLayout.setVisibility(View.VISIBLE);
		}
	};

	public void browserTo(File file) {
		if (file.isDirectory()) {
			mCurrentPath = file.getAbsolutePath();

			clearList();

			fillList(file.listFiles());

			// sort
			Collections.sort(mFolderList);
			Collections.sort(mFileList);

			mList.addAll(mFolderList);
			mList.addAll(mFileList);

			if (isFirst) {
				isFirst = false;

				mAdapter = new FileListAdapter(this, mList);
				mListView.setAdapter(mAdapter);
			} else {
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	private void clearList() {
		mList.clear();
		mFolderList.clear();
		mFileList.clear();
		mHiddenFolderList.clear();
		mHiddenFileList.clear();
	}

	// Fill
	private void fillList(File[] file) {
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
				} else {
					mFolderList.add(fileInfo);
				}
			} else {
				fileInfo.isDir = false;
				fileInfo.fileSize = currentFile.length();
				if (currentFile.isHidden()) {
					mHiddenFileList.add(fileInfo);
				} else {
					mFileList.add(fileInfo);
				}
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		FileInfo fileInfo = mList.get(position);
		if (fileInfo.isDir) {
			File file = new File(mList.get(position).filePath);
			browserTo(file);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.create_button:
			break;
		case R.id.ok_button:
			Intent intent = new Intent();
			intent.putExtra(Extra.COPY_PATH, mCurrentPath);
			setResult(RESULT_OK, intent);
			finish();
			break;
		case R.id.cancel_button:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.root_view:
			if (MountManager.NO_EXTERNAL_SDCARD.equals(MountManager.SDCARD_PATH)) {
				mCurrentPath = MountManager.INTERNAL_PATH;
				browserTo(new File(MountManager.INTERNAL_PATH));
			} else {
				mCurrentPath = MountManager.SDCARD_PATH;
				browserTo(new File(MountManager.SDCARD_PATH));
			}
			break;
		case R.id.up_view:
			if (mCurrentPath.equals(MountManager.SDCARD_PATH) || mCurrentPath.equals(MountManager.INTERNAL_PATH)) {
				mNotice.showToast("is root path now");
			} else {
				browserTo(new File(mCurrentPath).getParentFile());
			}
			break;

		default:
			break;
		}
	}
}
