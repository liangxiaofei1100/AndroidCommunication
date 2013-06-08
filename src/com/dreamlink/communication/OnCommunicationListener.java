package com.dreamlink.communication;

public interface OnCommunicationListener {

	void onReceiveMessage(byte[] msg, int id);

	void onSendResult(byte[] msg);

	void notifyConnectChanged(SocketCommunication com, boolean addFlag);

}
