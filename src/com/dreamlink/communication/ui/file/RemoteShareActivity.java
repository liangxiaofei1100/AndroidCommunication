package com.dreamlink.communication.ui.file;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.Command;
import com.dreamlink.communication.ui.DreamConstant.Cmd;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.dialog.FileExistDialog;
import com.dreamlink.communication.ui.dialog.FileExistDialog.onMenuItemClickListener;
import com.dreamlink.communication.ui.dialog.FileTransferDialog;
import com.dreamlink.communication.ui.dialog.FileTransferDialog.FileTransferOnClickListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.LogFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * access remote server
 */
public class RemoteShareActivity extends Activity implements OnItemClickListener, OnClickListener, 
					OnCommunicationListenerExternal, OnItemLongClickListener{
	private static final String TAG = "RemoteShareActivity";
	
	//show file list
	private ListView mListView = null;
	private ImageButton mRootViewBtn,mUpDirBtn,mRefreshBtn;
	private RelativeLayout mListLayout;
	private LinearLayout mLoadingLayout;
	//show tip msg
	private TextView mNoConnectionTips = null;
	private Button mStopServerBtn;
	//show remote share server list
	private RelativeLayout mServerListLayout;
	private Button mCreateServerBtn, mSearchServerBtn;
	private ListView mServerListView;
	private RemoteShareAdapter mShareAdapter = null;
	private List<User> mRemoteShareList = new ArrayList<User>();
	//show remote share server list
	
	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	private Context mContext = null;
	private SocketCommunicationManager mSocketMgr = null;
	
	//File Array
	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	/**save the files that get from remote server*/
	private List<String> mFileMsgList = new ArrayList<String>();
	
	private FileListAdapter mAdapter = null;
	private FileInfoManager mFileInfoManager = null;
	
	private Notice mNotice = null;
	
	/**save current path*/
	private  String currentPath = "";
	/**save parent path*/
	private  String parentPath = "";
	
	private static final int UPDATE_UI = 0x00;
	private static final int USER_DISCONNECTED = 0x01;
	private static final int UPDATE_SERVER_LIST = 0x02;
	
	/**record the file that need copy,this file info is the file that exist in remote device*/
	public FileInfo currentCopyFile = null;
	/**the file that current copy that save in local device*/
	public File mLocalFile = null;
	public  String currentFileNmae = "";
	private FileOutputStream fos = null;
	
	private int copyLen = 0;
	
	/**file transfer progress dialog*/
	//use custom progressbar dialog
	private FileTransferDialog mFileTransferDialog = null;
	
	private long start_time = 0;
	//record time to jisuan transfer speed
	private long now_time = 0;
	//start a thread record transfer speed
	private Timer mSpeedTimer;
	
	private static final int LOCAL_REQUEST_CODE = 0x001;
	private static final int FILE_TRANSFER_REQUEST_CODE = 0x002;
	
	/**current connect user*/
	private User mCurrentConnectUser;
	
	private Handler uiHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATE_UI:
				//update ui
				Log.d(TAG, "update remote file ui ");
				mLoadingLayout.setVisibility(View.INVISIBLE);
				mAdapter.notifyDataSetChanged();
				break;
			case USER_DISCONNECTED:
				mNoConnectionTips.setVisibility(View.VISIBLE);
				mServerListLayout.setVisibility(View.INVISIBLE);
				mListLayout.setVisibility(View.INVISIBLE);
				mStopServerBtn.setVisibility(View.INVISIBLE);
				break;
			case UPDATE_SERVER_LIST:
				mShareAdapter.notifyDataSetChanged();
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
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_remote_share);
		
		mContext = this;
		mSocketMgr = SocketCommunicationManager.getInstance(mContext);
		mAppId = AppUtil.getAppID(this);
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		initTitle();
		initView();
		// is connected to server
		if (mSocketMgr.isConnected()) {
			mNoConnectionTips.setVisibility(View.VISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mServerListLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
		} else {
			mSocketMgr.registerOnCommunicationListenerExternal(this, mAppId);
			mNoConnectionTips.setVisibility(View.INVISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mServerListLayout.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			mLoadingLayout.setVisibility(View.VISIBLE);
			getShareServerList();
		}
		
		mNotice = new Notice(mContext);
		Log.d(TAG, "onCreate end");
	}
	
	private void initView() {
		mListView = (ListView) findViewById(R.id.file_listview);
		mListLayout = (RelativeLayout) findViewById(R.id.list_layout);
		mLoadingLayout = (LinearLayout) findViewById(R.id.loading_layout);
		mNoConnectionTips = (TextView) findViewById(R.id.tv_no_connection_tips);
		mStopServerBtn = (Button) findViewById(R.id.stop_server_button);
		mStopServerBtn.setOnClickListener(this);
		
		mServerListLayout = (RelativeLayout) findViewById(R.id.remote_share_server_list);
		mCreateServerBtn = (Button) findViewById(R.id.create_share);
		mCreateServerBtn.setOnClickListener(this);
		mSearchServerBtn = (Button) findViewById(R.id.search_share);
		mSearchServerBtn.setOnClickListener(this);
		mServerListView = (ListView) findViewById(R.id.server_listview);
		mServerListView.setOnItemClickListener(this);
		mShareAdapter = new RemoteShareAdapter(mContext, mRemoteShareList);
		mServerListView.setAdapter(mShareAdapter);
		
		mRootViewBtn = (ImageButton) findViewById(R.id.root_view);
		mUpDirBtn = (ImageButton) findViewById(R.id.up_view);
		mRefreshBtn = (ImageButton) findViewById(R.id.refresh_view);
		
		mRootViewBtn.setOnClickListener(this);
		mUpDirBtn.setOnClickListener(this);
		mRefreshBtn.setOnClickListener(this);
		
		mAdapter = new FileListAdapter(mContext, mList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
	}

	private void initTitle() {
		// Title icon
		mTitleIcon = (ImageView) findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.network_neighborhood_share);
		// Title text
		mTitleView = (TextView) findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.network_share);
		// Title number
		mTitleNum = (TextView) findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
	}
	
	/**
	 * send command to current connect remote share device
	 */
	public void sendMsgToSingle(String cmdMsg){
		Log.d(TAG, "sendMsgToSingle=>" + cmdMsg);
		if ("".equals(cmdMsg)) {
			return;
		}
		if (null == mCurrentConnectUser) {
			Log.e(TAG, "current connect user is null");
			mNotice.showToast("User is null");
		}else {
			mSocketMgr.sendMessageToSingle(cmdMsg.getBytes(), mCurrentConnectUser, mAppId);
		}
	}
	
	/**
	 * send command msg to all connected devices
	 */
	public void sendMsgToAll(String cmdMsg){
		Log.d(TAG, "sendMsgToAll=>" + cmdMsg);
		if ("".equals(cmdMsg)) {
			return;
		}
		mSocketMgr.sendMessageToAll(cmdMsg.getBytes(), mAppId);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (parent.getId()) {
		case R.id.server_listview:
			String cmdMsg =  Cmd.LS + Command.ROOT_PATH;
			mLoadingLayout.setVisibility(View.VISIBLE);
			mCurrentConnectUser = mRemoteShareList.get(position);
			setTitle("已连接到" + mCurrentConnectUser.getUserName());
			sendMsgToSingle(cmdMsg);
			
			mNoConnectionTips.setVisibility(View.INVISIBLE);
			mServerListLayout.setVisibility(View.INVISIBLE);
			mListLayout.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			break;
		case R.id.file_listview:
			// click to into subdirectory
			// first you need send a command to tell server i want to ls the path
			// server return all the folders and files about the path to client
			// ps:english is so so
			String msg = "";

			// tell server that the path you want into
			FileInfo fileInfo = mList.get(position);
			if (fileInfo.isDir) {
//				msg = Command.LS + Command.AITE + fileInfo.filePath;
				//use int command
				msg = Cmd.LS + fileInfo.filePath;
				mLoadingLayout.setVisibility(View.VISIBLE);
				sendMsgToSingle(msg);
			}
			break;
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		final FileInfo fileInfo = mList.get(position);
		if (!fileInfo.isDir) {
			new AlertDialog.Builder(mContext).setTitle(fileInfo.fileName)
					.setItems(R.array.fileshare_menu, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								// copy
								currentCopyFile = fileInfo;
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
		switch (v.getId()) {
		case R.id.iv_refresh:
			// tell server that i want look u sdcard files
			if (mSocketMgr.getCommunications().isEmpty()) {
				new AlertDialog.Builder(mContext)
					.setMessage("请先连接")
					.setPositiveButton(android.R.string.ok, null)
					.create().show();
			}else {
				mNoConnectionTips.setVisibility(View.INVISIBLE);
				mServerListLayout.setVisibility(View.VISIBLE);
				mListLayout.setVisibility(View.INVISIBLE);
				mStopServerBtn.setVisibility(View.INVISIBLE);
			}
			break;
			
		case R.id.create_share:
			Intent intent2 = new Intent(mContext, RemoteShareService.class);
			intent2.putExtra("app_id", mAppId);
			startService(intent2);
			
			mNoConnectionTips.setVisibility(View.INVISIBLE);
			mServerListLayout.setVisibility(View.INVISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.VISIBLE);
			break;
		case R.id.search_share:
			getShareServerList();
			break;
		case R.id.stop_server_button:
			Intent stopIntent = new Intent(mContext, RemoteShareService.class);
			stopService(stopIntent);
			
			mNoConnectionTips.setVisibility(View.INVISIBLE);
			mServerListLayout.setVisibility(View.VISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);
			break;
			
		case R.id.root_view:
//			String cmd = Command.LS + Command.AITE + Command.ROOT_PATH;
			String cmd = Cmd.LS + Command.ROOT_PATH;
			mLoadingLayout.setVisibility(View.VISIBLE);
			sendMsgToSingle(cmd);
			break;
		case R.id.up_view:
			if (!"".equals(parentPath)) {
//				String msg = Command.LS + Command.AITE + parentPath;
				String msg = Cmd.LS + parentPath;
				mLoadingLayout.setVisibility(View.VISIBLE);
				sendMsgToSingle(msg);
			}
			break;
		case R.id.refresh_view:
			if (!"".equals(currentPath)) {
//				String msg = Command.LS + Command.AITE + currentPath;
				String msg = Cmd.LS + currentPath;
				mLoadingLayout.setVisibility(View.VISIBLE);
				sendMsgToSingle(msg);
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
		Log.v(TAG, "onReceiveMessage:" + new String(msg)  + ",sendUser:" + sendUser.getUserName());
		if (null == mCurrentConnectUser) {
			// 表示还没有连接远程共享设备,这时候收到的应该是更新共享列表的消息，其他的不用理会
			int cmd = -1;
			try {
				cmd = Integer.parseInt(new String(msg).substring(0, 3));
				if (Cmd.RETURN_REMOTE_SHARE_SERVICE == cmd) {
					mRemoteShareList.add(sendUser);
					uiHandler.sendMessage(uiHandler.obtainMessage(UPDATE_SERVER_LIST));
				}
			} catch (NumberFormatException e) {
			}
			return;
		}else if (mCurrentConnectUser.getUserID() != sendUser.getUserID()) {
			//不是和我相连的设备，我不想接受你的消息，没空啊
			return;
		}
		
		if (currentCopyFile != null) {
			Log.d(TAG, "copying:" + currentCopyFile.fileName);
			//receive file from server
			try {
				copyLen += msg.length;
				if (mFileTransferDialog != null) {
					mFileTransferDialog.setDProgress(copyLen);
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
				now_time = System.currentTimeMillis();
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
				for (int i = 0; i < splitMsg.length; i++) {
					mFileMsgList.add(splitMsg[i]);
				}
				
				if( Command.LSRETN.equals(mFileMsgList.get(0))){
					//LSRETN, is ls command return msg
					//every line is a folder or file
					//command format:[LSRTN][...]
					currentPath = mFileMsgList.get(1);
					parentPath = mFileMsgList.get(2);

					//folder list
					ArrayList<FileInfo> folderList = new ArrayList<FileInfo>();
					//file list
					ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

					FileInfo fileInfo = null;
					/////use list start 20130903
					//from position 3,end to size-1,because the last line is end flag
					for (int i = 3; i < mFileMsgList.size() - 1; i++) {
						// split again
						String[] fileStr = mFileMsgList.get(i).split(Command.SEPARTOR);
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
					mFileMsgList.clear();

					uiHandler.sendMessage(uiHandler.obtainMessage(UPDATE_UI));
				}else {
					Log.e(TAG, "why i here:" + mFileMsgList.get(0));
				}
			} else {
				// not over yet
				Log.d(TAG, "not over yet");
				//将每一行拆开存放到list中
				for (int i = 0; i < splitMsg.length; i++) {
					mFileMsgList.add(splitMsg[i]);
				}
			}
		}
	}

	@Override
	public void onUserConnected(User user) throws RemoteException {
		Log.d(TAG, "onUserConnected");
	}

	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		Log.d(TAG, "onUserDisconnected");
		if (null != mFileTransferDialog) {
			mFileTransferDialog.cancel();
		}
		
		if (mSpeedTimer != null) {
			mSpeedTimer.cancel();
		}
		doClear();
		uiHandler.sendMessage(uiHandler.obtainMessage(USER_DISCONNECTED));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK == resultCode) {
			if (LOCAL_REQUEST_CODE == requestCode) {
				String copyPath = "";
				if (null != data) {
					copyPath = data.getStringExtra(Extra.COPY_PATH);
					doCopy(copyPath);
				}
			}else if (FILE_TRANSFER_REQUEST_CODE == requestCode) {
				System.out.println("******************88");
			}
		}else if (RESULT_CANCELED == resultCode) {
			currentCopyFile = null;
		}
	}
	
	/**
	 * copy remote file to local
	 * @param path
	 */
	public void doCopy(final String path){
		final String localFilePath = path + "/" + currentCopyFile.fileName;
		mLocalFile = new File(localFilePath);
		try {
			if (!mLocalFile.exists()) {
				mLocalFile.createNewFile();
				startTransferDialog(currentCopyFile.fileName);
			}else {
				final FileExistDialog dialog = new FileExistDialog(mContext, path, currentCopyFile.fileName);
				dialog.setTitle(R.string.file_exist);
				dialog.setOnMenuItemClickListener(new onMenuItemClickListener() {
					@Override
					public void onMenuItemClick(int position) {
						switch (position) {
						case 0:
							//replace and copy
							startTransferDialog(currentCopyFile.fileName);
							break;
						case 1:
							//rename and copy
							String name = dialog.getRenameStr();
							mLocalFile = new File(path + "/" + name);
							startTransferDialog(name);
							break;
						case 2:
							//cancel
							//do nothing
							currentCopyFile = null;
							break;
						default:
							break;
						}
					}
				});
				dialog.setCancelable(false);
				dialog.show();
			}
			Log.d(TAG, "Current copy file is " + mLocalFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e(TAG, "IO error:" + e.toString());
			e.printStackTrace();
		}
	}
	
	private void startTransferDialog(String name){
		try {
			fos = new FileOutputStream(mLocalFile);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "file error:" + e.toString());
			currentCopyFile = null;
			e.printStackTrace();
			return;
		}
		mFileTransferDialog = new FileTransferDialog(mContext, R.style.TransferDialog);
		mFileTransferDialog.setDMax(currentCopyFile.fileSize);
		mFileTransferDialog.setFileName(name);
		mFileTransferDialog.setState(FileTransferDialog.STATE_COPYING);
		mFileTransferDialog.setFileTransferOnClickListener(new FileTransferOnClickListener() {
			@Override
			public void onClick(View view, int state) {
				int id = view.getId();
				switch (id) {
				case R.id.left_button:
					switch (state) {
					case FileTransferDialog.STATE_COPY_OK:
						//copy ok.click this can open file
						mFileInfoManager.openFile(mLocalFile.getPath());
						break;
					case FileTransferDialog.STATE_COPYING:
						//copying,click this can stop file transfer
						String copyCmd = Cmd.STOP_SEND_FILE + currentCopyFile.filePath;
						sendMsgToSingle(copyCmd);
						if (mLocalFile.exists()) {
							mLocalFile.delete();
						}
//						mNotice.showToast("Stop");
						break;
					case FileTransferDialog.STATE_COPY_FAIL:
						//copy fail,click this can retry
						mNotice.showToast("Retry");
						break;

					default:
						break;
					}
					break;
				case R.id.right_button:
					switch (state) {
					case FileTransferDialog.STATE_COPY_OK:
						mNotice.showToast("saved to " + mLocalFile.getPath());
						break;
					case FileTransferDialog.STATE_COPYING:
						//copying ,click this hide dialog .and show in notificaion
						mNotice.showToast("Hide");
						break;
					case FileTransferDialog.STATE_COPY_FAIL:
						//copy fail ,click this 1)delete local file. and so on,and dismiss dialog
						mNotice.showToast("Cancel");
						break;
					default:
						break;
					}
					break;

				default:
					break;
				}
			}

		});
		mFileTransferDialog.setCancelable(false);
		mFileTransferDialog.show();
		
		if (currentCopyFile.fileSize == 0) {
			mFileTransferDialog.setTP(0, 0);
			mFileTransferDialog.setState(FileTransferDialog.STATE_COPY_OK);
			
			doClear();
		}else {
			start_time = System.currentTimeMillis();
			Log.d(TAG, "start copy time:"+ start_time);
			
			mSpeedTimer = new Timer();
			mSpeedTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					long duration = now_time - start_time;
					
					 double speed =  copyLen / ((double)duration / 1000);
					 mFileTransferDialog.setTP(speed, duration);
					 
					if (copyLen >= ((int) currentCopyFile.fileSize)) {
						mFileTransferDialog.setState(FileTransferDialog.STATE_COPY_OK);
						doClear();
						mSpeedTimer.cancel();
					}
				}
			}, 1000, 500);
			
			//send msg to server that i want this file
//			String copyCmd = Command.COPY + Command.AITE + currentCopyFile.filePath;
			String copyCmd = Cmd.COPY + currentCopyFile.filePath;
			sendMsgToSingle(copyCmd);
		}
	}
	
	private void getShareServerList(){
		mRemoteShareList.clear();
		String search_cmd = Cmd.GET_REMOTE_SHARE_SERVICE + "";
		sendMsgToAll(search_cmd);
	}
	
	private void doClear(){
		Log.d(TAG, "doClear<===============");
		try {
			if (null != fos) {
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			fos = null;
		}
		
		currentCopyFile = null;
		copyLen = 0;
		start_time = 0;
		now_time = 0;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.share, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (null == mCurrentConnectUser) {
			menu.findItem(R.id.menu_disconnect).setEnabled(false);
		}else {
			menu.findItem(R.id.menu_disconnect).setEnabled(true);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_disconnect:
			//断开连接
			mCurrentConnectUser = null;
			
			setTitle("未连接");
			
			mNoConnectionTips.setVisibility(View.INVISIBLE);
			mListLayout.setVisibility(View.INVISIBLE);
			mServerListLayout.setVisibility(View.VISIBLE);
			mStopServerBtn.setVisibility(View.INVISIBLE);

			mLoadingLayout.setVisibility(View.VISIBLE);

			getShareServerList();
			break;

		default:
			break;
		}
		return true;
	}
	
	private void updateUI(int status){
		mNoConnectionTips.setVisibility(View.INVISIBLE);
		mServerListLayout.setVisibility(View.INVISIBLE);
		mListLayout.setVisibility(View.INVISIBLE);
		mStopServerBtn.setVisibility(View.VISIBLE);
	}
}
