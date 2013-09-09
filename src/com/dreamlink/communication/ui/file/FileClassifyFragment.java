package com.dreamlink.communication.ui.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.lib.util.Notice;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FileClassifyFragment extends Fragment implements OnItemClickListener, OnClickListener, OnItemLongClickListener {
	private static final String TAG = "FileClassifyFragment";
	private ListView mListView;
	private FileClassifyAdapter mAdapter;
	
	//save all lists
	private List<List<FileInfo>> mAllList = new ArrayList<List<FileInfo>>();
	
	//save classify list
	private List<FileInfo> mVideoList = new ArrayList<FileInfo>();
	private List<FileInfo> mDocList = new ArrayList<FileInfo>();
	private List<FileInfo> mTxtList = new ArrayList<FileInfo>();
	private List<FileInfo> mApkList = new ArrayList<FileInfo>();
	private List<FileInfo> mZipList = new ArrayList<FileInfo>();
	private List<FileInfo> mBigFileList = new ArrayList<FileInfo>();
	
	private static final String DEFAULT_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
	private static final long DEFAULT_MIN_SIZE = 50 * 1024 * 1024;//50M
	
	private FileInfoManager mFileInfoManager = null;
	private GetFilesTask mFilesTask = null;
	
	public static String[] file_types;
	public static String[] file_types_tips;
	
	private Notice mNotice = null;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			mAdapter.notifyDataSetChanged();
		};
	};
	
	//navigation title
	private LinearLayout mNavLayout;
	private View mDivider;
	private TextView mMainView;
	private TextView mClassifyView;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		View rootView = inflater.inflate(R.layout.ui_file_classify, container, false);
		mListView = (ListView) rootView.findViewById(R.id.classify_listview);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		
		mNavLayout = (LinearLayout) rootView.findViewById(R.id.classify_nav_layout);
		mDivider = rootView.findViewById(R.id.classify_divider);
		mMainView = (TextView) rootView.findViewById(R.id.file_classify_view1);
		mMainView.setOnClickListener(this);
		mClassifyView = (TextView) rootView.findViewById(R.id.file_classify_view2);
		mClassifyView.setOnClickListener(this);
		
		mFileInfoManager = new FileInfoManager(getActivity());
		file_types = getResources().getStringArray(R.array.file_classify);
		file_types_tips = getResources().getStringArray(R.array.file_classify_tip);
		
		mNotice = new Notice(getActivity());
		
		mFilesTask = new GetFilesTask();
		mFilesTask.execute("");
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	class GetFilesTask extends AsyncTask<String, String, String>{
		@Override
		protected String doInBackground(String... params) {
			File file = new File(DEFAULT_SDCARD);
			if (!file.exists()) {
				Log.e(TAG, DEFAULT_SDCARD + " is not exist");
			}else {
				listFiles(file);
			}
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setMainAdapter();
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			mHandler.sendEmptyMessage(1);
		}
	}
	
	private void listFiles(File file){
		File[] files = file.listFiles();
		FileInfo fileInfo = null;
		if (null == files) {
			return;
		}
		
		for(File file2 : files){
			if (file2.isHidden()) {
				//do not handler hide file
			}else {
				if (file2.isDirectory()) {
					listFiles(file2);
				}else {
					fileInfo = mFileInfoManager.getFileInfo(file2);
//					int type = mFileInfoManager.fileFilter(file2.getAbsolutePath());
					int type = fileInfo.type;
					switch (type) {
					case FileInfoManager.TYPE_EBOOK:
						mTxtList.add(fileInfo);
						break;
					case FileInfoManager.TYPE_VIDEO:
						mVideoList.add(fileInfo);
						break;
					case FileInfoManager.TYPE_APK:
						mApkList.add(fileInfo);
						break;
					case FileInfoManager.TYPE_ZIP:
						mZipList.add(fileInfo);
						break;
					case FileInfoManager.TYPE_DOC:
						mDocList.add(fileInfo);
						break;
					default:
						break;
					}
					
					if (file2.length() >= DEFAULT_MIN_SIZE) {
						mBigFileList.add(fileInfo);
					}
				}
				
				mAllList.add(mVideoList);
				mAllList.add(mDocList);
				mAllList.add(mTxtList);
				mAllList.add(mApkList);
				mAllList.add(mZipList);
				mAllList.add(mBigFileList);
				//update ui
				mFilesTask.onProgressUpdate("");
			}
		}
	}
	
	private int currentposition = -1;
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//		View 
		if (mAdapter.isMain) {
			String name = file_types[position];
			switch (position) {
			case 0:
				//VIDEO
				setItemAdapter(name, mVideoList, FileInfoManager.TYPE_VIDEO);
				break;
			case 1:
				//DOCUMENT
				setItemAdapter(name, mDocList, FileInfoManager.TYPE_DOC);
				break;
			case 2:
				//EBOOK
				setItemAdapter(name, mTxtList, FileInfoManager.TYPE_EBOOK);
				break;
			case 3:
				//APK
				setItemAdapter(name, mApkList, FileInfoManager.TYPE_APK);
				break;
			case 4:
				//ZIP
				setItemAdapter(name, mZipList, FileInfoManager.TYPE_ZIP);
				break;
			case 5:
				//BIG FILE
				setItemAdapter(name, mBigFileList, FileInfoManager.TYPE_BIG_FILE);
				break;

			default:
				break;
			}
		}else {
			int fileType = mAdapter.getFileType();
			List<FileInfo> list = getList(fileType);
			mFileInfoManager.openFile(list.get(position).filePath);
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		if (mAdapter.isMain) {
			return false;
		}else {
			final int fileType = mAdapter.getFileType();
			final List<FileInfo> list = getList(fileType);
			final FileInfo fileInfo = list.get(position);
			new AlertDialog.Builder(getActivity())
			.setTitle(fileInfo.fileName)
			.setItems(R.array.file_menu, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							//open
							mFileInfoManager.openFile(fileInfo.filePath);
							break;
						case 1:
							//send
							FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(fileInfo.filePath));
							
							FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
							fileSendUtil.sendFile(fileTransferInfo);
							break;
						case 2:
							//delete
							showDeleteDialog(list, position, fileInfo.filePath, fileType);
							break;
						case 3:
							//info
							mFileInfoManager.showInfoDialog(fileInfo);
							break;

						default:
							break;
						}
				}
			}).create().show();
		}
		return true;
	}
	
	private List<FileInfo> getList(int type){
		List<FileInfo> returnList = new ArrayList<FileInfo>();
		switch (type) {
		case FileInfoManager.TYPE_VIDEO:
			returnList = mVideoList;
			break;
		case FileInfoManager.TYPE_DOC:
			returnList = mDocList;
			break;
		case FileInfoManager.TYPE_EBOOK:
			returnList = mTxtList;
			break;
		case FileInfoManager.TYPE_APK:
			returnList = mApkList;
			break;
		case FileInfoManager.TYPE_ZIP:
			returnList = mZipList;
			break;
		case FileInfoManager.TYPE_BIG_FILE:
			returnList = mBigFileList;
			break;
		default:
			break;
		}
		
		return returnList;
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog(final List<FileInfo> list, final int pos, final String path, final int type) {
    	final FileDeleteDialog deleteDialog = new FileDeleteDialog(getActivity(), R.style.TransferDialog, path);
		deleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					doDelete(list, pos, path, type);
					break;
				default:
					break;
				}
			}
		});
		deleteDialog.show();
    }
    
    public void doDelete(List<FileInfo> list ,int position, String path, int type){
    	boolean ret = false;
    	switch (type) {
		case FileInfoManager.TYPE_AUDIO:
			ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.AUDIO_URI,
					path);
			break;
		case FileInfoManager.TYPE_VIDEO:
			ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI,
					path);
			break;
		case FileInfoManager.TYPE_IMAGE:
			ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI,
					path);
		default:
			ret = mFileInfoManager.deleteFile(path);
			break;
		}
    	
    	if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}else {
			list.remove(position);
			mAdapter.notifyDataSetChanged();
		}
    }
	
	private void setMainAdapter(){
		mNavLayout.setVisibility(View.GONE);
		mDivider.setVisibility(View.GONE);
		mAdapter = new FileClassifyAdapter(getActivity(), mAllList);
		mListView.setAdapter(mAdapter);
	}
	
	private void setItemAdapter(String itemName, List<FileInfo> list, int type){
		mNavLayout.setVisibility(View.VISIBLE);
		mDivider.setVisibility(View.VISIBLE);
		mClassifyView.setText(itemName);
		mAdapter = new FileClassifyAdapter(getActivity(), list, type);
		mListView.setAdapter(mAdapter);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.file_classify_view1:
			setMainAdapter();
			break;

		default:
			break;
		}
	}
}
