package com.dreamlink.communication.client;

import java.net.Socket;

import android.app.Activity;
import android.content.Context;
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
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.client.ClientConfig.OnClientConfigListener;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.server.SearchClient;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

public class ClientActivity extends Activity implements OnClickListener,
		OnClientConfigListener, OnSearchListener {
	@SuppressWarnings("unused")
	private static final String TAG = "ClientActivity";

	private EditText mMessageEidtText;
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
				.getInstance(mContext,true);
	}

	private void initView() {
		mMessageEidtText = (EditText) findViewById(R.id.edtMsg);
		mSendButton = (Button) findViewById(R.id.btnSend);
		mHistoricList = (ListView) findViewById(R.id.lstHistoric);

		mHistoricListAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1);
		mHistoricList.setAdapter(mHistoricListAdapter);

		mMessageEidtText.setOnClickListener(this);
		mSendButton.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCommunicationManager.getCommunications().size() == 0) {
			configClient();
		}
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
					mCommunicationManager.sendMessage(message,-1);
					mHistoricListAdapter.add("Send: " + message);
					mHistoricListAdapter.notifyDataSetChanged();
					mMessageEidtText.setText("");
				}
			} else {
				mNotice.showToast("No network");
			}
		}
	}

	private void configClient() {
		ClientConfig clientConfig = new ClientConfig(mContext, this);
		clientConfig.showConfigDialog();
	}

	@Override
	public void onClientConfig(String serverIP, String portNumber) {
		SocketClientTask clientTask = new SocketClientTask(mContext, mHandler,
				SocketMessage.MSG_SOCKET_CONNECTED);
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
					Socket socket = (Socket) (msg.obj);
					mCommunicationManager.addCommunication(socket, mHandler);
					break;

				case SocketMessage.MSG_SOCKET_NOTICE:
					String message = (String) (msg.obj);

					mNotice.showToast(message);
					break;

				case SocketMessage.MSG_SOCKET_MESSAGE:
					String messageBT = (String) (msg.obj);

					mHistoricListAdapter.add("Receive" + ": "+messageBT);
					mHistoricListAdapter.notifyDataSetChanged();
					break;
				}
			}
		};
	};

	private SearchSever mSearchServer;
	
	private void searchServer() {
		mSearchServer = new SearchSever(this);
		mSearchServer.startSearch(getApplicationContext());
		mNotice.showToast("Start Search");
	}

	@Override
	public void onSearchSuccess(String clineIP) {
		Message message = mHandler
				.obtainMessage(SocketMessage.MSG_SOCKET_NOTICE,
						"onSearchSuccess + " + clineIP);
		mHandler.sendMessage(message);

	}

	@Override
	public void onSearchFail() {
		Message message = mHandler.obtainMessage(
				SocketMessage.MSG_SOCKET_NOTICE, "onSearchFail");
		mHandler.sendMessage(message);
	}

	@Override
	public void onOffLine(String clineIP) {
		Message message = mHandler.obtainMessage(
				SocketMessage.MSG_SOCKET_NOTICE, "onOffLine");
		mHandler.sendMessage(message);

	}

}
