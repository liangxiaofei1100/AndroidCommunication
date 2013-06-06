package com.dreamlink.communication.server;

import java.net.Socket;
import java.util.ArrayList;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ClientListActivity extends Activity implements OnSearchListener {

	private Context mContext;

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mClients = new ArrayList<String>();
	private ListView mListView;

	private Notice mNotice;
	private SearchClient mSearchClient;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;

	private Handler mHandler = new Handler() {

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
				mCommunicationManager.addCommunication(socket,
						mSockMessageHandler);
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
	SocketCommunicationManager mCommunicationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_list);
		mContext = this;
		mNotice = new Notice(mContext);

		initView();

		SocketServerTask serverTask = new SocketServerTask(mContext,
				mSockMessageHandler, SocketMessage.MSG_SOCKET_CONNECTED);
		serverTask.execute(new String[] { SocketCommunication.PORT });

		mCommunicationManager = SocketCommunicationManager.getInstance(this);

		mSearchClient = SearchClient.getInstance(this);
		mSearchClient.startSearch(getApplicationContext());
		mNotice.showToast("Start Search");
	}

	private void initView() {
		mListView = (ListView) findViewById(R.id.list_client);
		mAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1, mClients);
		mListView.setAdapter(mAdapter);

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
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			message.obj = clientIP;
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchFail() {
		Message message = mHandler.obtainMessage(MSG_SEARCH_FAIL);
		mHandler.sendMessage(message);
	}
}
