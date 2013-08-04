package com.dreamlink.communication.ui;

import java.text.Collator;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.dreamlink.communication.ui.app.AppEntry;

public class DreamUtil {
	/**
	 * 插入一个数据到已经排好序的list中
	 * @param list 已经排好序的list
	 * @param appEntry 要插入的数据
	 * @return 将要插入的位置
	 */
	public static int getInsertIndex(List<AppEntry> list, AppEntry appEntry){
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
	public static String getFormatSize(long size){
		if (size > 1024 * 1024) {
			Double dsize = (double) (size / (1024 * 1024));
			return new DecimalFormat("#.00").format(dsize) + "MB";
		}else if (size > 1024) {
			Double dsize = (double)size / (1024);
			return new DecimalFormat("#.00").format(dsize) + "KB";
		}else {
			return String.valueOf((int)size) + " Bytes";
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
	
}
