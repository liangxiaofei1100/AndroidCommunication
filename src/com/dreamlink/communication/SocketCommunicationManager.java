package com.dreamlink.communication;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;

import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.SocketCommunication.ICommunicate;
import com.dreamlink.communication.SocketCommunication.OnCommunicationChangedListener;
import com.dreamlink.communication.UserManager.OnUserChangedListener;
import com.dreamlink.communication.client.SocketClientTask;
import com.dreamlink.communication.data.User;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.protocol.ProtocolDecoder;
import com.dreamlink.communication.protocol.ProtocolEncoder;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.server.SocketServerTask;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

/**
 * This class is used for providing communication operations for activity.</br>
 * 
 * This class is single instance, so use {@link #getInstance(Context)} to get
 * object.
 * 
 */
public class SocketCommunicationManager implements
		OnCommunicationChangedListener, ICommunicate, OnUserChangedListener,
		ILoginRequestCallBack, ILoginRespondCallback {
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

	/**
	 * Interface for Activity.
	 * 
	 */
	public interface OnCommunicationListenerExternal {

		/**
		 * Received a message from user.</br>
		 * 
		 * Be careful, this method is not run in UI thread. If do UI operation,
		 * we can use {@link android.os.Handler} to do UI operation.</br>
		 * 
		 * @param msg
		 *            the message.
		 * @param sendUser
		 *            the message from.
		 */
		void onReceiveMessage(byte[] msg, User sendUser);

		/**
		 * There is new user connected.
		 * 
		 * @param user
		 */
		void onUserConnected(User user);

		/**
		 * There is a user disconnected.
		 * 
		 * @param user
		 */
		void onUserDisconnected(User user);

	}

	private static SocketCommunicationManager mInstance;

	private Context mContext;
	private Notice mNotice;

	private Vector<SocketCommunication> mCommunications;
	/** Thread pool */
	private ExecutorService mExecutorService = null;

	private Vector<OnCommunicationListener> mOnCommunicationListeners;

	/**
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
		// mCommunications = new HashSet<SocketCommunication>();
		mCommunications = new Vector<SocketCommunication>();

		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);
		mUserManager.registerOnUserChangedListener(this);

		mProtocolDecoder = new ProtocolDecoder(this);
		mProtocolDecoder.setLoginRequestCallBack(this);
		mProtocolDecoder.setLoginRespondCallback(this);
	}

	public static synchronized SocketCommunicationManager getInstance(
			Context context) {
		if (mInstance == null) {
			mInstance = new SocketCommunicationManager(context);
		}
		return mInstance;
	}

	public void registerOnCommunicationListenerExternal(
			OnCommunicationListenerExternal listener, int appID) {
		mOnCommunicationListenerExternals.put(listener, appID);
	}

	public void unregisterOnCommunicationListenerExternal(
			OnCommunicationListenerExternal listener) {
		mOnCommunicationListenerExternals.remove(listener);
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
			communication.sendMsg(message);
		} else {
			mNotice.showToast("Connection lost.");
		}
	}

	/**
	 * For internal use.
	 * 
	 * @param message
	 * @param idThread
	 */
	public void sendMessage(byte[] message, int idThread) {
		// TODO need to update.
		if (idThread == -1) {
			return;
		}
		if (mCommunications != null) {
			synchronized (mCommunications) {
				for (SocketCommunication communication : mCommunications) {
					if (communication.getId() != idThread) {
						sendMessage(communication, message);
					}
				}
			}
		} else {
			try {
				mNotice.showToast("No connection.");
			} catch (Exception e) {
				// call in thread that has not called Looper.prepare().
			}
		}
	}

	/**
	 * send file to client</br>
	 * 
	 * 
	 * @param file
	 *            the file that need to send
	 * @param idThread
	 *            i don't know also
	 * @author yuri
	 */
	public void sendMessage(File file, int idThread) {
		// TODO need to update.
		if (idThread == -1) {
			return;
		}

		if (mCommunications != null && mCommunications.size() > 0) {
			for (SocketCommunication communication : mCommunications) {
				if (communication.getId() != idThread) {
					sendMessage(communication, file);
				}
			}
		} else {
			mNotice.showToast("No connection.");
		}
	}

	/**
	 * send file
	 * 
	 * @param communication
	 *            don't know also
	 * @param file
	 * @author yuri
	 */
	public void sendMessage(SocketCommunication communication, File file) {
		// TODO need to update.
		if (file == null) {
			return;
		}

		if (communication != null) {
			communication.sendMsg(file);
		} else {
			mNotice.showToast("Connection lost.");
		}
	}

	/**
	 * Stop all communications. </br>
	 * 
	 * Notice, this method should not be called by apps.</br>
	 */
	public void closeCommunication() {
		if (mCommunications != null) {
			for (final SocketCommunication communication : mCommunications) {
				new Thread() {
					@Override
					public void run() {
						communication.stopComunication();
					}
				}.start();
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
	public void addCommunication(Socket socket) {
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
			if (!mCommunications.isEmpty()) {
				for (SocketCommunication comm : mCommunications) {
					if ((comm.getConnectIP().equals(communication
							.getConnectIP()))
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
		synchronized (mCommunications) {
			if (mCommunications.contains(communication)) {
				mCommunications.remove(communication);
				notifyComunicationChange();
			}
		}
		if (mCommunications.isEmpty()) {
			if (mExecutorService == null) {
				return;
			}
			mExecutorService.shutdown();
			mExecutorService = null;
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
	public void receiveMessage(byte[] msg,
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

	@Override
	public void sendMessage(byte[] msg) {
		// TODO need to update.
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
		SocketServerTask serverTask = new SocketServerTask(context, this);
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
		SocketClientTask clientTask = new SocketClientTask(context, this);
		clientTask.execute(new String[] { serverIp, SocketCommunication.PORT });
	}

	public void notifyReceiveListeners(int sendUserID, int appID, byte[] data) {
		for (Map.Entry<OnCommunicationListenerExternal, Integer> entry : mOnCommunicationListenerExternals
				.entrySet()) {
			if (entry.getValue() == appID) {
				entry.getKey().onReceiveMessage(data,
						mUserManager.getAllUser().get(sendUserID));
			}
		}
	}

	@Override
	public void onUserConnected(User user) {
		for (Map.Entry<OnCommunicationListenerExternal, Integer> entry : mOnCommunicationListenerExternals
				.entrySet()) {
			entry.getKey().onUserConnected(user);
		}
	}

	@Override
	public void onUserDisconnected(User user) {
		for (Map.Entry<OnCommunicationListenerExternal, Integer> entry : mOnCommunicationListenerExternals
				.entrySet()) {
			entry.getKey().onUserDisconnected(user);
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

}
