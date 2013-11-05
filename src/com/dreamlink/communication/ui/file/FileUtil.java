package com.dreamlink.communication.ui.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

import com.dreamlink.communication.util.Log;

public class FileUtil {
	private static final String TAG = "";

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
	
	/**
	 * bytes tp chars
	 * 
	 * @param bytes
	 * @return
	 */
	public static char[] getChars(byte[] bytes) {
		Charset cs = Charset.forName("UTF-8");
		ByteBuffer bb = ByteBuffer.allocate(bytes.length);
		bb.put(bytes);
		bb.flip();
		CharBuffer cb = cs.decode(bb);
		return cb.array();
	}
	
	/**
	 * io copy
	 * 
	 * @param srcPath
	 *           src file path
	 * @param desPath
	 *           des file path
	 * @return
	 * @throws Exception
	 */
	public static void fileStreamCopy(String srcPath, String desPath) throws IOException{
		Log.d(TAG, "fileStreamCopy.src:" + srcPath);
		Log.d(TAG, "fileStreamCopy.dec:" + desPath);
		File files = new File(desPath);// 创建文件
		FileOutputStream fos = new FileOutputStream(files);
		byte buf[] = new byte[128];
		InputStream fis = new BufferedInputStream(new FileInputStream(srcPath),
				8192 * 4);
		do {
			int read = fis.read(buf);
			if (read <= 0) {
				break;
			}
			fos.write(buf, 0, read);
		} while (true);
		fis.close();
		fos.close();
	}

}
