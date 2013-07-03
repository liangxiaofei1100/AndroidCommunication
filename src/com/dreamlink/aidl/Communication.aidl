package com.dreamlink.aidl;

import com.dreamlink.aidl.NotifyListener;

interface Communication{
	String getCommunicationManager();
	void setListenr(NotifyListener lis);
	void setMessage(in byte[] msg);
}