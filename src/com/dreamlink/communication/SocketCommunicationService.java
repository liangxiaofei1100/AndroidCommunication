package com.dreamlink.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.dreamlink.aidl.Arg;
import com.dreamlink.aidl.Communication;
import com.dreamlink.aidl.NotifyListener;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

public class SocketCommunicationService extends Service implements
		OnCommunicationListener {
	SocketCommunicationManager socketCommunicationManager;
	private NotifyListener callBackListener;

	private class SocketCommunicationMananerRemote extends Communication.Stub {

		/**
		 * set the callback method,must use. the NotifyListener implements on
		 * client
		 * */
		@Override
		public void setListenr(NotifyListener lis) throws RemoteException {
			callBackListener = lis;
		}

		/** sent the msg to communication */
		@Override
		public void sendMessage(byte[] msg) throws RemoteException {
			socketCommunicationManager.sendMessage(msg, 0);
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		socketCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		socketCommunicationManager.registered(this);
		return new SocketCommunicationMananerRemote();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		socketCommunicationManager.unregistered(this);
		return super.onUnbind(intent);
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication communication) {
		try {
			callBackListener.onReceiveMessage(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSendResult(byte[] msg) {

	}

	@Override
	public void notifyConnectChanged() {
		List<Arg> list = new ArrayList<Arg>();
		Vector<SocketCommunication> vector = socketCommunicationManager
				.getCommunications();
		for (SocketCommunication com : vector) {
			Arg arg = new Arg();
			arg.userID = (int) com.getId();
			arg.userName = com.getName();
			arg.userIP = com.getConnectIP().getHostAddress();
			list.add(arg);
		}
		try {
			callBackListener.notifyConnectChanged(list);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
