package com.dreamlink.communication;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.content.Context;

import com.dreamlink.communication.SocketCommunication.ICommunicate;
import com.dreamlink.communication.SocketCommunication.OnCommunicationChangedListener;
import com.dreamlink.communication.client.SocketClientTask;
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
		OnCommunicationChangedListener, ICommunicate {
	/**
	 * Interface for Activity.
	 * 
	 */
	public interface OnCommunicationListener {

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

	private static final String TAG = "SocketCommunicationManager";
	private static SocketCommunicationManager mInstance;

	private Context mContext;
	private Notice mNotice;

	private HashSet<SocketCommunication> mCommunications;
	/** Thread pool */
	private ExecutorService mExecutorService = null;
	private ArrayList<OnCommunicationListener> mOnCommunicationListeners;

	private boolean clientFlag = false;

	private SocketCommunicationManager() {

	}

	private SocketCommunicationManager(Context context) {
		mContext = context;
		mOnCommunicationListeners = new ArrayList<OnCommunicationListener>();
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

	public static synchronized SocketCommunicationManager getInstance(
			Context context, boolean flag) {
		if (mInstance == null) {
			mInstance = new SocketCommunicationManager(context);
		}
		mInstance.clientFlag = flag;
		return mInstance;
	}

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

	public void sendMessage(byte[] message, int idThread) {
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
	 * send file to client
	 * 
	 * @param file
	 *            the file that need to send
	 * @param idThread
	 *            i don't know also
	 * @author yuri
	 */
	public void sendMessage(File file, int idThread) {
		if (idThread == -1) {
			return;
		}

		if (mCommunications != null && mCommunications.size() > 0) {
			HashSet<SocketCommunication> hash = mCommunications;
			for (SocketCommunication communication : hash) {
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
	void closeCommunication() {
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
		if (!clientFlag) {
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
		notifyComunicationChange();
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
	public HashSet<SocketCommunication> getCommunications() {
		return mCommunications;
	}

	@Override
	public void OnCommunicationEstablished(SocketCommunication communication) {
		synchronized (mCommunications) {
			mCommunications.add(communication);
			notifyComunicationChange();
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
		// Call all listeners to receive the message.
		for (OnCommunicationListener listener : mOnCommunicationListeners) {
			listener.onReceiveMessage(msg, socketCommunication);
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
	private void notifyComunicationChange() {
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
}
