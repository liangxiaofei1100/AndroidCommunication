package com.dreamlink.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;

import com.dreamlink.communication.ui.history.HistoryInfo;
import com.dreamlink.communication.ui.history.HistoryManager;
import com.dreamlink.communication.util.Log;

/**
 * Create server socket to send file. Send file to the connected client.
 * 
 */
public class FileSender {
	private static final String TAG = "FileSenderByYuri";
	/** 3 minutes time out. */
	private static final int SEND_SOCKET_TIMEOUT = 3 * 60 * 1000;

	private OnFileSendListener mListener;
	private File mSendFile;
	private HistoryInfo mSendHistoryInfo;
	private ServerSocket mServerSocket;

	private SendHandlerThread mHandlerThread;

	private static final int MSG_UPDATE_PROGRESS = 1;
	private static final int MSG_FINISH = 2;
	private Handler mHandler;

	private static final String KEY_SENT_BYTES = "KEY_SENT_BYTES";
	private static final String KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES";

	private static final int FINISH_RESULT_SUCCESS = 1;
	private static final int FINISH_RESULT_FAIL = 2;

	/**
	 * Create file send server and send file to the first connected client.
	 * After the file is sent, the server is closed.
	 * 
	 * @param file
	 * @param listener
	 * @return The server socket port.
	 */
	public int sendFile(HistoryInfo historyInfo, OnFileSendListener listener) {
		mListener = listener;
//		mSendFile = file;
		mSendHistoryInfo = historyInfo;

		mServerSocket = createServerSocket();
		if (mServerSocket == null) {
			Log.e(TAG, "sendFile() file: " + historyInfo.getFileInfo().getFilePath()
					+ ", fail. Create server socket error");
			return -1;
		}
		
		FileSenderThread fileSenderThread = new FileSenderThread();
		fileSenderThread.start();

		mHandlerThread = new SendHandlerThread("HandlerThread-FileSender");
		mHandlerThread.start();

		mHandler = new Handler(mHandlerThread.getLooper(), mHandlerThread);

		return mServerSocket.getLocalPort();
	}

	/**
	 * Get an available port.
	 * 
	 * @return
	 */
	private ServerSocket createServerSocket() {
		for (int port : SocketPort.FILE_TRANSPORT_PROT) {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				Log.d(TAG, "Create port successs. port number: " + port);
				return serverSocket;
			} catch (IOException e) {
				Log.d(TAG, "The server port is in use. port number: " + port);
			}
		}
		return null;
	}

	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	class FileSenderThread extends Thread {

		@Override
		public void run() {
			try {
				mServerSocket.setSoTimeout(SEND_SOCKET_TIMEOUT);
				Socket client = mServerSocket.accept();
				Log.d(TAG, "Client ip: "
						+ client.getInetAddress().getHostAddress());
				Log.d(TAG, "Server: connection done");
				OutputStream outputStream = client.getOutputStream();
				Log.d(TAG, "server: copying files " + mSendHistoryInfo.getFileInfo().getFilePath());
				copyFile(mSendHistoryInfo, outputStream);
				mServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "FileSenderThread " + e.toString());
			} catch (Exception e) {
				Log.e(TAG, "FileSenderThread " + e.toString());
			}
			Log.d(TAG, "FileSenderThread file: [" + mSendHistoryInfo.getFileInfo().getFileName()
					+ "] finished");
		}
	}

	class SendHandlerThread extends HandlerThread implements Callback {

		public SendHandlerThread(String name) {
			super(name);
		}

		@Override
		public boolean handleMessage(Message msg) {

			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
//				Bundle data = msg.getData();
//				long sentBytes = data.getLong(KEY_SENT_BYTES);
//				long totalBytes = data.getLong(KEY_TOTAL_BYTES);
//				if (mListener != null) {
//					mListener.onSendProgress(sentBytes, totalBytes);
//				}
				if (mListener != null) {
					mListener.onSendProgress(mSendHistoryInfo);
				}
				break;
			case MSG_FINISH:
				if (mListener != null) {
					if (msg.arg1 == FINISH_RESULT_SUCCESS) {
						mListener.onSendFinished(true);
					} else {
						mListener.onSendFinished(false);
					}
				}

				// Quit the HandlerThread
				quit();
				break;
			default:
				break;
			}
			return true;
		}

	}

	private void copyFile(HistoryInfo historyInfo, OutputStream out) throws FileNotFoundException {
		//set status
		historyInfo.setStatus(HistoryManager.STATUS_SENDING);
		InputStream inputStream = new FileInputStream(historyInfo.getFileInfo().getFilePath());
		byte buf[] = new byte[4096];
		int len;
		double sendBytes = 0;
		long start = System.currentTimeMillis();
//		long totalBytes = mSendFile.length();
		double totalBytes = historyInfo.getMax();
//		int lastProgress = 0;
//		int currentProgress = 0;
		double lastProgress = 0;
		double currentProgress = 0;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);
				sendBytes += len;
				currentProgress = (sendBytes / totalBytes) * 100;
				if (lastProgress != currentProgress) {
					lastProgress = currentProgress;
					mSendHistoryInfo.setProgress(sendBytes);
					notifiyProgress();
				}
			}

			historyInfo.setStatus(HistoryManager.STATUS_SEND_SUCCESS);
			notifyFinish(true);

			out.close();
			inputStream.close();
		} catch (IOException e) {
			historyInfo.setStatus(HistoryManager.STATUS_SEND_FAIL);
			notifyFinish(false);
			Log.d(TAG, e.toString());
		}
		long time = System.currentTimeMillis() - start;
		Log.d(TAG, "Total size = " + sendBytes + "bytes time = " + time
				+ ", speed = " + (sendBytes / time) + "KB/s");
	}
	
	private void notifiyProgress(){
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_PROGRESS;
		mHandler.sendMessage(message);
	}

	private void notifyProgress(long sentBytes, long totalBytes) {
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_PROGRESS;
		Bundle data = new Bundle();
		data.putLong(KEY_SENT_BYTES, sentBytes);
		data.putLong(KEY_TOTAL_BYTES, totalBytes);
		message.setData(data);
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
	 * Callback to get the send status.
	 * 
	 */
	public interface OnFileSendListener {
		/**
		 * Every send 1 percent of file, this method is invoked.
		 */
		void onSendProgress(HistoryInfo historyInfo);

		/**
		 * The file is sent.
		 * 
		 * @param success
		 */
		void onSendFinished(boolean success);
	}
}
