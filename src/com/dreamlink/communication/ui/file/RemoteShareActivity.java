package com.dreamlink.communication.ui.file;


import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.fileshare.Command;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.fileshare.FileListAdapter;
import com.dreamlink.communication.fileshare.ProgressBarDialog;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * access remote server
 */
public class RemoteShareActivity extends Activity implements OnCommunicationListener, OnItemClickListener{
	private static final String TAG = "RemoteShareActivity";
	
	//show file list
	private ListView mListView = null;
	//show tip msg
	private TextView mTipView = null;
	private LinearLayout mUnconnectLayout = null;
	private Button mAccessBtn;
	
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
	
//	private ActionMode mActionMode = null;
//	private RemoteActionModeCallBack mActionModeCallBack = new RemoteActionModeCallBack();
	
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
	//record time to jisuan transfer speed
	private long now_time = 0;
	//start a thread record transfer speed
	private Timer mSpeedTimer;
	
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
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.filelist_2);
		
		mContext = this;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mCommunicationManager.registered(this);
		
		mListView = (ListView) findViewById(R.id.file_listview);
//		mTipView = (TextView) findViewById(R.id.tip_text);
		mUnconnectLayout = (LinearLayout) findViewById(R.id.unconnect_layout);
		mAccessBtn = (Button) findViewById(R.id.access_button);
		mAccessBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// tell server that i want look u sdcard files
				if (mCommunicationManager.getCommunications().isEmpty()) {
					new AlertDialog.Builder(mContext)
						.setMessage("请先连接")
						.setPositiveButton(android.R.string.ok, null)
						.create().show();
				}else {
					String cmdMsg = Command.LS + Command.AITE + Command.ROOT_PATH;
					sendCommandMsg(cmdMsg);
					
					mUnconnectLayout.setVisibility(View.INVISIBLE);
					mListView.setVisibility(View.VISIBLE);
				}
			}
		});
		
		// is connected to server
		if (mCommunicationManager.getCommunications().isEmpty()) {
//			mTipView.setVisibility(View.VISIBLE);
			mUnconnectLayout.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
		} else {
//			mTipView.setVisibility(View.GONE);
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mListView.setVisibility(View.VISIBLE);

			if (mFileListDialog == null) {
				mFileListDialog = new ProgressDialog(mContext);
				mFileListDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mFileListDialog.setIndeterminate(true);
				mFileListDialog.setCancelable(true);
				mFileListDialog.show();
			}

			// tell server that i want look u sdcard files
			String cmdMsg = Command.LS + Command.AITE + Command.ROOT_PATH;
			sendCommandMsg(cmdMsg);
		}
		
		mAdapter = new FileListAdapter(mContext, mList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
//		mListView.setOnCreateContextMenuListener(new ListOnCreateContext());
		
		mNotice = new Notice(mContext);
		Log.d(TAG, "onCreate end");
	}
	
	/**
	 * send command to remote connected server
	 * @param cmd command
	 * @param path file path
	 */
	public void sendCommandMsg(String cmdMsg){
		if ("".equals(cmdMsg)) {
			return;
		}
		// TODO need to implement appID.
		mCommunicationManager.sendMessageToAll(cmdMsg.getBytes(), 0);
	}
	
	private static String wholeReceiveMsg = "";

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication communication) {
		// TODO Auto-generated method stub
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
				if (copyLen >= ((int)currentCopyFile.fileSize)) {
					//********clear begin***********//
					fos.close();
					fos = null;
					
					currentCopyFile = null;
					copyLen = 0;
					
					start_time = 0;
					now_time = 0;
					if (mSpeedTimer != null) {
						mSpeedTimer.cancel();
						mSpeedTimer = null;
					}
					
					if (mFileTransferBarDialog != null) {
						mFileTransferBarDialog.cancel();
					}
					Log.d(TAG, "Transfer Success!");
					//********clear end***********//
				}
				
			} catch (Exception e) {
				Log.e(TAG, "Receive error:" + e.toString());
				e.printStackTrace();
			}
		}else {
			//receive file list return msg
			// convert byte[] to String
			String ret_msg = new String(msg);

			// split msg
			//the first line is command flag
			//the last line is end flag
			String[] splitMsg = ret_msg.split(Command.ENTER);
			
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
					// Clear File array list
					mList.clear();
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		//click to into subdirectory
		//first you need send a command to tell server i want to ls the path
		//server return all the folders and files about the path to client
		//ps:english is so so
		String msg = "";
		if (position == 0) {
			//click to back to parent path
//			mNotice.showToast("back to " + parentPath);
			if (!"".equals(parentPath)) {
				msg = Command.LS + Command.AITE + parentPath;
			}
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
	
}
