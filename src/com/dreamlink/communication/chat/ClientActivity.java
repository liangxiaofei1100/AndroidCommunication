package com.dreamlink.communication.chat;

import java.net.Socket;

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
import com.dreamlink.communication.Search;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.SocketCommunication.OnCommunicationChangedListener;
import com.dreamlink.communication.client.ClientConfig;
import com.dreamlink.communication.client.ClientStatus;
import com.dreamlink.communication.client.SearchSever;
import com.dreamlink.communication.client.ServerListActivity;
import com.dreamlink.communication.client.SocketClientTask;
import com.dreamlink.communication.client.ClientConfig.OnClientConfigListener;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.fileshare.FileListActivity;
import com.dreamlink.communication.fileshare.FileMainUI;
import com.dreamlink.communication.server.SearchClient;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

public class ClientActivity extends Activity implements OnClickListener,
		OnCommunicationListener, OnClientConfigListener {
	@SuppressWarnings("unused")
	private static final String TAG = "ClientActivity";

	private static final int REQUEST_SERACH_SERVER = 1;

	private EditText mMessageEidtText;
	private Button mSendButton;

	private ListView mHistoricList;
	private ArrayAdapter<String> mHistoricListAdapter;

	private Context mContext;
	private Notice mNotice;

	private SocketCommunicationManager mCommunicationManager;

	private SearchSever mSearchServer;
	
	//add by yuri
	//add a button to access remote server
	private Button mAccessBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		mContext = this;
		mNotice = new Notice(mContext);
		initView();
		mCommunicationManager = SocketCommunicationManager.getInstance(
				mContext, true);
		mCommunicationManager.registered(this);
	}

	private void initView() {
		mMessageEidtText = (EditText) findViewById(R.id.edtMsg);
		mSendButton = (Button) findViewById(R.id.btnSend);
		mHistoricList = (ListView) findViewById(R.id.lstHistoric);
		
		mAccessBtn = (Button) findViewById(R.id.btnAccess);
		mAccessBtn.setVisibility(View.VISIBLE);

		mHistoricListAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1);
		mHistoricList.setAdapter(mHistoricListAdapter);

		mMessageEidtText.setOnClickListener(this);
		mSendButton.setOnClickListener(this);
		mAccessBtn.setOnClickListener(this);
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
				String message = mMessageEidtText.getText().toString();
				if (TextUtils.isEmpty(message)) {
					mNotice.showToast("Please input message");
				} else {
					mCommunicationManager.sendMessage(message.getBytes(), 0);
					mHistoricListAdapter.add("Send: " + message);
					mHistoricListAdapter.notifyDataSetChanged();
					mMessageEidtText.setText("");
				}
			} else {
				mNotice.showToast("No network");
			}
			break;
			
		case R.id.btnAccess:
			//start FileListActivity
//			Intent intent = new Intent(ClientActivity.this, FileListActivity.class);
//			startActivity(intent);
			
			//use new file share activity
			Intent intent = new Intent(ClientActivity.this, FileMainUI.class);
			startActivity(intent);
			break;
		}
	}

	private void configClient() {
		ClientConfig clientConfig = new ClientConfig(mContext, this);
		clientConfig.showConfigDialog();
	}

	@Override
	public void onClientConfig(String serverIP, String portNumber) {
		SocketClientTask clientTask = new SocketClientTask(mContext,
				mCommunicationManager, SocketMessage.MSG_SOCKET_CONNECTED);
		clientTask.execute(new String[] { serverIP, portNumber });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.client, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_client_config:
			if (mCommunicationManager.getCommunications().size() > 0) {
				mNotice.showToast("Already connected to server.");
			} else {
				configClient();
			}
			break;
		case R.id.menu_network_status:
			showClientStatus();
			break;
		case R.id.menu_search:
			searchServer();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showClientStatus() {
		ClientStatus clientStatus = new ClientStatus(mContext);
		clientStatus.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregistered(this);
		mCommunicationManager.closeCommunication();
		if (mSearchServer != null) {
			mSearchServer.stopSearch();
		}

	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			synchronized (msg) {
				switch (msg.what) {
				case SocketMessage.MSG_SOCKET_CONNECTED:
//					Socket socket = (Socket) (msg.obj);
//					mCommunicationManager.addCommunication(socket, mHandler);
					break;

				case SocketMessage.MSG_SOCKET_NOTICE:
					String message = (String) (msg.obj);

					mNotice.showToast(message);
					break;

				case SocketMessage.MSG_SOCKET_MESSAGE:
					String messageBT = (String) (msg.obj);

					mHistoricListAdapter.add("Receive" + ": " + messageBT);
					mHistoricListAdapter.notifyDataSetChanged();
					break;
				}
			}
		};
	};

	private void searchServer() {
		Intent intent = new Intent();
		intent.setClass(this, ServerListActivity.class);
		startActivityForResult(intent, REQUEST_SERACH_SERVER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_SERACH_SERVER:
				String serverIP = data.getStringExtra(Search.EXTRA_IP);
				onClientConfig(serverIP, SocketCommunication.PORT);
				break;

			default:
				break;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication id) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onReceiveMessage");
		/** need to parse the msg */
		String messageBT = new String(msg);
		mHandler.obtainMessage(SocketMessage.MSG_SOCKET_MESSAGE, messageBT)
				.sendToTarget();
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyConnectChanged() {
		// TODO Auto-generated method stub
		
	}
	
}
