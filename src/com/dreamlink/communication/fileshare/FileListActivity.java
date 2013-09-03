package com.dreamlink.communication.fileshare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.ui.Command;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.ui.file.FileListAdapter;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 * list remote files
 * 
 * @author Yuri
 * @deprecated use FileMainUI instead
 */
public class FileListActivity extends ListActivity implements
		OnItemClickListener, OnCommunicationListener, OnItemLongClickListener {
	private static final String TAG = "FileListActivity";

	private FileListAdapter mAdapter = null;

	private ListView mListView = null;

	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();

	private Context mContext = null;
	private SocketCommunicationManager mCommunicationManager = null;

	private static final String ROOT_PATH = "ROOT";

	private TextView mTipText;
	private Notice mNotice = null;

	// save curennt path
	private static String currentPath = "";
	// save parent path
	private static String parentPath = "";

	private static final int UPDATE_UI = 0x00;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.filelist);

		mContext = this;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mCommunicationManager.registered(this);

		mTipText = (TextView) findViewById(R.id.tip_text);
		mListView = getListView();

		// is connect server?
		if (mCommunicationManager.getCommunications().isEmpty()) {
			mTipText.setVisibility(View.VISIBLE);
		} else {
			mTipText.setVisibility(View.GONE);
			// tell server i want look your root path
			sendCommandMsg(Command.LS, Command.ROOT_PATH);
		}

		mAdapter = new FileListAdapter(FileListActivity.this, mList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);

		mNotice = new Notice(mContext);

	}

	/**
	 * send command to remote connected server
	 * 
	 * @param cmd
	 *            command
	 * @param path
	 *            file path
	 */
	public void sendCommandMsg(String cmd, String path) {
		String accessMsg = cmd + Command.AITE + path;
		// TODO need to implement appID.
		mCommunicationManager.sendMessageToAll(accessMsg.getBytes(), 0);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// click to into subdirectory
		// first you need send a command to tell server i want to ls the path
		// server return all the folders and files about the path to client
		// ps:english is so so
		String msg = "";
		if (position == 0) {
			// click to back to parent path
			mNotice.showToast("back to " + parentPath);
			msg = Command.LS + Command.AITE + parentPath;
		} else {
			position = position - 1;
			String filePath = mList.get(position).filePath;
			mNotice.showToast(filePath);

			// tell server that the path you want into
			FileInfo fileInfo = mList.get(position);
			if (fileInfo.isDir) {
				msg = Command.LS + Command.AITE + fileInfo.filePath;
			}
		}
		// TODO need to implement appID.
		mCommunicationManager.sendMessageToAll(msg.getBytes(), 0);
	}

	private static String allMsg = "";

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		Log.d(TAG, "onReceiveMessage");
		mList.clear();
		String retMsg = new String(msg);

		// split message
		String[] splitMsg = retMsg.split(Command.ENTER);

		// 鐪嬪埌缁撴潫鏍囧織锛屾墠鐪熺殑缁撴潫锛屽惁鍒欑户缁�杩欏彞鐢ㄨ嫳鏂囩炕璇戜笉鏉�..)
		if (Command.END_FLAG.equals(splitMsg[splitMsg.length - 1])) {
			allMsg += retMsg;
			String[] newSplitMsg = allMsg.split(Command.ENTER);
			if (Command.LSRETN.equals(newSplitMsg[0])) {
				// Log.d(TAG, "=========================");
				// LSRETN, is ls command return msg
				// every line is a folder or file
				// command format:[LSRTN][]
				currentPath = splitMsg[1];
				parentPath = splitMsg[2];

				// folder list
				ArrayList<FileInfo> folderList = new ArrayList<FileInfo>();
				// file list
				ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

				FileInfo fileInfo = null;
				Log.d(TAG, "11111length=" + newSplitMsg.length);
				for (int i = 3; i < newSplitMsg.length - 1; i++) {
					String[] fileStr = newSplitMsg[i].split(Command.SEPARTOR);
					if (fileStr.length != 5) {
						// do nothing
						Log.e(TAG, "lenght=" + fileStr.length);
					} else {
						fileInfo = new FileInfo(fileStr[4]);
						fileInfo.fileDate = Long.parseLong(fileStr[0]);
						fileInfo.isDir = Command.DIR_FLAG.equals(fileStr[1]);
						fileInfo.fileSize = Double.parseDouble(fileStr[2]);
						fileInfo.filePath = fileStr[3];
						Log.d(TAG, "filePaht=" + fileInfo.filePath);
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

				// clear
				allMsg = "";

				uihandler.sendMessage(uihandler.obtainMessage(UPDATE_UI));
			}
		} else {
			// not receive over
			allMsg += retMsg;
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

	private Handler uihandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATE_UI:
				// update ui
				Log.d(TAG, "&&&&&&&&&&&&&&&&&&&&&&&&");
				mAdapter.notifyDataSetChanged();
				break;

			default:
				break;
			}
		};
	};

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		// Debug
		mNotice.showToast("currentPath=" + currentPath + "\n" + "parentPaht="
				+ parentPath);
		return false;
	}
}
