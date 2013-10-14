package com.dreamlink.communication.ui.image;

import java.util.List;

import com.dreamlink.communication.R;

import android.R.integer;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PictureAdapter extends BaseAdapter {
	private static final String TAG = "PictureAdapter";
	private LayoutInflater inflater = null;
	private List<PictureFolderInfo> dataList;
	private ContentResolver contentResolver;

	public PictureAdapter(Context context, List<PictureFolderInfo> folderList){
		inflater = LayoutInflater.from(context);
		dataList = folderList;
		contentResolver = context.getContentResolver();
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return dataList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View view = null;
		ViewHolder holder = null;
		if (null == convertView) {
			view = inflater.inflate(R.layout.ui_picture_folder_item, null);
			holder = new ViewHolder();
			
			holder.imageView = (ImageView) view.findViewById(R.id.iv_picture_folder_icon);
			holder.nameView = (TextView) view.findViewById(R.id.tv_picture_folder_name);
			holder.sizeView = (TextView) view.findViewById(R.id.tv_picture_num);
			
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) convertView.getTag();
		}
		
		long id = dataList.get(position).getIdList().get(0);
		String name = dataList.get(position).getBucketDisplayName();
		int size = dataList.get(position).getIdList().size();
		Bitmap bitmap = getBitmap(id);
		holder.imageView.setImageBitmap(bitmap);
		holder.nameView.setText(name);
		holder.sizeView.setText(size + "");
		
		return view;
	}
	
	private class ViewHolder{
		ImageView imageView;//folder icon
		TextView nameView;//folder name
		TextView sizeView;//picture num
	}
	
	public Bitmap getBitmap(long id){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inDither = false;//采用默认值
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;//采用默认值
		// get images thumbail
		Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, Images.Thumbnails.MICRO_KIND, options);
		return bitmap;
	}

}
