package com.dreamlink.communication.server;

import java.io.File;
import java.net.Socket;
import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.fileshare.Command;
import com.dreamlink.communication.server.SearchClient.OnSearchListener;
import com.dreamlink.communication.server.ServerConfig.OnServerConfigListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

public class ServerActivity extends Activity implements OnClickListener,
		OnCommunicationListener, OnServerConfigListener {
	@SuppressWarnings("unused")
	private static final String TAG = "ServerActivity";

	private EditText mMessageEditText;
	private Button mSendButton;
	private ListView mHistoricList;
	private ArrayAdapter<String> mHistoricListAdapter;

	private Context mContext;
	private Notice mNotice;

	private SocketCommunicationManager mCommunicationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		mContext = this;
		mNotice = new Notice(mContext);
		initView();

		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mCommunicationManager.registered(this);
	}

	private void initView() {
		mMessageEditText = (EditText) findViewById(R.id.edtMsg);
		mSendButton = (Button) findViewById(R.id.btnSend);
		mHistoricList = (ListView) findViewById(R.id.lstHistoric);

		mHistoricListAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1);
		mHistoricList.setAdapter(mHistoricListAdapter);

		mMessageEditText.setOnClickListener(this);
		mSendButton.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btnSend:
			if (NetWorkUtil.isNetworkConnected(mContext)) {
				String message = mMessageEditText.getText().toString();
				if (TextUtils.isEmpty(message)) {
					mNotice.showToast("Please input message");
				} else {
					mCommunicationManager.sendMessage(message.getBytes(), 0);
					mHistoricListAdapter.add("Send: " + message);
					mHistoricListAdapter.notifyDataSetChanged();
					mMessageEditText.setText("");
				}
			} else {
				mNotice.showToast("No network");
			}
		}
	}

	private void configServer() {
		ServerConfig serverConfig = new ServerConfig(mContext, this);
		serverConfig.showConfigDialog();
	}

	@Override
	public void onServerConfig(String portNumber) {
		// closeCommunication();
		SocketServerTask serverTask = new SocketServerTask(mContext,
				mCommunicationManager);
		serverTask.execute(new String[] { portNumber });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.server, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_server_config:
			configServer();
			break;
		case R.id.menu_network_status:
			showServerStatus();
			break;
		case R.id.menu_search:
			searchServer();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showServerStatus() {
		ServerStatus serverStatus = new ServerStatus(mContext);
		serverStatus.show();
	}

	public void closeCommunication() {
		mCommunicationManager.closeCommunication();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregistered(this);
		closeCommunication();
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			synchronized (msg) {
				switch (msg.what) {
				case SocketMessage.MSG_SOCKET_CONNECTED:
					break;

				case SocketMessage.MSG_SOCKET_NOTICE:
					String message = (String) (msg.obj);

					mNotice.showToast(message);
					break;

				case SocketMessage.MSG_SOCKET_MESSAGE:
					mHistoricListAdapter.add("Receive" + ": "
							+ (String) (msg.obj));
					mHistoricListAdapter.notifyDataSetChanged();

					break;
				}
			}
		};
	};

	private void searchServer() {
		Intent intent = new Intent();
		intent.setClass(this, ClientListActivity.class);
		startActivity(intent);
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication id) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onReceiveMessage");
		if (parseMessage(msg)) {
			// doSomething
			return;
		}
		final String messageBT = new String(msg);
		mHandler.obtainMessage(SocketMessage.MSG_SOCKET_MESSAGE, messageBT)
				.sendToTarget();
		final byte[] message = msg;
		final SocketCommunication com = id;
		new Thread() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				mCommunicationManager.sendMessage(message, (int) com.getId());
			}

		}.start();
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyConnectChanged() {
		// TODO Auto-generated method stub

	}

	/**
	 * 解析命令消息
	 * @param msg 命令消息
	 * @return 
	 */
	private boolean parseMessage(byte[] msg) {
		Log.d(TAG, "parseMessage");
		String cmdMsg = new String(msg);
		//发送回传消息
		String retMsg = "";
		
		//消息分割
		String[] splitMsg = cmdMsg.split(Command.AITE);
		
		//命令解析
		if (Command.LS.equals(splitMsg[0])) {
			//原来是要我给你文件浏览权限啊
			//看一下，你要看哪个目录的文件
			String path = "";
			if (Command.ROOT_PATH.equals(splitMsg[1])) {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					path = Environment.getExternalStorageDirectory().getAbsolutePath();
				}else {
					//sdcard不存在，不显示
					return false;
				}
			}else {
				path = splitMsg[1];
			}
			
			File file = new File(path);
			
			if (!file.exists()) {
				return false;
			}else if (file.isDirectory()) {
				//给回馈信息头文件标示
				retMsg = Command.LSRETN + Command.ENTER;
				//第二个表示当前路径
				retMsg += file.getPath() + Command.ENTER;
				//第三个标示父路径
				retMsg += file.getParentFile().getPath() + Command.ENTER;
				
				File[] files = file.listFiles();
				Log.e(TAG, "files.length=" + files.length);
				for (int i = 0; i < files.length; i++) {
					if (files[i].isHidden()) {
						//隐藏文件不给显示
					}else {
						if (files[i].isDirectory()) {
							//目录一只, 2013-05-04 22:22:22,<DIR>,0,Camera
							//[最后修改时间],[目录标识],[目录大小],[路径][目录名称]
							retMsg += files[i].lastModified() + Command.SEPARTOR 
									+ Command.DIR_FLAG + Command.SEPARTOR
									+ Command.DIR_SIZE  + Command.SEPARTOR 
									+ files[i].getAbsolutePath() + Command.SEPARTOR
									+ files[i].getName() + Command.ENTER;
						}else {
							//文件一枚，2013-05-04 22:22:22,,123123 Bytes,xxx.jpg
							//[最后修改时间],[文件标识],[文件大小],[路径],[文件名称]
							retMsg += files[i].lastModified() + Command.SEPARTOR 
									+ Command.FILE_FLGA + Command.SEPARTOR
									+ files[i].length()  + Command.SEPARTOR 
									+ files[i].getAbsolutePath() + Command.SEPARTOR
									+ files[i].getName() + Command.ENTER;
						}
					}
				}
				//增加结束标识符
				retMsg += Command.END_FLAG;
			}else {
				//不是文件夹，怎么浏览
				return false;
			}
			Log.d(TAG, "retMsg=" + retMsg);
			mCommunicationManager.sendMessage(retMsg.getBytes(), 0);
			return true;
		}else {
			//其他情况命令解析
			return false;
		}
	}
}
