package com.dreamlink.communication.fileshare;

import java.text.DecimalFormat;

public class FileUtil {

	/**
	 * convert bytes to KB,MB
	 * @param fileSize
	 * @return like:3.45MB
	 */
	public static String getFormatFileSize(long fileSize){
		if (fileSize > 1024 * 1024) {
			Double dsize = (double) (fileSize / (1024 * 1024));
			return new DecimalFormat("#.00").format(dsize) + "MB";
		}else if (fileSize > 1024) {
			Double dsize = (double)fileSize / (1024);
			return new DecimalFormat("#.00").format(dsize) + "KB";
		}else {
			return String.valueOf((int)fileSize) + " Bytes";
		}
	}
}
