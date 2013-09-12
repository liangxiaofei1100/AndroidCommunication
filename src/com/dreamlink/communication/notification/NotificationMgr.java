package com.dreamlink.communication.notification;

import com.dreamlink.communication.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationMgr {
	private Context mContext;
	
	private static final int NOTIFICATION_ID = 111;
	
	public static final int STATUS_DEFAULT = 0;
	public static final int STATUS_UNCONNECTED = 1;
	public static final int STATUS_CONNECTED = 2;
	
	private int mPreStatus = -1;
	private NotificationManager mNotificationManager = null;
	public NotificationMgr(Context context){
		mContext = context;
		mNotificationManager = (NotificationManager) mContext 
                .getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	public void showNotificaiton(int status){
		mPreStatus = status;
		
		String label = mContext.getString(R.string.app_name);
		String runningString = mContext.getString(R.string.running);
		String unconnected = mContext.getString(R.string.unconnected);
		String connected = mContext.getString(R.string.connected);
		String title = "";
		switch (status) {
		case STATUS_UNCONNECTED:
			 title = runningString + unconnected;
			break;
		case STATUS_CONNECTED:
			 title = runningString + connected;
			break;
		}
		Notification notification = new Notification(R.drawable.logo, title, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_NO_CLEAR; 
		notification.flags |= Notification.FLAG_ONGOING_EVENT; 
		
		//点击通知栏后，返回原Activity
		 Intent intent = new Intent(mContext, mContext.getClass()); 
		 //设置启动activity的启动模式，FLAG_ACTIVITY_SINGLE_TOP作用是在返回的时候用原来的activity的实例而不是建立新的 
		 intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); 
		 PendingIntent pi = PendingIntent.getActivity(mContext, 0, intent, 
                 PendingIntent.FLAG_UPDATE_CURRENT); 
		 notification.setLatestEventInfo(mContext, label, title, pi);
		 mNotificationManager.notify(NOTIFICATION_ID, notification); 
	}
	
	public void updateNotification(int status){
		if (status == mPreStatus) {
			return;
		}else if (status == STATUS_DEFAULT) {
			cancelNotification();
			showNotificaiton(mPreStatus);
		}else {
			cancelNotification();
			showNotificaiton(status);
		}
	}
	
	public void cancelNotification() {
		mNotificationManager.cancel(NOTIFICATION_ID);
	} 
}
