package com.dreamlink.communication.ui.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.R.integer;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AppCursorAdapter extends CursorAdapter {
	private static final String TAG = "HistoryMsgAdapter";
	private LayoutInflater inflater = null;
	private int status = -1;
	private Notice mNotice = null;
	private Context mContext;
	private AsyncImageLoader bitmapLoader = null;
	private boolean scrollFlag = true;
	private PackageManager pm = null;
	
	public AppCursorAdapter(Context context){
		super(context, null, true);
		
		this.mContext = context;
		mNotice  = new Notice(context);
		inflater = LayoutInflater.from(context);
		
		bitmapLoader = new AsyncImageLoader(context);
		pm = context.getPackageManager();
	}
	
	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return super.getItem(position);
	}
	
	public void setFlag(boolean flag){
		this.scrollFlag = flag;
	}
	
	private Cursor mCursor = null;
	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		
		final String packagename = cursor.getString(cursor.getColumnIndex(AppData.App.PKG_NAME));
		ApplicationInfo applicationInfo = null;
		try {
			applicationInfo = pm.getApplicationInfo(packagename, 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
		
		holder.iconView.setImageDrawable(applicationInfo.loadIcon(pm));
		holder.nameView.setText(applicationInfo.loadLabel(pm));
		long size = new File(applicationInfo.sourceDir).length();
		holder.sizeView.setText(DreamUtil.getFormatSize(size));
	}
	
	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		View view  = inflater.inflate(R.layout.ui_app_normal_item, null);
		ViewHolder holder = new ViewHolder();
		
		holder.iconView = (ImageView) view.findViewById(R.id.app_icon_text_view);
		holder.nameView = (TextView) view.findViewById(R.id.app_name_textview);
		holder.sizeView = (TextView) view.findViewById(R.id.app_size_textview);
		view.setTag(holder);
		
		return view;
	}
	
	class ViewHolder{
		ImageView iconView;
		TextView nameView;
		TextView sizeView;
	}

}
