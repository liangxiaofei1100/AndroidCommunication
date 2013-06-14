package com.dreamlink.communication.fileshare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.fileshare.Command;
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
 * 远程服务器文件目录界面
 * @author Yuri
 */
public class FileListActivity extends ListActivity implements OnItemClickListener, OnCommunicationListener, OnItemLongClickListener{
	private static final String TAG = "FileListActivity";
	
	private FileListAdapter mAdapter = null;
	
	private ListView mListView = null;
	
	private ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	
	private Context mContext = null;
	private SocketCommunicationManager mCommunicationManager = null;
	
	private static final String ROOT_PATH = "ROOT";
	
	private TextView mTipText;
	private Notice mNotice = null;
	
	//保存当前路径
	private static String currentPath = "";
	//保存上一级路径
	private static String parentPath = "";
	
	private static final int UPDATE_UI = 0x00;
	
	//由于发送的消息有可能过大而分次发送，所以接收方需要等待所有发完了，再解析
	private static String allMsg = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.filelist);
		
		mContext = this;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext,true);
		mCommunicationManager.registered(this);
		
		mTipText = (TextView) findViewById(R.id.tip_text);
		mListView = getListView();
		
		//判断是否连接服务器
		if (mCommunicationManager.getCommunications().isEmpty()) {
			mTipText.setVisibility(View.VISIBLE);
		}else {
			mTipText.setVisibility(View.GONE);
			//初始化的时候，向远程服务器发送一个LS命令，告知列出/sdcard的文件
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
	 * @param cmd command
	 * @param path file path
	 */
	public void sendCommandMsg(String cmd, String path){
		String accessMsg = cmd + Command.AITE + path;
		mCommunicationManager.sendMessage(accessMsg.getBytes(), 0);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//进入子目录，得分两步骤
		//将该文件夹的绝对路径发给服务器，
		//服务器收到CD命令，并根据文件夹的绝对路径，列举出该文件夹下的所有文件/文件夹，返回给客户端
		String msg = "";
		if (position == 0) {
			//表示返回上一级
			mNotice.showToast("返回上一级" + parentPath);
			//如何得知当前的path？
			msg = Command.LS + Command.AITE + parentPath;
		}else {
			position = position -1;
			String filePath = mList.get(position).filePath;
			mNotice.showToast(filePath);
			
			//告诉服务器我要进入指定的目录
			FileInfo fileInfo = mList.get(position);
			if (fileInfo.isDir) {
				msg = Command.LS + Command.AITE + fileInfo.filePath;
			}
		}
		mCommunicationManager.sendMessage(msg.getBytes(),0);
	}
	
	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		Log.d(TAG, "onReceiveMessage");
		mList.clear();
		String retMsg = new String(msg);
		
		//按行分割
		String[] splitMsg = retMsg.split(Command.ENTER);
		
		//看到结束标志，才真的结束，否则不解析
		if (Command.END_FLAG.equals(splitMsg[splitMsg.length -1])) {
			//结束了
			allMsg += retMsg;
			String[] newSplitMsg = allMsg.split(Command.ENTER);
			if (Command.LSRETN.equals(newSplitMsg[0])) {
				Log.d(TAG, "=========================");
				//原来是目录浏览反馈，那么接下来的每一行都表示一个文件（文件夹）
				//[LSRTN][当前路径][上一级路径][文件1][文件2][文件3][文件4][...]
				currentPath = splitMsg[1];
				parentPath = splitMsg[2];
				
				//文件夹
				ArrayList<FileInfo> folderList = new ArrayList<FileInfo>();
				//文件
				ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
				
				FileInfo fileInfo = null;
				Log.d(TAG, "11111length=" + newSplitMsg.length);
				for (int i = 3; i < newSplitMsg.length - 1; i++) {
					//分割每一行
					Log.d(TAG, "i=" + i);
					String[] fileStr = newSplitMsg[i].split(Command.SEPARTOR);
					if (fileStr.length != 5) {
						//do nothing
						Log.e(TAG, "lenght=" + fileStr.length);
					}else {
						fileInfo = new FileInfo(fileStr[4]);
						fileInfo.fileDate = Long.parseLong(fileStr[0]);
						fileInfo.isDir = Command.DIR_FLAG.equals(fileStr[1]);
						fileInfo.fileSize = Double.parseDouble(fileStr[2]);
						fileInfo.filePath = fileStr[3];
						Log.d(TAG, "filePaht=" + fileInfo.filePath);
						if (fileInfo.isDir) {
							//文件夹
							folderList.add(fileInfo);
						}else {
							//文件
							fileList.add(fileInfo);
						}
					}
				}
				//排序
				Collections.sort(folderList);
				Collections.sort(fileList);
				//合并
				mList.addAll(folderList);
				mList.addAll(fileList);
				
				//请0
				allMsg = "";
				
				uihandler.sendMessage(uihandler.obtainMessage(UPDATE_UI));
			}
		}else {
			//还没结束
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
	
	private Handler uihandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case UPDATE_UI:
				//update ui
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
		//Debug
		mNotice.showToast("currentPath=" + currentPath + "\n"
				+ "parentPaht=" + parentPath);
		return false;
	}
}
