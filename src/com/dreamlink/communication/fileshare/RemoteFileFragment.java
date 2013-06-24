package com.dreamlink.communication.fileshare;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

/**
 * list remote server /sdcard files to client
 * @author yuri
 */
public class RemoteFileFragment extends Fragment implements OnCommunicationListener, 
							OnItemClickListener{
	private static final String TAG = "RemoteFileFragment";
	
	//show file list
	private ListView mListView = null;
	//show tip msg
	private TextView mTipView = null;
	
	private Context mContext = null;
	private SocketCommunicationManager mCommunicationManager = null;
	
	//File Array
	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	
	private FileListAdapter mAdapter = null;
	
	private Notice mNotice = null;
	
	//save current path
	private  String currentPath = "";
	//save parent path
	private  String parentPath = "";
	
	private static final int UPDATE_UI = 0x00;
	
	private ActionMode mActionMode = null;
	private RemoteActionModeCallBack mActionModeCallBack = new RemoteActionModeCallBack();
	
	/**record the file that need copy*/
	public FileInfo currentCopyFile = null;
	public static String copyPath = "";
	public  String currentFileNmae = "";
	private FileOutputStream fos = null;
	
	private int copyLen = 0;
	
	/**file transfer progress dialog*/
	//use custom progressbar dialog
	private ProgressBarDialog mFileTransferBarDialog = null;
	/**file list progress dialog*/
	private ProgressDialog mFileListDialog = null;
	
	//test var
	private long start_time = 0;
	private long end_time = 0;
	//record time to jisuan transfer speed
	private long now_time = 0;
	//start a thread record transfer speed
	private Timer mSpeedTimer;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.filelist_2, container, false);
		
		mContext = getActivity();
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext,true);
		mCommunicationManager.registered(this);
		
		mListView = (ListView) rootView.findViewById(R.id.file_listview);
		mTipView = (TextView) rootView.findViewById(R.id.tip_text);
		
		// is connected to server
		if (mCommunicationManager.getCommunications().isEmpty()) {
			mTipView.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
		} else {
			mTipView.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
			
			if (mFileListDialog == null) {
				mFileListDialog = new ProgressDialog(mContext);
				mFileListDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mFileListDialog.setIndeterminate(true);
				mFileListDialog.setCancelable(false);
				mFileListDialog.show();
			}
			
			//tell server that i want look u sdcard files
			String cmdMsg = Command.LS + Command.AITE + Command.ROOT_PATH;
			sendCommandMsg(cmdMsg);
		}
		
		mAdapter = new FileListAdapter(mContext, mList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnCreateContextMenuListener(new ListOnCreateContext());
		
		mNotice = new Notice(mContext);

		return rootView;
	}
	
	/**
	 * send command to remote connected server
	 * @param cmd command
	 * @param path file path
	 */
	public void sendCommandMsg(String cmdMsg){
		mCommunicationManager.sendMessage(cmdMsg.getBytes(), 0);
	}

	private static String wholeReceiveMsg = "";
	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		Log.d(TAG, "onReceiveMessage:" + msg.length);
		
		if (currentCopyFile != null) {
			//receive file from server
			try {
				copyLen += msg.length;
				if (mFileTransferBarDialog != null) {
					mFileTransferBarDialog.setDProgress(copyLen);
				}else {
					Log.e(TAG, "mProgressDialog is null");
				}

				Log.d(TAG, "copyLen=" + copyLen);
				Log.d(TAG, "totalSize=" + (int)currentCopyFile.fileSize);
				//use FileOutPutStream do not use DataOutPutStrem
				if (fos != null) {
					fos.write(msg);
					fos.flush();
				} else {
					Log.e(TAG, "fos is null");
				}
				
				//receive over,close fileoutputstream
				if (copyLen == ((int)currentCopyFile.fileSize)) {
					///////////for test/////////////
					end_time = System.currentTimeMillis();
					Log.d(TAG, "end copy time:"+ end_time);
					//bytes/second
					long bytes_per = (long) (currentCopyFile.fileSize /((end_time - start_time) / 1000));
					Log.d(TAG, "bytes/per:" + bytes_per);
					///////////for test/////////////
					
					//********clear begin***********//
					fos.close();
					fos = null;
					
					currentCopyFile = null;
					copyLen = 0;
					
					start_time = 0;
					end_time = 0;
					now_time = 0;
					if (mSpeedTimer != null) {
						mSpeedTimer.cancel();
						mSpeedTimer = null;
					}
					
					////////////////
					if (mFileTransferBarDialog != null) {
						mFileTransferBarDialog.cancel();
					}
					//********clear end***********//
				}
				
			} catch (Exception e) {
				Log.e(TAG, "Receive error:" + e.toString());
				e.printStackTrace();
			}
		}else {
			// Clear File array list
			mList.clear();
			
			// convert byte[] to String
			String ret_msg = new String(msg);

			// split msg
			//the first line is command flag
			//the last line is end flag
			String[] splitMsg = ret_msg.split(Command.ENTER);
			
			// 看到结束标志，才真的结束，否则不解析
			if (Command.END_FLAG.equals(splitMsg[splitMsg.length - 1])) {
				wholeReceiveMsg += ret_msg;
				String[] newSplitMsg = wholeReceiveMsg.split(Command.ENTER);
				if (Command.LSRETN.equals(newSplitMsg[0])) {
					//LSRETN, is ls command return msg
					//every line is a folder or file
					//command format:[LSRTN][...]
					currentPath = newSplitMsg[1];
					parentPath = newSplitMsg[2];

					//folder list
					ArrayList<FileInfo> folderList = new ArrayList<FileInfo>();
					//file list
					ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

					FileInfo fileInfo = null;
					for (int i = 3; i < newSplitMsg.length - 1; i++) {
						// split again
						String[] fileStr = newSplitMsg[i].split(Command.SEPARTOR);
						if (fileStr.length != 5) {
							// do nothing
						} else {
							fileInfo = new FileInfo(fileStr[4]);
							fileInfo.fileDate = Long.parseLong(fileStr[0]);
							fileInfo.isDir = Command.DIR_FLAG.equals(fileStr[1]);
							fileInfo.fileSize = Double.parseDouble(fileStr[2]);
							fileInfo.filePath = fileStr[3];
//							Log.d(TAG, "filePath=" + fileInfo.filePath);
							if (fileInfo.isDir) {
								folderList.add(fileInfo);
							} else {
								fileList.add(fileInfo);
							}
						}
					}
					// sort
					Collections.sort(folderList);
					Collections.sort(fileList);
					// combine
					mList.addAll(folderList);
					mList.addAll(fileList);
					Log.d(TAG, "files list nums:" + mList.size());
					// clear 0
					wholeReceiveMsg = "";

					uihandler.sendMessage(uihandler.obtainMessage(UPDATE_UI));
				}
			} else {
				// not over yet
				wholeReceiveMsg += ret_msg;
			}
		}
		
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void notifyConnectChanged() {
		// TODO Auto-generated method stub
	}
	
	private Handler uihandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATE_UI:
				//update ui
				Log.d(TAG, "update remote file ui ");
				if (mFileListDialog != null) {
					mFileListDialog.cancel();
					mFileListDialog = null;
				}
				mAdapter.notifyDataSetChanged();
				break;

			default:
				break;
			}
		};
	};

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//click to into subdirectory
		//first you need send a command to tell server i want to ls the path
		//server return all the folders and files about the path to client
		//ps:english is so so
		String msg = "";
		if (position == 0) {
			//click to back to parent path
//			mNotice.showToast("back to " + parentPath);
			msg = Command.LS + Command.AITE + parentPath;
		} else {
			//why - 1?beacuse the first position is back button.
			position = position - 1;
//			String filePath = mList.get(position).filePath;
//			mNotice.showToast(filePath);

			//tell server that the path you want into
			FileInfo fileInfo = mList.get(position);
			if (fileInfo.isDir) {
				msg = Command.LS + Command.AITE + fileInfo.filePath;
			}
		}
		Log.d(TAG, "send cmd=>" + msg);
		sendCommandMsg(msg);
	}
	
	private static final int MENU_COPY = 0x01;
	/**context menu*/
	class ListOnCreateContext implements OnCreateContextMenuListener{
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			menu.setHeaderTitle(R.string.operitor_title);
			menu.add(0, MENU_COPY, 0, "Copy");
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		int position = menuInfo.position;
		FileInfo fileInfo = null;
		switch (item.getItemId()) {
		case MENU_COPY:
			
			if (position == 0) {
				mNotice.showToast("invalid");
			}else {
				fileInfo = mList.get(position -1);
				if (fileInfo.isDir) {
					mNotice.showToast("not support dir,now(english is so so)");
				}else {
					//tell server i want this file
					currentCopyFile = fileInfo;
					copyPath = fileInfo.filePath;
					currentFileNmae = fileInfo.fileName;
					
					mActionMode = mListView.startActionMode(mActionModeCallBack);
				}
			}
			break;

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	/**
	 * Remote Action Mode Call Back
	 */
	class RemoteActionModeCallBack implements Callback{
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.file_paste:
				mActionMode.finish();
				
				//start copy
				mNotice.showToast("start copy " + currentCopyFile.fileName);
				
				try {
					File file = new File(LocalFileFragment.mCurrentPath + "/" + currentCopyFile.fileName);
					if (!file.exists()) {
						file.createNewFile();
					}
					Log.d(TAG, "Current copy file is " + file.getAbsolutePath());
					fos = new FileOutputStream(file);
				} catch (FileNotFoundException e) {
					Log.e(TAG, "file error:" + e.toString());
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(TAG, "IO error:" + e.toString());
					e.printStackTrace();
				}
				
				mFileTransferBarDialog = new ProgressBarDialog(mContext);
				mFileTransferBarDialog.setDMax(currentCopyFile.fileSize);
				mFileTransferBarDialog.setMessage("copying:" + currentCopyFile.fileName);
				mFileTransferBarDialog.setCancelable(false);
				mFileTransferBarDialog.show();
				
				start_time = System.currentTimeMillis();
				Log.d(TAG, "start copy time:"+ start_time);
				
				mSpeedTimer = new Timer();
				mSpeedTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						now_time = System.currentTimeMillis();
						 long speed = (long) ((double)copyLen / ((now_time - start_time) / 1000));
						 mFileTransferBarDialog.setSpeed(speed);
					}
				}, 1000, 1000);
				
				//send msg to server that i want this file
				String copyCmd = Command.COPY + Command.AITE + currentCopyFile.filePath;
				mCommunicationManager.sendMessage(copyCmd.getBytes(), 0);
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
