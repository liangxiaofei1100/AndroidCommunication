package com.dreamlink.communication;

public interface ICommunicate {
	void receiveMessage(byte[] msg,int ID);

	void sendMessage(byte[] msg);
}
