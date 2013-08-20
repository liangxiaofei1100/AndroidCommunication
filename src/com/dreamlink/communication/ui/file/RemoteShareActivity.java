package com.dreamlink.communication.ui.file;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.fileshare.Command;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.fileshare.FileListAdapter;
import com.dreamlink.communication.fileshare.FileShareServerService;
import com.dreamlink.communication.fileshare.LocalFileFragment;
import com.dreamlink.communication.fileshare.ProgressBarDialog;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.util.AppUtil;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * access remote server
 */
public class RemoteShareActivity extends Activity implements OnCommunicationListener, OnItemClickListener, OnClickListener, OnCommunicationListenerExternal, OnItemLongClickListener{
	private static final String TAG = "RemoteShareActivity";
	
	//show file list
	private ListView mListView = null;
	//show tip msg
	private LinearLayout mUnconnectLayout = null;
	private LinearLayout mServerOrClientLayout = null;
	private Button mAccessBtn;
	private Button mServerBtn;
	private Button mClientBtn;
	private Button mStopServerBtn;
	
	private Context mContext = null;
	private SocketCommunicationManager mSocketMgr = null;
	
	//File Array
	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	
	private FileListAdapter mAdapter = null;
	
	private Notice mNotice = null;
	
	//save current path
	private  String currentPath = "";
	//save parent path
	private  String parentPath = "";
	
	private static final int UPDATE_UI = 0x00;
	
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
	
	private static final int LOCAL_REQUEST_CODE = 0x001;
	
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
	
	private int mAppId=0;
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_remote_share);
		
		mContext = this;
		mSocketMgr = SocketCommunicationManager
				.getInstance(mContext);
//		mSocketMgr.registered(this);
		mAppId = AppUtil.getAppID(getParent());
		
		mListView = (ListView) findViewById(R.id.file_listview);
		mUnconnectLayout = (LinearLayout) findViewById(R.id.unconnect_layout);
		mServerOrClientLayout = (LinearLayout) findViewById(R.id.server_or_client);
		mAccessBtn = (Button) findViewById(R.id.access_button);
		mServerBtn = (Button) findViewById(R.id.server_button);
		mClientBtn = (Button) findViewById(R.id.client_button);
		mStopServerBtn = (Button) findViewById(R.id.stop_server_button);
		mServerBtn.setOnClickListener(this);
		mClientBtn.setOnClickListener(this);
		mAccessBtn.setOnClickListener(this);
		mStopServerBtn.setOnClickListener(this);
		
		// is connected to server
		if (mSocketMgr.getCommunications().isEmpty()) {
			mUnconnectLayout.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
		} else {
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mListView.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);

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
		mListView.setOnItemLongClickListener(this);
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
		mSocketMgr.sendMessageToAll(cmdMsg.getBytes(), mAppId);
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
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		if (position == 0) {
			mSocketMgr.unregisterOnCommunicationListenerExternal(this);
			
			mUnconnectLayout.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
		}else {
			currentCopyFile = mList.get(position - 1);
			new AlertDialog.Builder(mContext)
			.setTitle(currentCopyFile.fileName)
			.setItems(R.array.fileshare_menu, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					switch (which) {
					case 0:
						//copy
						Intent intent = new Intent(mContext, LocalFolderDialog.class);
						startActivityForResult(intent, LOCAL_REQUEST_CODE);
						break;

					default:
						break;
					}
				}
			}).create().show();
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.access_button:
			// tell server that i want look u sdcard files
			if (mSocketMgr.getCommunications().isEmpty()) {
				new AlertDialog.Builder(mContext)
					.setMessage("请先连接")
					.setPositiveButton(android.R.string.ok, null)
					.create().show();
			}else {
				mUnconnectLayout.setVisibility(View.INVISIBLE);
				mServerOrClientLayout.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.INVISIBLE);
				mStopServerBtn.setVisibility(View.INVISIBLE);
			}
			break;
			
		case R.id.server_button:
			Intent intent = new Intent(mContext, FileShareServerService.class);
			intent.putExtra("app_id", mAppId);
			startService(intent);
//			Intent intent = new Intent(mContext, FileShareServer.class);
//			intent.putExtra("app_id", mAppId);
//			startActivity(intent);
			
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.INVISIBLE);
			mListView.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.VISIBLE);
			break;
		case R.id.client_button:
			mSocketMgr.registerOnCommunicationListenerExternal(this, mAppId);
			
			String cmdMsg = Command.LS + Command.AITE + Command.ROOT_PATH;
			sendCommandMsg(cmdMsg);
			
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.INVISIBLE);
			mListView.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			break;
		case R.id.stop_server_button:
			Intent stopIntent = new Intent(mContext, FileShareServerService.class);
			stopService(stopIntent);
			
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			break;

		default:
			break;
		}
	}

	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) throws RemoteException {
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
	public void onUserConnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK == resultCode) {
			String copyPath = "";
			if (null != data) {
				copyPath = data.getStringExtra(Extra.COPY_PATH);
				doCopy(copyPath);
			}
		}
	}
	
	public void doCopy(String path){
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
		mFileTransferBarDialog.setCancelable(true);
		mFileTransferBarDialog.show();
		
		if (currentCopyFile.fileSize == 0) {
			mFileTransferBarDialog.cancel();
			mNotice.showToast("Transfer Success!");
		}else {
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
			// TODO need to implement appID.
			mSocketMgr.sendMessageToAll(copyCmd.getBytes(), mAppId);
		}
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
		sendBroadcast(intent);
	}
	
}
