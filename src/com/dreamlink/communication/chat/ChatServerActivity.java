package com.dreamlink.communication.chat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListenerExternal;
import com.dreamlink.communication.data.User;
import com.dreamlink.communication.server.ServerStatus;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

/**
 * This activity is a chat server. It can send to chat client and receive
 * message from chat client.</br>
 * 
 * When there are more than one client connected. The server will send the
 * received message to all clients.</br>
 * 
 */
public class ChatServerActivity extends Activity implements OnClickListener,
		OnCommunicationListenerExternal {
	@SuppressWarnings("unused")
	private static final String TAG = "ChatServerActivity";

	private EditText mMessageEditText;
	private Button mSendButton;
	private ListView mHistoricList;
	private ArrayAdapter<String> mHistoricListAdapter;

	private Context mContext;
	private Notice mNotice;

	private SocketCommunicationManager mCommunicationManager;

	// Handler message
	private static final int MSG_RECEIVED_MESSAGE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		mContext = this;
		mNotice = new Notice(mContext);
		initView();

		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mCommunicationManager.registerOnCommunicationListenerExternal(this);
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
					// TODO app id need to implement.
					mCommunicationManager.sendMessageToAll(message.getBytes(),
							100);
					mHistoricListAdapter.add("Me: " + message);
					mHistoricListAdapter.notifyDataSetChanged();
					mMessageEditText.setText("");
				}
			} else {
				mNotice.showToast("No network");
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.server, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_network_status:
			showServerStatus();
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			synchronized (msg) {
				switch (msg.what) {
				case MSG_RECEIVED_MESSAGE:
					mHistoricListAdapter.add((String) (msg.obj));
					mHistoricListAdapter.notifyDataSetChanged();
					break;
				}
			}
		};
	};

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		Log.d(TAG, "onReceiveMessage");
		String receivedMsg = sendUser.getUserName() + ": " + new String(msg);
		mHandler.obtainMessage(MSG_RECEIVED_MESSAGE, receivedMsg)
				.sendToTarget();
	}

	@Override
	public void onUserConnected(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserDisconnected(User user) {
		// TODO Auto-generated method stub

	}
}
