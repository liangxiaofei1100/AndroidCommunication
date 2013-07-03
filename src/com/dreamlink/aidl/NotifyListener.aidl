package com.dreamlink.aidl;

import com.dreamlink.aidl.Arg;

interface NotifyListener{
	void getMessage(in byte[] msg);
	void notifyUserChanged(in List<Arg> list);
}