package com.dreamlink.communication.server;

import java.net.Socket;
import java.util.ArrayList;

import com.dreamlink.communication.OnCommunicationListener;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.server.SearchClient.OnSearchListener;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ClientListActivity extends Activity implements OnSearchListener,
		OnClickListener, OnCommunicationListener {

	private Context mContext;

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mClients = new ArrayList<String>();
	private ListView mListView;

	private Notice mNotice;
	private SearchClient mSearchClient;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;

	private SocketCommunicationManager mCommunicationManager;

	private Handler mSearchHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				// Got another server.
				mNotice.showToast("Found another server: " + msg.obj);
				break;
			case MSG_SEARCH_FAIL:
				mNotice.showToast("Seach client fail.");
				break;

			default:
				break;
			}
		}
	};

	private Handler mSockMessageHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {

			case SocketMessage.MSG_SOCKET_CONNECTED:
				Socket socket = (Socket) msg.obj;
				mCommunicationManager.addCommunication(socket);
				addClient(socket.getInetAddress().getHostAddress());
				break;
			case SocketMessage.MSG_SOCKET_MESSAGE:
				String message = (String) msg.obj;
				mNotice.showToast(message);
				break;
			case SocketMessage.MSG_SOCKET_NOTICE:
				String notice = (String) msg.obj;
				mNotice.showToast(notice);
				break;

			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_list);
		mContext = this;
		mNotice = new Notice(mContext);

		initView();
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);
		SocketServerTask serverTask = new SocketServerTask(mContext,
				mCommunicationManager);
		serverTask.execute(new String[] { SocketCommunication.PORT });

		mSearchClient = SearchClient.getInstance(this);
		mSearchClient.setOnSearchListener(this);
		mSearchClient.startSearch();
		mNotice.showToast("Start Search");
	}

	private void initView() {
		mListView = (ListView) findViewById(R.id.list_client);
		mAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1, mClients);
		mListView.setAdapter(mAdapter);

		Button startButton = (Button) findViewById(R.id.btn_start);
		startButton.setOnClickListener(this);
		Button quitButton = (Button) findViewById(R.id.btn_quit);
		quitButton.setOnClickListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSearchClient != null) {
			mSearchClient.stopSearch();
		}
	}

	private void addClient(String ip) {
		if (!mClients.contains(ip)) {
			mClients.add(ip);
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onSearchSuccess(String clientIP) {
		if (!mClients.contains(clientIP)) {
			Message message = mSearchHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			message.obj = clientIP;
			mSearchHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchFail() {
		Message message = mSearchHandler.obtainMessage(MSG_SEARCH_FAIL);
		mSearchHandler.sendMessage(message);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			finish();
			break;
		case R.id.btn_quit:
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	public void onReceiveMessage(byte[] msg, int id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyConnectChanged(SocketCommunication com, boolean addFlag) {
		if (addFlag) {
			addClient(com.getConnectIP().getHostAddress());
		}
	}
}
