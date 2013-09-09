package com.dreamlink.communication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.net.wifi.WifiConfiguration.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Handler.Callback;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.history.HistoryInfo;
import com.dreamlink.communication.ui.history.HistoryManager;
import com.dreamlink.communication.util.Log;

/**
 * Connect to server socket and get the file from server.
 * 
 */
public class FileReceiver {
	private static final String TAG = "FileReceiver";
	private static final int SOCKET_TIMEOUT = 5000;
	private User mSendUser;
	private InetAddress mServerInetAddress;
	private int mServerPort;
	private FileTransferInfo mFileTransferInfo;
	private File mReceivedFile;
	private HistoryInfo mReceivedHistoryInfo;
	private OnReceiveListener mListener;

	private ReceiveHandlerThread mHandlerThread;

	private static final int MSG_UPDATE_PROGRESS = 1;
	private static final int MSG_FINISH = 2;
	private static final int MSG_STOP_RECEIVE = 3;
	private Handler mHandler;

	private static final String KEY_RECEIVE_BYTES = "KEY_RECEIVE_BYTES";
	private static final String KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES";

	private static final int FINISH_RESULT_SUCCESS = 1;
	private static final int FINISH_RESULT_FAIL = 2;

	/** The socket to recieve file. */
	private Socket mSocket;

	public FileReceiver(User sendUser, byte[] serverAddress, int serverPort,
			FileTransferInfo fileTransferInfo) {
		mSendUser = sendUser;
		try {
			mServerInetAddress = InetAddress.getByAddress(serverAddress);
		} catch (UnknownHostException e) {
			Log.e(TAG, "FileReceiver() get server addresss error. " + e);
		}
		mServerPort = serverPort;
		mFileTransferInfo = fileTransferInfo;
	}

	/**
	 * @return the SendUser
	 */
	public User getSendUser() {
		return mSendUser;
	}

	/**
	 * @return the FileInfo
	 */
	public FileTransferInfo getFileTransferInfo() {
		return mFileTransferInfo;
	}

	/**
	 * Connect the server and receive file from server.
	 * 
	 * @param receivedFile
	 *            the file to save.
	 * @param listener
	 */
	public void receiveFile(HistoryInfo historyInfo, OnReceiveListener listener) {
		Log.d(TAG,
				"receiveFile() received file " + historyInfo.getFileInfo().getFilePath());
//		mReceivedFile = receivedFile;
		mReceivedHistoryInfo = historyInfo;
		mListener = listener;
		if (mServerInetAddress == null) {
			Log.e(TAG, "receiveFile() Server Address is null.");
			return;
		}

		FileReceiverThread fileReceiverThread = new FileReceiverThread();
		fileReceiverThread.start();

		mHandlerThread = new ReceiveHandlerThread("ReceiveHandlerThread");
		mHandlerThread.start();

		mHandler = new Handler(mHandlerThread.getLooper(), mHandlerThread);
	}

	/**
	 * Stop receiving.
	 */
	public void stopReceive() {
		if (mHandler != null) {
			mHandler.sendEmptyMessage(MSG_STOP_RECEIVE);
		}
	}

	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	class FileReceiverThread extends Thread {

		@Override
		public void run() {
			Log.d(TAG, "FileReceiverThread run()");

			mSocket = new Socket();
			try {
				Log.d(TAG, "Opening client socket - server address: "
						+ mServerInetAddress.getHostAddress() + ", port : "
						+ mServerPort);
				mSocket.bind(null);
				mSocket.connect((new InetSocketAddress(mServerInetAddress,
						mServerPort)), SOCKET_TIMEOUT);
				Log.d(TAG, "Client socket - " + mSocket.isConnected());
				InputStream inputStream = mSocket.getInputStream();
//				copyFile(inputStream, new FileOutputStream(mReceivedFile));
				copyFile(inputStream);
				Log.d(TAG, "Client: Data written");
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			} finally {
				if (mSocket != null) {
					if (mSocket.isConnected()) {
						try {
							mSocket.close();
						} catch (IOException e) {
							// Give up
							e.printStackTrace();
						}
					}
				}
			}
//			Log.d(TAG, "FileReceiverThread file: [" + mReceivedFile.getName()
//					+ "] finished.");
			Log.d(TAG, "FileReceiverThread file: [" + mReceivedHistoryInfo.getFileInfo().getFileName()
					+ "] finished.");
		}
	}

//	private void copyFile(InputStream inputStream, OutputStream out) throws FileNotFoundException {
	private void copyFile(InputStream inputStream) throws FileNotFoundException {
		mReceivedHistoryInfo.setStatus(HistoryManager.STATUS_RECEIVING);
		OutputStream out = new FileOutputStream(mReceivedHistoryInfo.getFileInfo().getFilePath());
		byte buf[] = new byte[4096];
		int len;
		double receiveBytes = 0;
		double totalBytes = mFileTransferInfo.getFileSize();
		long start = System.currentTimeMillis();
		double lastProgress = 0;
		double currentProgress = 0;
		
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);
				receiveBytes += len;
				currentProgress = (int) (((double) receiveBytes / totalBytes) * 100);
//				currentProgress = (receiveBytes / totalBytes) * 100;
				if (lastProgress != currentProgress) {
					lastProgress = currentProgress;
//					notifyProgress(receiveBytes, totalBytes);
					mReceivedHistoryInfo.setProgress(receiveBytes);
					notifyProgress();
				}
			}
			mReceivedHistoryInfo.setStatus(HistoryManager.STATUS_RECEIVE_SUCCESS);
			notifyFinish(true);
			out.close();
			inputStream.close();
		} catch (IOException e) {
			mReceivedHistoryInfo.setStatus(HistoryManager.STATUS_RECEIVE_FAIL);
			notifyFinish(false);
			Log.d(TAG, e.toString());
		}
		long time = System.currentTimeMillis() - start;
		Log.d(TAG, "Total size = " + receiveBytes + "bytes time = " + time
				+ ", speed = " + (receiveBytes / time) + "KB/s");
	}

	class ReceiveHandlerThread extends HandlerThread implements Callback {

		public ReceiveHandlerThread(String name) {
			super(name);
		}

		@Override
		public boolean handleMessage(Message msg) {

			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				Bundle data = msg.getData();
				long receiveBytes = data.getLong(KEY_RECEIVE_BYTES);
				long totalBytes = data.getLong(KEY_TOTAL_BYTES);
				if (mListener != null) {
//					mListener.onReceiveProgress(receiveBytes, totalBytes);
					mListener.onReceiveProgress(mReceivedHistoryInfo);
				}
				break;
			case MSG_FINISH:
				if (mListener != null) {
					if (msg.arg1 == FINISH_RESULT_SUCCESS) {
						mListener.onReceiveFinished(true);
					} else {
						mListener.onReceiveFinished(false);
					}
				}
				// Quit the HandlerThread.
				quit();
				mHandler = null;
				break;

			case MSG_STOP_RECEIVE:
				Log.d(TAG, "MSG_STOP_RECEIVE");
				if (mSocket != null && mSocket.isConnected()) {
					try {
						mSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				break;
			default:
				break;
			}
			return true;
		}

	}

	private void notifyProgress(long sentBytes, long totalBytes) {
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_PROGRESS;
		Bundle data = new Bundle();
		data.putLong(KEY_RECEIVE_BYTES, sentBytes);
		data.putLong(KEY_TOTAL_BYTES, totalBytes);
		message.setData(data);
		mHandler.sendMessage(message);
	}
	
	public void notifyProgress(){
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_PROGRESS;
		mHandler.sendMessage(message);
	}

	private void notifyFinish(boolean result) {
		Message message = mHandler.obtainMessage();
		message.what = MSG_FINISH;
		if (result) {
			message.arg1 = FINISH_RESULT_SUCCESS;
		} else {
			message.arg1 = FINISH_RESULT_FAIL;
		}
		mHandler.sendMessage(message);
	}

	/**
	 * Callback to get the receive status.
	 * 
	 */
	public interface OnReceiveListener {
		/**
		 * Every receive 1 percent of file, this method is invoked.
		 * 
		 * @param receivedBytes
		 * @param totalBytes
		 */
		void onReceiveProgress(HistoryInfo historyInfo);

		/**
		 * The file is received.
		 * 
		 * @param success
		 */
		void onReceiveFinished(boolean success);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FileReceiver [mSendUser=" + mSendUser + ", mServerInetAddress="
				+ mServerInetAddress + ", mServerPort=" + mServerPort
				+ ", mFileInfo=" + mFileTransferInfo + "]";
	}

}
