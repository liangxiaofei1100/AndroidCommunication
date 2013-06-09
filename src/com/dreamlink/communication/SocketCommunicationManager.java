package com.dreamlink.communication;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.dreamlink.communication.SocketCommunication.OnCommunicationChangedListener;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

public class SocketCommunicationManager implements
		OnCommunicationChangedListener, ICommunicate {
	private static final String TAG = "SocketCommunicationManager";
	private static SocketCommunicationManager mInstance;

	private HashSet<SocketCommunication> mCommunications;
	/** Thread pool */
	private ExecutorService mExecutorService = null;
	private Context mContext;
	private Notice mNotice;
	private ArrayList<OnCommunicationListener> list;

	private SocketCommunicationManager() {

	}

	private SocketCommunicationManager(Context context) {
		mContext = context;
		list = new ArrayList<OnCommunicationListener>();
		mNotice = new Notice(context);
		mCommunications = new HashSet<SocketCommunication>();
	}

	public static synchronized SocketCommunicationManager getInstance(
			Context context) {
		if (mInstance == null) {
			mInstance = new SocketCommunicationManager(context);
		}
		mInstance.clientFlag = false;
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

	public void sendMessage(String message, int idThread) {
		if (mCommunications != null && mCommunications.size() > 0) {
			synchronized (mCommunications) {
				for (SocketCommunication communication : mCommunications) {
					if (communication.getId() != idThread) {
						sendMessage(communication, message);
					}
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
		if (!clientFlag) {
			SocketServer.getInstance().stopServer();
		}
		if (mExecutorService != null) {
			mExecutorService.shutdown();
		}
	}

	public void addCommunication(Socket socket) {
		if (mExecutorService == null) {
			mExecutorService = Executors.newCachedThreadPool();
		}
		SocketCommunication communication = new SocketCommunication(socket,
				this);
		communication.setOnCommunicationChangedListener(this);
		notifyComunicationChange(communication, true);
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
		synchronized (mCommunications) {
			mCommunications.add(communication);
			if (!mCommunications.isEmpty()) {
				for (SocketCommunication comm : mCommunications) {
					if ((comm.getConnectIP().equals(communication
							.getConnectIP()))
							&& (comm.getId() != communication.getId())) {
						comm.stopComunication();
					}
				}
			}
		}
	}

	@Override
	public void OnCommunicationLost(SocketCommunication communication) {
		synchronized (communication) {
			mCommunications.remove(communication);
			notifyComunicationChange(communication, false);
		}
		if (mCommunications.isEmpty()) {
			mExecutorService.shutdown();
			mExecutorService = null;
		}
	}

	private boolean clientFlag = false;

	public static synchronized SocketCommunicationManager getInstance(
			Context context, boolean flag) {
		if (mInstance == null) {
			mInstance = new SocketCommunicationManager(context);
		}
		mInstance.clientFlag = flag;
		return mInstance;
	}

	public void registered(OnCommunicationListener iSubscribe) {
		list.add(iSubscribe);
	}

	public void unregistered(OnCommunicationListener iSubscribe) {
		list.remove(iSubscribe);
	}

	@Override
	public void receiveMessage(byte[] msg, int ID) {
		// TODO Auto-generated method stub
		sendMessage(new String(msg), ID);
		if (!list.isEmpty()) {
			for (OnCommunicationListener listener : list) {
				listener.onReceiveMessage(msg, ID);
			}
		}
	}

	@Override
	public void sendMessage(byte[] msg) {
		// TODO Auto-generated method stub
	}

	/**
	 * @param addFlag
	 *            ,if true ,connect add ,else connect remove
	 * */
	private void notifyComunicationChange(SocketCommunication com,
			boolean addFlag) {
		//if need notify someone ,doing here
	}
}
