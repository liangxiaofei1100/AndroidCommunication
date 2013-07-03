package com.dreamlink.aidl;

import com.dreamlink.aidl.NotifyListener;

interface Communication{
	void setListenr(NotifyListener lis);
	void sendMessage(in byte[] msg);
}