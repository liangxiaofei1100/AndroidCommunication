package com.dreamlink.aidl;

import com.dreamlink.aidl.Arg;

interface NotifyListener{
	void onReceiveMessage(in byte[] msg);
	void notifyConnectChanged(in List<Arg> list);
}