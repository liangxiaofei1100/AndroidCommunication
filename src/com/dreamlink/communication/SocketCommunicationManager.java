package com.dreamlink.communication;

import java.io.File;
import java.net.Socket;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;
import android.os.RemoteException;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.SocketCommunication.OnReceiveMessageListener;
import com.dreamlink.communication.SocketCommunication.OnCommunicationChangedListener;
import com.dreamlink.communication.UserManager.OnUserChangedListener;
import com.dreamlink.communication.client.SocketClientTask;
import com.dreamlink.communication.client.SocketClientTask.OnConnectedToServerListener;
import com.dreamlink.communication.protocol.ProtocolDecoder;
import com.dreamlink.communication.protocol.ProtocolEncoder;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.server.SocketServerTask;
import com.dreamlink.communication.server.SocketServerTask.OnClientConnectedListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

/**
 * This class is used for providing communication operations for activity.</br>
 * 
 * This class is single instance, so use {@link #getInstance(Context)} to get
 * object.
 * 
 */
public class SocketCommunicationManager implements OnClientConnectedListener,
		OnConnectedToServerListener, OnCommunicationChangedListener,
		OnReceiveMessageListener, OnUserChangedListener, ILoginRequestCallBack,
		ILoginRespondCallback {
	private static final String TAG = "SocketCommunicationManager";

	/**
	 * Interface for Activity.
	 * 
	 */
	public interface OnCommunicationListener {
		// TODO need to update.

		/**
		 * Received a message from communication.</br>
		 * 
		 * Be careful, this method is not run in UI thread. If do UI operation,
		 * we can use {@link android.os.Handler} to do UI operation.</br>
		 * 
		 * @param msg
		 *            the message.
		 * @param communication
		 *            the message from.
		 */
		void onReceiveMessage(byte[] msg, SocketCommunication communication);

		void onSendResult(byte[] msg);

		/**
		 * There is new communication established or a communication lost.
		 */
		void notifyConnectChanged();

	}

	private static SocketCommunicationManager mInstance;

	private Context mContext;
	private Notice mNotice;

	private Vector<SocketCommunication> mCommunications;
	/** Thread pool */
	private ExecutorService mExecutorService = null;

	private Vector<OnCommunicationListener> mOnCommunicationListeners;

	/**
	 * Map for OnCommunicationListenerExternal and appID management. When an
	 * application register to SocketCommunicationManager, record it in this
	 * map. When received a message, notify the related applications base on the
	 * appID.</br>
	 * 
	 * Map structure</br>
	 * 
	 * key: listener, value: app ID.
	 */
	private ConcurrentHashMap<OnCommunicationListenerExternal, Integer> mOnCommunicationListenerExternals = new ConcurrentHashMap<OnCommunicationListenerExternal, Integer>();
	private UserManager mUserManager = UserManager.getInstance();
	private ProtocolDecoder mProtocolDecoder;

	private ILoginRequestCallBack mLoginRequestCallBack;
	private ILoginRespondCallback mLoginRespondCallback;

	private SocketCommunicationManager() {

	}

	private SocketCommunicationManager(Context context) {
		mContext = context;
		mOnCommunicationListeners = new Vector<OnCommunicationListener>();
		mNotice = new Notice(context);
		mCommunications = new Vector<SocketCommunication>();

		mUserManager.registerOnUserChangedListener(this);

		mProtocolDecoder = new ProtocolDecoder(this);
		mProtocolDecoder.setLoginRequestCallBack(this);
		mProtocolDecoder.setLoginRespondCallback(this);
	}

	public static synchronized SocketCommunicationManager getInstance(
			Context context) {
		if (mInstance == null) {
			mInstance = new SocketCommunicationManager(
					context.getApplicationContext());
		}
		return mInstance;
	}

	public void registerOnCommunicationListenerExternal(
			OnCommunicationListenerExternal listener, int appID) {
		Log.d(TAG, "registerOnCommunicationListenerExternal() appID = " + appID);
		mOnCommunicationListenerExternals.put(listener, appID);
	}

	public void unregisterOnCommunicationListenerExternal(
			OnCommunicationListenerExternal listener) {
		int appID = mOnCommunicationListenerExternals.remove(listener);
		Log.d(TAG, "unregisterOnCommunicationListenerExternal() appID = " + appID);
	}

	public void setLoginRequestCallBack(ILoginRequestCallBack callback) {
		mLoginRequestCallBack = callback;
	}

	public void setLoginRespondCallback(ILoginRespondCallback callback) {
		mLoginRespondCallback = callback;
	}

	/**
	 * Send message to communication.</br>
	 * 
	 * for internal use.
	 * 
	 * @param communication
	 * @param message
	 */
	public void sendMessage(SocketCommunication communication, byte[] message) {
		if (message.length == 0) {
			return;
		}
		if (communication != null) {
			communication.sendMessage(message);
		} else {
			mNotice.showToast("Connection lost.");
		}
	}

	/**
	 * Stop all communications. </br>
	 * 
	 * Notice, this method should not be called by apps.</br>
	 */
	public void closeAllCommunication() {
		if (mCommunications != null) {
			synchronized (mCommunications) {
				for (final SocketCommunication communication : mCommunications) {
					new Thread() {
						@Override
						public void run() {
							communication.stopComunication();
						}
					}.start();
				}
			}
		}
		mCommunications.clear();
		if (SocketServer.getInstance() != null) {
			SocketServer.getInstance().stopServer();
		}
		if (mExecutorService != null) {
			mExecutorService.shutdown();
		}
	}

	/**
	 * Start a communication.
	 * 
	 * @param socket
	 */
	public void startCommunication(Socket socket) {
		if (mExecutorService == null) {
			mExecutorService = Executors.newCachedThreadPool();
		}
		SocketCommunication communication = new SocketCommunication(socket,
				this);
		communication.setOnCommunicationChangedListener(this);
		try {
			mExecutorService.execute(communication);
		} catch (RejectedExecutionException e) {
			Log.e(TAG, "addCommunication fail." + e.toString());
			e.printStackTrace();
		}

	}

	/**
	 * Get all communications
	 * 
	 * @return
	 */
	public Vector<SocketCommunication> getCommunications() {
		return mCommunications;
	}

	@Override
	public void OnCommunicationEstablished(SocketCommunication communication) {
		synchronized (mCommunications) {
			mCommunications.add(communication);
			if (!SocketServer.getInstance().isServerStarted()) {
				sendLoginRequest();
			}
			if (!mCommunications.isEmpty()) {
				for (SocketCommunication comm : mCommunications) {
					if ((comm.getConnectedAddress().equals(communication
							.getConnectedAddress()))
							&& (comm.getId() != communication.getId())) {
						comm.stopComunication();
					}
				}
			}
			notifyComunicationChange();
		}
	}

	@Override
	public void OnCommunicationLost(SocketCommunication communication) {
		mCommunications.remove(communication);
		notifyComunicationChange();
		if (mCommunications.isEmpty()) {
			if (mExecutorService != null) {
				mExecutorService.shutdown();
				mExecutorService = null;
			}
		}
		mUserManager.removeUser(communication);
		mUserManager.removeLocalCommunication(communication);
		sendMessageToUpdateAllUser();
	}

	public void registered(OnCommunicationListener iSubscribe) {
		mOnCommunicationListeners.add(iSubscribe);
	}

	public void unregistered(OnCommunicationListener iSubscribe) {
		mOnCommunicationListeners.remove(iSubscribe);
	}

	@Override
	public void onReceiveMessage(byte[] msg,
			SocketCommunication socketCommunication) {
		// decode;
		mProtocolDecoder.decode(msg, socketCommunication);
	}

	private ConcurrentHashMap<Integer, SocketCommunication> mLocalCommunications = new ConcurrentHashMap<Integer, SocketCommunication>();
	private int mLastLocalID = 0;

	public int addLocalCommunicaiton(SocketCommunication communication) {
		if (!mLocalCommunications.contains(communication)) {
			mLastLocalID++;
			mLocalCommunications.put(mLastLocalID, communication);
			return mLastLocalID;
		} else {
			int id = 0;
			for (Map.Entry<Integer, SocketCommunication> entry : mLocalCommunications
					.entrySet()) {
				if (communication == entry.getValue()) {
					id = entry.getKey();
					break;
				}
			}
			return id;
		}
	}

	public void removeLocalCommunicaiton(int id) {
		mLocalCommunications.remove(id);
	}

	public SocketCommunication getLocalCommunicaiton(int id) {
		return mLocalCommunications.get(id);
	}

	/**
	 * client login server directly.
	 */
	public void sendLoginRequest() {
		byte[] loginRequest = ProtocolEncoder.encodeLoginRequest(mUserManager
				.getLocalUser());
		sendMessageToAllWithoutEncode(loginRequest);
	}

	/**
	 * Send message to the receiver.
	 * 
	 * @param msg
	 * @param receiveUser
	 * @param appID
	 */
	public void sendMessageToSingle(byte[] msg, User receiveUser, int appID) {
		int localUserID = mUserManager.getLocalUser().getUserID();
		int receiveUserID = receiveUser.getUserID();
		byte[] data = ProtocolEncoder.encodeSendMessageToSingle(msg,
				localUserID, receiveUserID, appID);
		sendMessageToAllWithoutEncode(data);
	}

	/**
	 * Send message to all users in the network.
	 * 
	 * @param msg
	 */
	public void sendMessageToAll(byte[] msg, int appID) {
		Log.d(TAG, "sendMessageToAll.msg.=" + new String(msg));
		int localUserID = mUserManager.getLocalUser().getUserID();
		byte[] data = ProtocolEncoder.encodeSendMessageToAll(msg, localUserID,
				appID);
		sendMessageToAllWithoutEncode(data);
	}

	/**
	 * send the message to all users in the network.
	 * 
	 * @param msg
	 */
	public void sendMessageToAllWithoutEncode(byte[] msg) {
		for (SocketCommunication communication : mCommunications) {
			sendMessage(communication, msg);
		}
	}

	/**
	 * @param addFlag
	 *            ,if true ,connect add ,else connect remove
	 * */
	private void notifyComunicationChange() {
		// TODO need to update.
		// if need notify someone ,doing here
		if (!mOnCommunicationListeners.isEmpty()) {
			for (OnCommunicationListener listener : mOnCommunicationListeners) {
				listener.notifyConnectChanged();
			}
		}
	}

	/**
	 * Start Server.
	 * 
	 * @param context
	 */
	public void startServer(Context context) {
		SocketServerTask serverTask = new SocketServerTask(context);
		serverTask.setOnClientConnectedListener(this);
		serverTask.execute(new String[] { SocketCommunication.PORT });
	}

	/**
	 * Stop server.
	 */
	public void stopServer() {
		SocketServer server = SocketServer.getInstance();
		server.stopServer();
	}

	/**
	 * Update user when user connect and disconnect.
	 */
	private void sendMessageToUpdateAllUser() {
		byte[] allUserData = ProtocolEncoder.encodeUpdateAllUser(mUserManager);
		sendMessageToAllWithoutEncode(allUserData);
	}

	/**
	 * Connect to server.
	 * 
	 * @param context
	 *            Activity context.
	 * @param serverIp
	 */
	public void connectServer(Context context, String serverIp) {
		SocketClientTask clientTask = new SocketClientTask(context);
		clientTask.setOnConnectedToServerListener(this);
		clientTask.execute(new String[] { serverIp, SocketCommunication.PORT });
	}

	public void notifyReceiveListeners(int sendUserID, int appID, byte[] data) {
		for (Map.Entry<OnCommunicationListenerExternal, Integer> entry : mOnCommunicationListenerExternals
				.entrySet()) {
			if (entry.getValue() == appID) {
				try {
					entry.getKey().onReceiveMessage(data,
							mUserManager.getAllUser().get(sendUserID));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onUserConnected(User user) {
		for (Map.Entry<OnCommunicationListenerExternal, Integer> entry : mOnCommunicationListenerExternals
				.entrySet()) {
			try {
				entry.getKey().onUserConnected(user);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onUserDisconnected(User user) {
		for (Map.Entry<OnCommunicationListenerExternal, Integer> entry : mOnCommunicationListenerExternals
				.entrySet()) {
			try {
				entry.getKey().onUserDisconnected(user);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onLoginSuccess(User localUser, SocketCommunication communication) {
		if (mLoginRespondCallback != null) {
			mLoginRespondCallback.onLoginSuccess(localUser, communication);
		} else {
			Log.d(TAG, "mLoginReusltCallback is null");
		}
	}

	@Override
	public void onLoginFail(int failReason, SocketCommunication communication) {
		if (mLoginRespondCallback != null) {
			mLoginRespondCallback.onLoginFail(failReason, communication);
		} else {
			Log.d(TAG, "mLoginReusltCallback is null");
		}
	}

	@Override
	public void onLoginRequest(User user, SocketCommunication communication) {
		Log.d(TAG, "onLoginRequest()");
		if (mLoginRequestCallBack != null) {
			mLoginRequestCallBack.onLoginRequest(user, communication);
		}

	}

	/**
	 * Respond to the login request.
	 * 
	 * @param user
	 * @param communication
	 * @param isAllow
	 */
	public void respondLoginRequest(User user,
			SocketCommunication communication, boolean isAllow) {
		mProtocolDecoder.respondLoginRequest(user, communication, isAllow);
	}

	@Override
	public void onClientConnected(Socket clientSocket) {
		startCommunication(clientSocket);
	}

	@Override
	public void onConnectedToServer(Socket socket) {
		startCommunication(socket);
	}

}
