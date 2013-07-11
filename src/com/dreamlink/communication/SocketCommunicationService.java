package com.dreamlink.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import com.dreamlink.aidl.Communication;
import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;

public class SocketCommunicationService extends Service {
	SocketCommunicationManager socketCommunicationManager;
	SocketCommunicationMananerRemote remote = new SocketCommunicationMananerRemote();

	private class SocketCommunicationMananerRemote extends Communication.Stub {

		/**
		 * set the callback method,must use. the NotifyListener implements on
		 * client
		 * */
		@Override
		public void registListenr(OnCommunicationListenerExternal lis, int appid)
				throws RemoteException {
			socketCommunicationManager.registerOnCommunicationListenerExternal(
					lis, appid);
		}

		/** if user is null ,mean send all */
		@Override
		public void sendMessage(byte[] msg, int appID, User user)
				throws RemoteException {
			// TODO Auto-generated method stub
			if (user == null) {
				socketCommunicationManager.sendMessageToAll(msg, appID);
			} else {
				socketCommunicationManager
						.sendMessageToSingle(msg, user, appID);
			}
		}

		@Override
		public List<User> getAllUser() throws RemoteException {
			// TODO Auto-generated method stub
			UserManager userManager = UserManager.getInstance();
			ArrayList<User> list = new ArrayList<User>();
			Map<Integer, User> map = userManager.getAllUser();
			for (Map.Entry<Integer, User> entry : map.entrySet()) {
				if (entry.getValue().getUserID() == UserManager.getInstance()
						.getLocalUser().getUserID())
					continue;
				list.add(entry.getValue());
			}
			return list;
		}

		@Override
		public void unRegistListenr(OnCommunicationListenerExternal lis)
				throws RemoteException {
			// TODO Auto-generated method stub
			socketCommunicationManager
					.unregisterOnCommunicationListenerExternal(lis);
		}

		@Override
		public User getLocalUser() throws RemoteException {
			// TODO Auto-generated method stub
			return UserManager.getInstance().getLocalUser();
		}

		@Override
		public User setLocalUser() throws RemoteException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		socketCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		return remote;
	}

	@Override
	public boolean onUnbind(Intent intent) {

		return super.onUnbind(intent);
	}

}
