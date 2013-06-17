package com.dreamlink.communication.fileshare;

import java.util.ArrayList;
import java.util.Collections;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.Notice;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

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
	
	//保存当前路径
	private  String currentPath = "";
	//保存上一级路径
	private  String parentPath = "";
	
	private static final int UPDATE_UI = 0x00;
	
	//由于发送的消息有可能过大而分次发送，所以接收方需要等待所有发完了，再解析
	private static String allMsg = "";
	
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
		
		// 判断是否连接服务器
		if (mCommunicationManager.getCommunications().isEmpty()) {
			mTipView.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
		} else {
			mTipView.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
			//初始化的时候，向远程服务器发送一个LS命令，告知列出/sdcard的文件
			sendCommandMsg(Command.LS, Command.ROOT_PATH);
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
	public void sendCommandMsg(String cmd, String path){
		String accessMsg = cmd + Command.AITE + path;
		mCommunicationManager.sendMessage(accessMsg.getBytes(), 0);
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		Log.d(TAG, "onReceiveMessage");
		// Clear File array list
		mList.clear();

		// convert byte[] to String
		String ret_msg = new String(msg);

		// 按行分割
		// 第一行为命令标志，最后一行为结束标志־
		String[] splitMsg = ret_msg.split(Command.ENTER);

		// 看到结束标志，才真的结束，否则不解析
		if (Command.END_FLAG.equals(splitMsg[splitMsg.length - 1])) {
			// 结束了
			allMsg += ret_msg;
			String[] newSplitMsg = allMsg.split(Command.ENTER);
			if (Command.LSRETN.equals(newSplitMsg[0])) {
				Log.d(TAG, "=========================");
				// 原来是目录浏览反馈，那么接下来的每一行都表示一个文件（文件夹）
				// [LSRTN][当前路径][上一级路径][文件1][文件2][文件3][文件4][...]
				currentPath = splitMsg[1];
				parentPath = splitMsg[2];

				//folder list
				ArrayList<FileInfo> folderList = new ArrayList<FileInfo>();
				//file list
				ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

				FileInfo fileInfo = null;
				Log.d(TAG, "11111length=" + newSplitMsg.length);
				for (int i = 3; i < newSplitMsg.length - 1; i++) {
					// 分割每一行
					String[] fileStr = newSplitMsg[i].split(Command.SEPARTOR);
					if (fileStr.length != 5) {
						// do nothing
					} else {
						fileInfo = new FileInfo(fileStr[4]);
						fileInfo.fileDate = Long.parseLong(fileStr[0]);
						fileInfo.isDir = Command.DIR_FLAG.equals(fileStr[1]);
						fileInfo.fileSize = Double.parseDouble(fileStr[2]);
						fileInfo.filePath = fileStr[3];
						Log.d(TAG, "filePath=" + fileInfo.filePath);
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
				// 合并
				mList.addAll(folderList);
				mList.addAll(fileList);

				// clear 0
				allMsg = "";

				uihandler.sendMessage(uihandler.obtainMessage(UPDATE_UI));
			}
		} else {
			// 还没结束
			allMsg += ret_msg;
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
				mAdapter.notifyDataSetChanged();
				break;

			default:
				break;
			}
		};
	};

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// 进入子目录，得分两步骤
		// 将该文件夹的绝对路径发给服务器，
		// 服务器收到CD命令，并根据文件夹的绝对路径，列举出该文件夹下的所有文件/文件夹，返回给客户端
		String msg = "";
		if (position == 0) {
			// 表示返回上一级
			mNotice.showToast("back to " + parentPath);
			msg = Command.LS + Command.AITE + parentPath;
		} else {
			position = position - 1;
			String filePath = mList.get(position).filePath;
			mNotice.showToast(filePath);

			// 告诉服务器我要进入指定的目录
			FileInfo fileInfo = mList.get(position);
			if (fileInfo.isDir) {
				msg = Command.LS + Command.AITE + fileInfo.filePath;
			}
		}
		mCommunicationManager.sendMessage(msg.getBytes(), 0);
	}
	
	class ListOnCreateContext implements OnCreateContextMenuListener{

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			// TODO Auto-generated method stub
			menu.setHeaderTitle("OP");
			menu.add(0, 1, 0, "Copy");
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case 1:
			mNotice.showToast("context menu-->" + menuInfo.position);
			mListView.startActionMode(new ActionModeCallBack(mContext));
			break;

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}
}
