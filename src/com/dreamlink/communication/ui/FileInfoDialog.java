package com.dreamlink.communication.ui;

import com.dreamlink.communication.R;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.ui.app.AppEntry;
import com.dreamlink.communication.ui.image.ImageInfo;
import com.dreamlink.communication.ui.media.MediaInfo;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class FileInfoDialog extends DialogFragment {
	private static final String TAG = "FileInfoDialog";

	private static final String TYPE = "type";

	public static final int APP_INFO = 0;
	public static final int FILE_INFO = 1;
	public static final int MEDIA_INFO = 2;
	public static final int IMAGE_INFO = 3;
	private static Object object;
	private static int mType;

	private static final String ENTER = "\n";

	/**
	 * create confirm dialog instance and you can set some parms
	 */
	public static FileInfoDialog newInstance(Object parm, int type) {
		FileInfoDialog fragment = new FileInfoDialog();
		Bundle bundle = new Bundle();
		bundle.putInt(TYPE, type);
		fragment.setArguments(bundle);
		object = parm;
		return fragment;
	}

	public static FileInfoDialog newInstance(String info) {
		FileInfoDialog fragment = new FileInfoDialog();
		Bundle bundle = new Bundle();
		bundle.putString("info", info);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int type = getArguments().getInt(TYPE);
		// String info = getArguments().getString("info");
		String info = getInfos(type);
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.menu_info)
				.setIcon(android.R.drawable.ic_dialog_info).setMessage(info)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	public String getInfos(int type) {
		String result = "";
		switch (type) {
		case APP_INFO:
			AppEntry appEntry = (AppEntry) object;
			result = "名称:" + appEntry.getLabel() + ENTER + "类型:"
					+ (appEntry.isGameApp() ? "游戏" : "应用") + ENTER + "版本:"
					+ appEntry.getVersion() + ENTER + "包名:"
					+ appEntry.getPackageName() + ENTER + "位置:"
					+ appEntry.getInstallPath() + ENTER + "大小:"
					+ appEntry.getFormatSize() + ENTER + "修改日期:"
					+ appEntry.getDate();
			break;
		case FILE_INFO:
			FileInfo fileInfo = (FileInfo) object;
			result = "名称:" + fileInfo.fileName + ENTER + "类型:"
					+ (fileInfo.isDir ? "文件夹" : "文件") + ENTER + "位置:"
					+ fileInfo.filePath + ENTER + "大小:"
					+ fileInfo.getFormatFileSize() + ENTER + "修改日期:"
					+ fileInfo.getFormateDate();
			break;
		case MEDIA_INFO:
			MediaInfo mediaInfo = (MediaInfo) object;
			result = "名称:" + mediaInfo.getDisplayName() + ENTER + "类型:"
					+ (mediaInfo.isAudio() ? "音频" : "视频") + ENTER + "位置:"
					+ mediaInfo.getUrl() + ENTER + "大小:"
					+ mediaInfo.getFormatSize() + ENTER + "修改日期:"
					+ mediaInfo.getFormatDate();
			break;
		case IMAGE_INFO:
			ImageInfo imageInfo = (ImageInfo) object;
			result = "名称:" + imageInfo.getName() + ENTER + "类型:" + "图片" + ENTER
					+ "位置:" + imageInfo.getPath() + ENTER + "大小:"
					+ imageInfo.getFormatSize() + ENTER + "修改日期:"
					+ imageInfo.getFormatDate();
			break;
		default:
			result = "ERROR INFO";
			break;
		}

		return result;
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		// TODO Auto-generated method stub
		try {
			super.show(manager, tag);
		} catch (IllegalStateException e) {
			// TODO: handle exception
			// ignore
			Log.e(TAG, "Error in dialog fragment:" + e.toString());
		}
	}

	@Override
	public int show(FragmentTransaction transaction, String tag) {
		// TODO Auto-generated method stub
		try {
			return super.show(transaction, tag);
		} catch (IllegalStateException e) {
			// TODO: handle exception
			// ignore
		}
		return -1;
	}
}
