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

public class ComService extends Service implements OnCommunicationListener {
	SocketCommunicationManager socketCommunicationManager;
	private NotifyListener listener;

	private class Test extends Communication.Stub {
		/** just test method ,not use */
		@Override
		public String getCommunicationManager() throws RemoteException {
			// TODO Auto-generated method stub
			byte[] b = "arbiterliu".getBytes();
			listener.getMessage(b);
			return "DreamLink----------------------------------"
					+ Process.myPid();
		}

		/**
		 * set the callback method,must use. the NotifyListener implements on
		 * client
		 * */
		@Override
		public void setListenr(NotifyListener lis) throws RemoteException {
			// TODO Auto-generated method stub
			listener = lis;
		}

		/** sent the msg to communication */
		@Override
		public void setMessage(byte[] msg) throws RemoteException {
			// TODO Auto-generated method stub

			socketCommunicationManager.sendMessage(msg, 0);
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		socketCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		socketCommunicationManager.registered(this);
		return new Test();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		socketCommunicationManager.unregistered(this);
		return super.onUnbind(intent);
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication communication) {
		// TODO Auto-generated method stub
		try {
			listener.getMessage(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyConnectChanged() {
		List<Arg> list = new ArrayList<Arg>();
		Vector<SocketCommunication> v = socketCommunicationManager
				.getCommunications();
		for (SocketCommunication c : v) {
			Arg a = new Arg();
			a.userID = (int) c.getId();
			a.userName = c.getName();
			a.userIP = c.getConnectIP().getHostAddress();
			list.add(a);
		}
		try {
			listener.notifyUserChanged(list);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
