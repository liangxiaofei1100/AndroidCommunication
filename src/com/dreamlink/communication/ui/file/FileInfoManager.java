package com.dreamlink.communication.ui.file;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.dialog.DeleteDialog;
import com.dreamlink.communication.util.Log;

public class FileInfoManager {
	private static final String TAG = FileInfoManager.class.getName();
	public static final int TEXT = 0x01;
	public static final int HTML = 0x02;
	public static final int WORD = 0x03;
	public static final int EXCEL = 0x04;
	public static final int PPT = 0x05;
	public static final int PDF = 0x06;
	public static final int AUDIO = 0x07;
	public static final int VIDEO = 0x08;
	public static final int CHM = 0x09;
	public static final int APK = 0x10;
	public static final int ZIP = 0x11;
	public static final int IMAGE = 0x12;
	public static final int UNKNOW = 0x20;

	public static final int TYPE_DEFAULT = 0x20;//32
	public static final int TYPE_EBOOK = 0x21;
	public static final int TYPE_VIDEO = 0x22;
	public static final int TYPE_DOC = 0x23;
	public static final int TYPE_APK = 0x24;//36
	public static final int TYPE_ZIP = 0x25;
	public static final int TYPE_BIG_FILE = 0x26;
	public static final int TYPE_IMAGE = 0x27;
	public static final int TYPE_AUDIO = 0x28;//40

	private Context context;

	public FileInfoManager(Context context) {
		this.context = context;
	}

	/**
	 * android获取一个用于打开Audio文件的Intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getAudioFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("oneshot", 0);
		intent.putExtra("configchange", 0);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "audio/*");
		return intent;
	}

	/**
	 * 获取一个打开文本文件的Intent
	 * 
	 * @param param
	 *            file path
	 * */
	public static Intent getTextFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "text/plain");
		return intent;
	}

	/**
	 * android获取一个用于打开PPT文件的intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getPptFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
		return intent;
	}

	/**
	 * android获取一个用于打开Excel文件的intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getExcelFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "application/vnd.ms-excel");
		return intent;
	}

	/**
	 * android获取一个用于打开Word文件的intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getWordFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "application/msword");
		return intent;
	}

	/**
	 * android获取一个用于打开PDF文件的intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getPdfFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "application/pdf");
		return intent;
	}

	/**
	 * android获取一个用于打开Image文件的Intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getImageFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "image/*");
		return intent;
	}

	/**
	 * android获取一个用于打开VIDEO文件的Intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getVIDEOFileIntent(String param) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("oneshot", 0);
		intent.putExtra("configchange", 0);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "video/*");
		return intent;
	}

	/**
	 * android获取一个用于打开apk文件的intent
	 * 
	 * @param param
	 *            file path
	 * @return
	 */
	public static Intent getApkFileIntent(String param) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		return intent;
	}

	// 判断文件类型，根据不同类型设置图标
	public FileInfo getFileInfo(File currentFile) {
		Drawable currentIcon = null;
		int fileType = FileInfoManager.TYPE_DEFAULT;
		// 取得文件路径
		String filePath = currentFile.getPath();

		// 根据文件名来判断文件类型，设置不同的图标
		int result = fileFilter(filePath);
		switch (result) {
		case TEXT:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_txt);
			fileType = FileInfoManager.TYPE_EBOOK;
			break;
		// case HTML:
		// currentIcon = context.getResources().getDrawable(R.drawable.webtext);
		// break;
		case IMAGE:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_image);
			fileType = FileInfoManager.TYPE_IMAGE;
			break;
		case AUDIO:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_audio);
			fileType = FileInfoManager.TYPE_AUDIO;
			break;
		case VIDEO:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_video);
			fileType = FileInfoManager.TYPE_VIDEO;
			break;
		case WORD:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_doc);
			fileType = FileInfoManager.TYPE_DOC;
			break;
		case PPT:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_ppt);
			fileType = FileInfoManager.TYPE_DOC;
			break;
		case EXCEL:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_xls);
			fileType = FileInfoManager.TYPE_DOC;
			break;
		case PDF:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_pdf);
			fileType = FileInfoManager.TYPE_DOC;
			break;
		case ZIP:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_rar);
			fileType = FileInfoManager.TYPE_ZIP;
			break;
		case APK:
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_apk);
			fileType = FileInfoManager.TYPE_APK;
			break;
		default:
			// 默认
			currentIcon = context.getResources().getDrawable(
					R.drawable.icon_file);
			break;
		}
		FileInfo fileInfo = new FileInfo(currentFile.getName());
		fileInfo.fileSize = currentFile.length();
		fileInfo.fileDate = currentFile.lastModified();
		fileInfo.filePath = currentFile.getAbsolutePath();
		fileInfo.isDir = false;
		fileInfo.type = fileType;
		fileInfo.icon = currentIcon;
		return fileInfo;
	}

	/**
	 * 未安装的程序通过apk文件获取icon
	 * 
	 * @param path
	 *            apk文件路径
	 * @return apk的icon
	 */
	public Drawable getApkIcon(String path) {
		String apkPath = path; // apk 文件所在的路径
		String PATH_PackageParser = "android.content.pm.PackageParser";
		String PATH_AssetManager = "android.content.res.AssetManager";
		try {
			Class<?> pkgParserCls = Class.forName(PATH_PackageParser);
			Class<?>[] typeArgs = { String.class };
			Constructor<?> pkgParserCt = pkgParserCls.getConstructor(typeArgs);
			Object[] valueArgs = { apkPath };
			Object pkgParser = pkgParserCt.newInstance(valueArgs);

			DisplayMetrics metrics = new DisplayMetrics();
			metrics.setToDefaults();

			typeArgs = new Class<?>[] { File.class, String.class,
					DisplayMetrics.class, int.class };

			Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod(
					"parsePackage", typeArgs);

			valueArgs = new Object[] { new File(apkPath), apkPath, metrics, 0 };
			Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser,
					valueArgs);

			Field appInfoFld = pkgParserPkg.getClass().getDeclaredField(
					"applicationInfo");

			ApplicationInfo info = (ApplicationInfo) appInfoFld
					.get(pkgParserPkg);

			Class<?> assetMagCls = Class.forName(PATH_AssetManager);
			Object assetMag = assetMagCls.newInstance();
			typeArgs = new Class[1];
			typeArgs[0] = String.class;

			Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod(
					"addAssetPath", typeArgs);
			valueArgs = new Object[1];
			valueArgs[0] = apkPath;

			assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);

			Resources res = context.getResources();

			typeArgs = new Class[3];
			typeArgs[0] = assetMag.getClass();
			typeArgs[1] = res.getDisplayMetrics().getClass();
			typeArgs[2] = res.getConfiguration().getClass();

			Constructor<Resources> resCt = Resources.class
					.getConstructor(typeArgs);

			valueArgs = new Object[3];

			valueArgs[0] = assetMag;
			valueArgs[1] = res.getDisplayMetrics();
			valueArgs[2] = res.getConfiguration();
			res = (Resources) resCt.newInstance(valueArgs);

			if (info != null) {
				if (info.icon != 0) {
					Drawable icon = res.getDrawable(info.icon);
					return icon;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return context.getResources().getDrawable(
				R.drawable.icon_apk);
	}

	public int fileFilter(String filepath) {
		// 首先取得文件名
		String fileName = new File(filepath).getName();
		int ret;

		if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingTxt))) {
			// text
			ret = TEXT;
		}
		// else if (checkEndsWithInStringArray(fileName,
		// context.getResources().getStringArray(R.array.fileEndingWebText))) {
		// //html ...
		// ret = HTML;
		// }
		else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingImage))) {
			// Images
			ret = IMAGE;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingAudio))) {
			// audios
			ret = AUDIO;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingVideo))) {
			// videos
			ret = VIDEO;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingApk))) {
			// apk
			ret = APK;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingDoc))) {
			// word
			ret = WORD;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingPpt))) {
			// ppt
			ret = PPT;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingExcel))) {
			// excel
			ret = EXCEL;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingPackage))) {
			// packages
			ret = ZIP;
		} else if (checkEndsWithInStringArray(fileName, context.getResources()
				.getStringArray(R.array.fileEndingPdf))) {
			// pdf
			ret = PDF;
		} else {
			ret = UNKNOW;
		}

		return ret;
	}

	// 通过文件名判断是什么类型的文件
	private boolean checkEndsWithInStringArray(String checkItsEnd,
			String[] fileEndings) {
		String str = checkItsEnd.toLowerCase();
		for (String aEnd : fileEndings) {
			if (str.endsWith(aEnd))
				return true;
		}
		return false;
	}

	/**
	 * open file
	 * 
	 * @param filePath
	 *            file path
	 */
	public void openFile(String filePath) {
		Intent intent = null;

		// 根据不同的文件，执行不同的打开方式
		// 根据文件名来判断文件类型，设置不同的图标
		int result = fileFilter(filePath);
		switch (result) {
		case TEXT:
			intent = getTextFileIntent(filePath);
			break;
		case IMAGE:
			intent = getImageFileIntent(filePath);
			break;
		case AUDIO:
			intent = getAudioFileIntent(filePath);
			break;
		case VIDEO:
			intent = getVIDEOFileIntent(filePath);
			break;
		case WORD:
			intent = getWordFileIntent(filePath);
			break;
		case PPT:
			intent = getPptFileIntent(filePath);
			break;
		case EXCEL:
			intent = getExcelFileIntent(filePath);
			break;
		case PDF:
			intent = getPdfFileIntent(filePath);
			break;
		case ZIP:
			// 待实现
			break;
		case APK:
			intent = getApkFileIntent(filePath);
			break;
		default:
			// 默认
			break;
		}
		// open
		if (intent != null) {
			context.startActivity(intent);
		} else {
			Toast.makeText(context, "Can not find app to open this file",
					Toast.LENGTH_SHORT).show();
		}
	}

	public boolean deleteFile(String filePath) {
		File file = new File(filePath);
		return deleteFile(file);
	}

	/**
	 * API11 以后多了一个MediaStore.Files这个类， 应该是Google为了方便管理，将系统中的所有文件都保存到了一个表files中
	 * 这样的话，删除也方便 而2.3的话，如果要删除你得去不同的表中删除，比如Audio，Images，Video 3.0以下系统可以使用该方法
	 * 
	 * @param path
	 */
	public boolean deleteFileInMediaStore(Uri uri, String path) {
		if (TextUtils.isEmpty(path)) {
			return false;
		}
		
		if (!deleteFile(path)) {
			return false;
		}

		String where = MediaStore.Audio.Media.DATA + "=?";
		String[] whereArgs = new String[] { path };
		ContentResolver cr = context.getContentResolver();
		try {
			cr.delete(uri, where, whereArgs);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, "Error in delete file in media store:" + e.toString());
		}
		
		return true;
	}

	/**
	 * 3.0以上系统，才可以使用该方法，删除多媒体文件
	 */
	public boolean deleteFileInMediaStore(String path) {

		if (TextUtils.isEmpty(path)) {
			return false;
		}
		
		if (!deleteFile(path)) {
			return false;
		}
		
		Uri uri = null;
		// 不能用Files这个类，这个类API11 以后才支持
		// Uri uri = MediaStore.Files.getContentUri("external");
		String where = MediaStore.Files.FileColumns.DATA + "=?";
		String[] whereArgs = new String[] { path };
		ContentResolver cr = context.getContentResolver();
		try {
			cr.delete(uri, where, whereArgs);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, "Error in delete file in media store:" + e.toString());
		}
		
		return true;
	}

	public boolean deleteFile(File file) {
		if (!file.exists()) {
			return false;
		}

		return file.delete();
	}
	
	/**
	 * rename the file
	 * @param oldFile
	 * @param newName
	 */
	public void rename(File oldFile, String newName){
			String parentPath = oldFile.getParent(); // 取得上一级目录
			File newPath = new File(parentPath + "/" + newName);
			oldFile.renameTo(newPath);
	}

	public void showInfoDialog(FileInfo fileInfo) {
		String info = getFileInfo(fileInfo);
		DreamUtil.showInfoDialog(context, info);
	}

	private String getFileInfo(FileInfo fileInfo) {
		String path = fileInfo.filePath.substring(0, fileInfo.filePath.lastIndexOf("/"));
		String result = "";
		result = "名称:" + fileInfo.fileName + DreamConstant.ENTER + "类型:"
				+ (fileInfo.isDir ? "文件夹" : "文件") + DreamConstant.ENTER + "位置:"
				+ path + DreamConstant.ENTER + "大小:"
				+ fileInfo.getFormatFileSize() + DreamConstant.ENTER + "修改日期:"
				+ fileInfo.getFormateDate();
		return result;
	}
	
	private static final String kuohu1 = ")";
	private static final String kuohu2 = "(";
	/**
	 * auto rename
	 * @param oldName
	 * @return
	 */
	public static String autoRename(String oldName){
		String newName = "";
		String tempName = "";
		String extensionName = "";
		int index = oldName.lastIndexOf(".");
		if (index == -1) {
			tempName = oldName;
		}else {
			//得到除去扩展名的文件名，如：abc
			tempName = oldName.substring(0, oldName.lastIndexOf("."));
			extensionName =  oldName.substring(index);
		}
		
		//得到倒数第一个括弧的位置
		int kuohuoIndex1 = tempName.lastIndexOf(kuohu1);
		//得到倒数第二个括弧的位置
		int kuohuoIndex2 = tempName.lastIndexOf(kuohu2);
		if (kuohuoIndex1 != tempName.length() - 1) {
			newName = tempName + "(2)" + extensionName;
		}else {
			//得到括弧里面的String
			String str = tempName.substring(kuohuoIndex2 + 1, kuohuoIndex1);
			try {
				int num = Integer.parseInt(str);
				newName =  tempName.substring(0, kuohuoIndex2) + "(" + (num + 1) + ")"+ extensionName;
			} catch (NumberFormatException e) {
				newName = tempName + "(2)" + extensionName;
			}
		}
		return newName;
	}

	private List<NavigationRecord> mNavigationLists = new LinkedList<FileInfoManager.NavigationRecord>();

	public void addNavigationList(NavigationRecord navigationRecord) {
		if (mNavigationLists.size() <= 20) {
			mNavigationLists.add(navigationRecord);
		} else {
			mNavigationLists.remove(0);
			mNavigationLists.add(navigationRecord);
		}
	}

	/** record current path navigation */
	public static class NavigationRecord {
		private String path;
		private int top;
		private FileInfo selectFile;

		public NavigationRecord(String path, int top, FileInfo fileInfo) {
			this.path = path;
			this.top = top;
			this.selectFile = fileInfo;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public int getTop() {
			return top;
		}

		public void setTop(int top) {
			this.top = top;
		}

		public FileInfo getSelectFile() {
			return selectFile;
		}

		public void setSelectFile(FileInfo selectFile) {
			this.selectFile = selectFile;
		}
	}
}
