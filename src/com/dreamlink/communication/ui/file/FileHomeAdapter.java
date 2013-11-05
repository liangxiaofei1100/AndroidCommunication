package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant.Extra;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileHomeAdapter extends BaseAdapter {
	private static final String TAG = "FileHomeAdapter";
	private List<Integer> homeList = new ArrayList<Integer>();
	private LayoutInflater mInflater = null;
	
	private SharedPreferences sp = null;
	private Resources res = null;

	public FileHomeAdapter(Context context, List<Integer> homeList) {
		mInflater = LayoutInflater.from(context);
		this.homeList = homeList;
		res = context.getResources();
		sp = context.getSharedPreferences(Extra.SHARED_PERFERENCE_NAME, Context.MODE_PRIVATE);
	}

	@Override
	public int getCount() {
		return homeList.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	class ViewHolder {
		ImageView iconView;
		TextView nameView;
		TextView dateAndSizeView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;

		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_file_item, parent, false);
			holder.iconView = (ImageView) view
					.findViewById(R.id.file_icon_imageview);
			holder.nameView = (TextView) view
					.findViewById(R.id.file_name_textview);
			holder.dateAndSizeView = (TextView) view
					.findViewById(R.id.file_info_textview);
			view.setTag(holder);
		} else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}

		switch (homeList.get(position)) {
		case FileBrowserFragment.INTERNAL:
			holder.iconView.setImageResource(R.drawable.storage_internal_n);
			holder.nameView.setText(R.string.internal_sdcard);
			holder.dateAndSizeView.setVisibility(View.GONE);
			break;
		case FileBrowserFragment.SDCARD:
			holder.iconView.setImageResource(R.drawable.storage_sd_card_n);
			holder.nameView.setText(R.string.sdcard);
			holder.dateAndSizeView.setVisibility(View.GONE);
			break;
		case FileBrowserFragment.DOC:
			holder.iconView.setImageResource(R.drawable.default_doc_icon);
			int doc_num = sp.getInt(FileInfoManager.DOC_NUM, -1);
			if (-1 == doc_num) {
				holder.nameView.setText(R.string.doc_type);
			}else {
				holder.nameView.setText(res.getString(R.string.doc_type_format, doc_num));
			}
			holder.dateAndSizeView.setText(R.string.doc_type_tip);
			break;
		case FileBrowserFragment.EBOOK:
			holder.iconView.setImageResource(R.drawable.default_ebook_icon);
			int ebook_num = sp.getInt(FileInfoManager.EBOOK_NUM, -1);
			if (-1 == ebook_num) {
				holder.nameView.setText(R.string.ebook_type);
			}else {
				holder.nameView.setText(res.getString(R.string.ebook_type_format, ebook_num));
			}
			holder.dateAndSizeView.setText(R.string.ebook_type_tip);
			break;
		case FileBrowserFragment.APK:
			holder.iconView.setImageResource(R.drawable.deafult_apk_icon);
			int apk_num = sp.getInt(FileInfoManager.APK_NUM, -1);
			if (-1 == apk_num) {
				holder.nameView.setText(R.string.archive_type);
			}else {
				holder.nameView.setText(res.getString(R.string.apk_type_format, apk_num));
			}
			holder.dateAndSizeView.setText(R.string.apk_type_tip);
			break;
		case FileBrowserFragment.ARCHIVE:
			holder.iconView.setImageResource(R.drawable.default_archive_icon);
			int archive_num = sp.getInt(FileInfoManager.ARCHIVE_NUM, -1);
			if (-1 == archive_num) {
				holder.nameView.setText(R.string.archive_type);
			}else {
				holder.nameView.setText(res.getString(R.string.archive_type_format, archive_num));
			}
			holder.dateAndSizeView.setText(R.string.archive_type_tip);
			break;
		}

		return view;
	}

}
