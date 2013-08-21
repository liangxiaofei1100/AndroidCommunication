package com.dreamlink.communication.ui.file;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.fileshare.Command;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.fileshare.FileListAdapter;
import com.dreamlink.communication.fileshare.FileShareServerService;
import com.dreamlink.communication.fileshare.ProgressBarDialog;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.util.AppUtil;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.LogFile;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

/**
 * access remote server
 */
public class RemoteShareActivity extends Activity implements OnItemClickListener, OnClickListener, OnCommunicationListenerExternal, OnItemLongClickListener{
	private static final String TAG = "RemoteShareActivity";
	
	//show file list
	private ListView mListView = null;
	private ImageButton mRootViewBtn,mUpDirBtn,mRefreshBtn;
	private RelativeLayout mListLayout;
	private LinearLayout mLoadingLayout;
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
	private static final int USER_DISCONNECTED = 0x01;
	
	/**record the file that need copy,this file info is the file that exist in remote device*/
	public FileInfo currentCopyFile = null;
	/**the file that current copy that save in local device*/
	public File mCurrentLocalFile = null;
	public  String currentFileNmae = "";
	private FileOutputStream fos = null;
	
	private int copyLen = 0;
	
	/**file transfer progress dialog*/
	//use custom progressbar dialog
	private ProgressBarDialog mFileTransferBarDialog = null;
	
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
				mLoadingLayout.setVisibility(View.INVISIBLE);
				mAdapter.notifyDataSetChanged();
				break;
			case USER_DISCONNECTED:
				mUnconnectLayout.setVisibility(View.VISIBLE);
				mServerOrClientLayout.setVisibility(View.INVISIBLE);
				mListLayout.setVisibility(View.INVISIBLE);
				mStopServerBtn.setVisibility(View.INVISIBLE);
				break;

			default:
				break;
			}
		};
	};
	
	private int mAppId=0;
	LogFile logFile = null;
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_remote_share);
		
		mContext = this;
		mSocketMgr = SocketCommunicationManager
				.getInstance(mContext);
		mAppId = AppUtil.getAppID(getParent());
		
		logFile = new LogFile(mContext, "log_client.txt");
		logFile.open();
		
		mListView = (ListView) findViewById(R.id.file_listview);
		mListLayout = (RelativeLayout) findViewById(R.id.list_layout);
		mLoadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
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
		
		mRootViewBtn = (ImageButton) findViewById(R.id.root_view);
		mUpDirBtn = (ImageButton) findViewById(R.id.up_view);
		mRefreshBtn = (ImageButton) findViewById(R.id.refresh_view);
		
		mRootViewBtn.setOnClickListener(this);
		mUpDirBtn.setOnClickListener(this);
		mRefreshBtn.setOnClickListener(this);
		
		// is connected to server
		if (mSocketMgr.getCommunications().isEmpty()) {
			mUnconnectLayout.setVisibility(View.VISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
		} else {
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);

			mLoadingLayout.setVisibility(View.VISIBLE);

			// tell server that i want look u sdcard files
			String cmdMsg = Command.LS + Command.AITE + Command.ROOT_PATH;
			sendCommandMsg(cmdMsg);
		}
		
		mAdapter = new FileListAdapter(mContext, mList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		
		mNotice = new Notice(mContext);
		Log.d(TAG, "onCreate end");
	}
	
	/**
	 * send command to remote connected server
	 * @param cmd command
	 * @param path file path
	 */
	public void sendCommandMsg(String cmdMsg){
		Log.d(TAG, "sendCommandMsg=>" + cmdMsg);
		if ("".equals(cmdMsg)) {
			return;
		}
		// TODO need to implement appID.
		mSocketMgr.sendMessageToAll(cmdMsg.getBytes(), mAppId);
	}
	
	private static String wholeReceiveMsg = "";


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// click to into subdirectory
		// first you need send a command to tell server i want to ls the path
		// server return all the folders and files about the path to client
		// ps:english is so so
		String msg = "";

		// tell server that the path you want into
		FileInfo fileInfo = mList.get(position);
		if (fileInfo.isDir) {
			msg = Command.LS + Command.AITE + fileInfo.filePath;
			mLoadingLayout.setVisibility(View.VISIBLE);
			sendCommandMsg(msg);
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		FileInfo fileInfo = mList.get(position);
		if (!fileInfo.isDir) {
			currentCopyFile = mList.get(position);
			new AlertDialog.Builder(mContext).setTitle(currentCopyFile.fileName)
					.setItems(R.array.fileshare_menu, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								// copy
								Intent intent = new Intent(mContext, LocalFolderDialog.class);
								startActivityForResult(intent, LOCAL_REQUEST_CODE);
								break;

							default:
								break;
							}
						}
					}).create().show();
		}else {
			//now we do not support copy folder
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
				mListLayout.setVisibility(View.INVISIBLE);
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
			mListLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.VISIBLE);
			break;
		case R.id.client_button:
			mSocketMgr.registerOnCommunicationListenerExternal(this, mAppId);
			
			String cmdMsg = Command.LS + Command.AITE + Command.ROOT_PATH;
			mLoadingLayout.setVisibility(View.VISIBLE);
			sendCommandMsg(cmdMsg);
			
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.INVISIBLE);
			mListLayout.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			break;
		case R.id.stop_server_button:
			Intent stopIntent = new Intent(mContext, FileShareServerService.class);
			stopService(stopIntent);
			
			mUnconnectLayout.setVisibility(View.INVISIBLE);
			mServerOrClientLayout.setVisibility(View.VISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			break;
			
		case R.id.root_view:
			String cmd = Command.LS + Command.AITE + Command.ROOT_PATH;
			mLoadingLayout.setVisibility(View.VISIBLE);
			sendCommandMsg(cmd);
			break;
		case R.id.up_view:
			if (!"".equals(parentPath)) {
				String msg = Command.LS + Command.AITE + parentPath;
				mLoadingLayout.setVisibility(View.VISIBLE);
				sendCommandMsg(msg);
			}
			break;
		case R.id.refresh_view:
			if (!"".equals(currentPath)) {
				String msg = Command.LS + Command.AITE + currentPath;
				mLoadingLayout.setVisibility(View.VISIBLE);
				sendCommandMsg(msg);
			}
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
		Log.d(TAG, "onReceiveMessage:" + msg.length);
		
		if (currentCopyFile != null) {
			Log.d(TAG, "copying:" + currentCopyFile.fileName);
			//receive file from server
			try {
				copyLen += msg.length;
				if (mFileTransferBarDialog != null) {
					mFileTransferBarDialog.setDProgress(copyLen);
				}else {
					Log.e(TAG, "mProgressDialog is null");
				}

//				Log.d(TAG, "copyLen=" + copyLen);
//				Log.d(TAG, "totalSize=" + (int)currentCopyFile.fileSize);
//				System.out.println(copyLen + "/" + currentCopyFile.fileSize);
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
					if (fos != null) {
						fos.close();
						fos = null;
					}
					
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
			/*
			 * [第一行]表示显示文件列表返回标识:lsretn
			 * [第二行]表示当前目录的绝对路径:如,/mnt/sdcard,/mnt/sdcard/Android/data
			 * [第三行]表示当前目录的父目录,如果当前目录已是sdcard的根目录，则设定父目录为空
			 * [第四行...第N行]表示当前目录中的内容，或文件夹，或文件，每一行表示一个文件夹，或文件
			 * 文件夹的格式规则：[最后修改时间],[文件夹标识<DIR],[文件夹大小(0)],文件夹名称]
			 * 文件的格式规则：[最后修改时间],[文件标识],[文件大小],[文件名称]
			 * 所以解析的时候，获得一个文件或文件夹的绝对路径，可以用[第二行]+文件名称/文件夹名称
			 * [最后一行]结束表示符，只有读到改行，即表示当前目录的所有文件信息已经传输完毕，可以显示并更新UI了
			 * */
			//receive file list return msg
			// convert byte[] to String
			logFile.writeLog(msg);
//			logFile.writeLog("\n=============\n");
			String ret_msg = new String(msg);
//			logFile.writeLog(ret_msg);
			
			Log.i(TAG, "onReceiveMessage.ret_msg" + ret_msg);

			// split msg
			//the first line is command flag
			//the last line is end flag
			String[] splitMsg = ret_msg.split(Command.ENTER);
			Log.d(TAG, "splitMsg[splitMsg.length - 1])=" +splitMsg[splitMsg.length - 1]);
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
						if (fileStr.length != 4) {
							// do nothing
						} else {
							fileInfo = new FileInfo(fileStr[3]);
							fileInfo.fileDate = Long.parseLong(fileStr[0]);
							fileInfo.isDir = Command.DIR_FLAG.equals(fileStr[1]);
							fileInfo.fileSize = Double.parseDouble(fileStr[2]);
//							fileInfo.filePath = fileStr[3];
							fileInfo.filePath = currentPath + "/" + fileStr[3];
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
				Log.d(TAG, "not over yet");
				wholeReceiveMsg += ret_msg;
			}
		}
	}

	@Override
	public void onUserConnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		Log.d(TAG, "onUserConnected");
	}

	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		Log.d(TAG, "onUserDisconnected");
		if (null != mFileTransferBarDialog) {
			mFileTransferBarDialog.cancel();
		}
		
		currentCopyFile = null;
		
		uihandler.sendMessage(uihandler.obtainMessage(USER_DISCONNECTED));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK == resultCode) {
			String copyPath = "";
			if (null != data) {
				copyPath = data.getStringExtra(Extra.COPY_PATH);
				doCopy(copyPath);
			}
		}else if (RESULT_CANCELED == resultCode) {
			currentCopyFile = null;
		}
	}
	
	public void doCopy(String path){
		mCurrentLocalFile = new File(path + "/" + currentCopyFile.fileName);
		try {
			if (!mCurrentLocalFile.exists()) {
				mCurrentLocalFile.createNewFile();
			}
			Log.d(TAG, "Current copy file is " + mCurrentLocalFile.getAbsolutePath());
			fos = new FileOutputStream(mCurrentLocalFile);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "file error:" + e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "IO error:" + e.toString());
			e.printStackTrace();
		}
		
		mFileTransferBarDialog = new ProgressBarDialog(mContext);
		mFileTransferBarDialog.setDMax(currentCopyFile.fileSize);
		mFileTransferBarDialog.setMyTitle("copying:" + currentCopyFile.fileName);
		mFileTransferBarDialog.setTime(0);
		mFileTransferBarDialog.setCancelable(true);
//		mFileTransferBarDialog.setButton(Dialog.BUTTON_NEGATIVE, "Stop", new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				
//				sendCommandMsg(Command.STOP_SEND_FILE + Command.AITE);
////				if (copyLen < currentCopyFile.fileSize) {
//////					file.delete();
////					if (null != mCurrentLocalFile) {
////						mCurrentLocalFile.delete();
////						mCurrentLocalFile = null;
////					}
////				}
////				currentCopyFile = null;
//				mFileTransferBarDialog.setTitle("Pausing:" + currentCopyFile.fileName);
////				DreamUtil.setDialogDismiss(dialog, false);
//				try {
//					Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
//					field.setAccessible(true);
//					field.set(dialog, false);
//					dialog.dismiss();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				
//			}
//		});
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
					long duration = now_time - start_time;
					 long speed = (long) ((double)copyLen / (duration / 1000));
					 mFileTransferBarDialog.setSpeed(speed);
					 mFileTransferBarDialog.setTime(duration);
				}
			}, 1000, 1000);
			
			//send msg to server that i want this file
			String copyCmd = Command.COPY + Command.AITE + currentCopyFile.filePath;
			sendCommandMsg(copyCmd);
		}
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
		sendBroadcast(intent);
	}
	
}
