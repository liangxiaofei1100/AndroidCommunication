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
 * Զ�̷������ļ�Ŀ¼����
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
	
	//���浱ǰ·��
	private static String currentPath = "";
	//������һ��·��
	private static String parentPath = "";
	
	private static final int UPDATE_UI = 0x00;
	
	//���ڷ��͵���Ϣ�п��ܹ�����ִη��ͣ����Խ��շ���Ҫ�ȴ����з����ˣ��ٽ���
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
		
		//�ж��Ƿ����ӷ�����
		if (mCommunicationManager.getCommunications().isEmpty()) {
			mTipText.setVisibility(View.VISIBLE);
		}else {
			mTipText.setVisibility(View.GONE);
			//��ʼ����ʱ����Զ�̷���������һ��LS�����֪�г�/sdcard���ļ�
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
		//������Ŀ¼���÷�������
		//�����ļ��еľ���·��������������
		//�������յ�CD����������ļ��еľ���·�����оٳ����ļ����µ������ļ�/�ļ��У����ظ��ͻ���
		String msg = "";
		if (position == 0) {
			//��ʾ������һ��
			mNotice.showToast("������һ��" + parentPath);
			//��ε�֪��ǰ��path��
			msg = Command.LS + Command.AITE + parentPath;
		}else {
			position = position -1;
			String filePath = mList.get(position).filePath;
			mNotice.showToast(filePath);
			
			//���߷�������Ҫ����ָ����Ŀ¼
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
		
		//���зָ�
		String[] splitMsg = retMsg.split(Command.ENTER);
		
		//����������־������Ľ��������򲻽���
		if (Command.END_FLAG.equals(splitMsg[splitMsg.length -1])) {
			//������
			allMsg += retMsg;
			String[] newSplitMsg = allMsg.split(Command.ENTER);
			if (Command.LSRETN.equals(newSplitMsg[0])) {
				Log.d(TAG, "=========================");
				//ԭ����Ŀ¼�����������ô��������ÿһ�ж���ʾһ���ļ����ļ��У�
				//[LSRTN][��ǰ·��][��һ��·��][�ļ�1][�ļ�2][�ļ�3][�ļ�4][...]
				currentPath = splitMsg[1];
				parentPath = splitMsg[2];
				
				//�ļ���
				ArrayList<FileInfo> folderList = new ArrayList<FileInfo>();
				//�ļ�
				ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();
				
				FileInfo fileInfo = null;
				Log.d(TAG, "11111length=" + newSplitMsg.length);
				for (int i = 3; i < newSplitMsg.length - 1; i++) {
					//�ָ�ÿһ��
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
							//�ļ���
							folderList.add(fileInfo);
						}else {
							//�ļ�
							fileList.add(fileInfo);
						}
					}
				}
				//����
				Collections.sort(folderList);
				Collections.sort(fileList);
				//�ϲ�
				mList.addAll(folderList);
				mList.addAll(fileList);
				
				//��0
				allMsg = "";
				
				uihandler.sendMessage(uihandler.obtainMessage(UPDATE_UI));
			}
		}else {
			//��û����
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
