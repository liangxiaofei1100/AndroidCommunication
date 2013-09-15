package com.dreamlink.communication.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.app.AppInfo;

public class DreamUtil {
	
	/**my app 's install path*/
	public static String package_source_dir;
	/**
	 * 插入一个数据到已经排好序的list中
	 * @param list 已经排好序的list
	 * @param appEntry 要插入的数据
	 * @return 将要插入的位置
	 */
	public static int getInsertIndex(List<AppInfo> list, AppInfo appEntry){
		Collator sCollator = Collator.getInstance();
		for (int i = 0; i < list.size(); i++) {
			int ret = sCollator.compare(appEntry.getLabel(), list.get(i).getLabel());
			if (ret <=0 ) {
				return i;
			}
		}
		return list.size();
	}
	
	/**
	 * byte convert
	 * @param size like 3232332
	 * @return like 3.23M
	 */
//	public static String getFormatSize(long size){
//		if (size >= 1024 * 1024 * 1024){
//			Double dsize = (double) (size / (1024 * 1024 * 1024));
//			return new DecimalFormat("#.00").format(dsize) + "G";
//		}else if (size >= 1024 * 1024) {
//			Double dsize = (double) (size / (1024 * 1024));
//			return new DecimalFormat("#.00").format(dsize) + "M";
//		}else if (size >= 1024) {
//			Double dsize = (double) (size / 1024);
//			return new DecimalFormat("#.00").format(dsize) + "K";
//		}else {
//			return String.valueOf((int)size) + "B";
//		}
//	}
	
	public static String getFormatSize(double size){
		if (size >= 1024 * 1024 * 1024){
			Double dsize = size / (1024 * 1024 * 1024);
			return new DecimalFormat("#.00").format(dsize) + "G";
		}else if (size >= 1024 * 1024) {
			Double dsize = size / (1024 * 1024);
			return new DecimalFormat("#.00").format(dsize) + "M";
		}else if (size >= 1024) {
			Double dsize = size / 1024;
			return new DecimalFormat("#.00").format(dsize) + "K";
		}else {
			return String.valueOf((int)size) + "B";
		}
	}
	
	/**get app install date*/
	public static String getFormatDate(long date){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = format.format(new Date(date));
		return dateString;
	}
	
	/** 
     * 格式化时间，将毫秒转换为分:秒格式 
     * @param time audio/video time like 12323312
     * @return the format time string like 00:12:23
     */  
	public static String mediaTimeFormat(long duration) {
		long hour = duration / (60 * 60 * 1000);
		String min = duration % (60 * 60 * 1000) / (60 * 1000) + "";
		String sec = duration % (60 * 60 * 1000) % (60 * 1000) + "";

		if (min.length() < 2) {
			min = "0" + duration / (1000 * 60) + "";
		}

		if (sec.length() == 4) {
			sec = "0" + sec;
		} else if (sec.length() == 3) {
			sec = "00" + sec;
		} else if (sec.length() == 2) {
			sec = "000" + sec;
		} else if (sec.length() == 1) {
			sec = "0000" + sec;
		}

		if (hour == 0) {
			return min + ":" + sec.trim().substring(0, 2);
		} else {
			String hours = "";
			if (hour < 10) {
				hours = "0" + hour;
			} else {
				hours = hours + "";
			}
			return hours + ":" + min + ":" + sec.trim().substring(0, 2);
		}
	}
	
	public static void showInfoDialog(Context context, String info){
		new AlertDialog.Builder(context)
		.setTitle(R.string.menu_info)
		.setMessage(info)
		.setPositiveButton(android.R.string.ok, null)
		.create().show();
	}
	
	/**set dialog dismiss or not*/
	public static void setDialogDismiss(DialogInterface dialog, boolean dismiss){
		try {
			Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, dismiss);
			dialog.dismiss();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
