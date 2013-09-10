package com.dreamlink.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.dreamlink.communication.aidl.Communication;
import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;

public class SocketCommunicationService extends Service {
	private SocketCommunicationManager mSocketCommunicationManager;
	private SocketCommunicationMananerRemote mRemote = new SocketCommunicationMananerRemote();
	public static RemoteCallbackList<OnCommunicationListenerExternal> mCallBackList = new RemoteCallbackList<OnCommunicationListenerExternal>();

	private class SocketCommunicationMananerRemote extends Communication.Stub {

		/**
		 * set the callback method,must use. the NotifyListener implements on
		 * client
		 * */
		@Override
		public void registListener(OnCommunicationListenerExternal lis,
				int appid) throws RemoteException {
			if (lis != null)
				mCallBackList.register(lis, appid);
			// socketCommunicationManager.registerOnCommunicationListenerExternal(
			// lis, appid);
		}

		/** if user is null ,mean send all */
		@Override
		public void sendMessage(byte[] msg, int appID, User user)
				throws RemoteException {
			if (user == null) {
				mSocketCommunicationManager.sendMessageToAll(msg, appID);
			} else {
				mSocketCommunicationManager.sendMessageToSingle(msg, user,
						appID);
			}
		}

		@Override
		public List<User> getAllUser() throws RemoteException {
			UserManager userManager = UserManager.getInstance();
			ArrayList<User> list = new ArrayList<User>();
			Map<Integer, User> map = userManager.getAllUser();
			for (Map.Entry<Integer, User> entry : map.entrySet()) {
				list.add(entry.getValue());
			}
			return list;
		}

		@Override
		public void unRegistListener(OnCommunicationListenerExternal lis)
				throws RemoteException {
			if (lis != null)
				mCallBackList.unregister(lis);
			// socketCommunicationManager
			// .unregisterOnCommunicationListenerExternal(lis);
		}

		@Override
		public User getLocalUser() throws RemoteException {
			return UserManager.getInstance().getLocalUser();
		}

		@Override
		public void sendMessageToAll(byte[] msg, int appID)
				throws RemoteException {
			mSocketCommunicationManager.sendMessageToAll(msg, appID);
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		mSocketCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		return mRemote;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		return super.onUnbind(intent);
	}
}
