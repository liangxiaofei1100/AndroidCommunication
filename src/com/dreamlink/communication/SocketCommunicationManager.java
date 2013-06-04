package com.dreamlink.communication;

import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.dreamlink.communication.SocketCommunication.OnCommunicationChangedListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

public class SocketCommunicationManager implements
		OnCommunicationChangedListener {
	private static final String TAG = "SocketCommunicationManager";
	private static SocketCommunicationManager mInstance;

	private HashSet<SocketCommunication> mCommunications;
	/** Thread pool */
	private ExecutorService mExecutorService = null;
	private Context mContext;
	private Notice mNotice;

	private SocketCommunicationManager() {

	}

	private SocketCommunicationManager(Context context) {
		mContext = context;
		mNotice = new Notice(context);
		mCommunications = new HashSet<SocketCommunication>();
	}

	public static synchronized SocketCommunicationManager getInstance(
			Context context) {
		if (mInstance == null) {
			mInstance = new SocketCommunicationManager(context);
		}
		return mInstance;
	}

	public void sendMessage(SocketCommunication communication, String message) {
		if (TextUtils.isEmpty(message)) {
			return;
		}
		if (communication != null) {
			if (message.trim().length() > 0) {
				communication.sendMsg(message);
			}
		} else {
			mNotice.showToast("Connection lost.");
		}
	}

	public void sendMessage(String message, int id) {// if the id==-1,send all
		if (mCommunications != null && mCommunications.size() > 0) {
			for (SocketCommunication communication : mCommunications) {
				if (communication.getId() != id) {
					sendMessage(communication, message);
				}
			}
		} else {
			mNotice.showToast("No connection.");
		}
	}

	public void closeCommunication() {
		if (mCommunications != null) {
			for (SocketCommunication communication : mCommunications) {
				communication.stopComunication();
			}
		}
		mCommunications.clear();

		if (mExecutorService != null) {
			mExecutorService.shutdown();
		}
	}

	public void addCommunication(Socket socket, Handler handler) {
		if (mExecutorService == null) {
			mExecutorService = Executors.newCachedThreadPool();
		}
		SocketCommunication communication = new SocketCommunication(socket,
				handler, SocketMessage.MSG_SOCKET_MESSAGE,
				SocketMessage.MSG_SOCKET_NOTICE);
		communication.setOnCommunicationChangedListener(this);
		try {
			mExecutorService.execute(communication);
		} catch (RejectedExecutionException e) {
			Log.e(TAG, "addCommunication fail." + e.toString());
			e.printStackTrace();
		}

	}

	public HashSet<SocketCommunication> getCommunications() {
		return mCommunications;
	}

	@Override
	public void OnCommunicationEstablished(SocketCommunication communication) {
		mCommunications.add(communication);
	}

	@Override
	public void OnCommunicationLost(SocketCommunication communication) {
		mCommunications.remove(communication);
	}

}
