package com.dreamlink.communication.server;

import java.net.Socket;
import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.server.SearchClient.OnSearchListener;
import com.dreamlink.communication.server.ServerConfig.OnServerConfigListener;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

public class ServerActivity extends Activity implements OnClickListener,
		OnServerConfigListener {
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
					mCommunicationManager.sendMessage(message, -1);
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
		SocketServerTask serverTask = new SocketServerTask(mContext, mHandler,
				SocketMessage.MSG_SOCKET_CONNECTED);
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
		closeCommunication();
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			synchronized (msg) {
				switch (msg.what) {
				case SocketMessage.MSG_SOCKET_CONNECTED:
					Socket socket = (Socket) (msg.obj);
					mCommunicationManager.addCommunication(socket, mHandler);
					break;

				case SocketMessage.MSG_SOCKET_NOTICE:
					String message = (String) (msg.obj);

					mNotice.showToast(message);
					break;

				case SocketMessage.MSG_SOCKET_MESSAGE:
					final String messageBT = (String) (msg.obj);
					final int id = msg.arg1;

					mHistoricListAdapter.add("Receive" + ": " + messageBT);
					mHistoricListAdapter.notifyDataSetChanged();
					// add by liucheng
					new Thread() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							super.run();
							mCommunicationManager.sendMessage(messageBT, id);
						}
					}.start();
					// end add by liucheng
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

}
