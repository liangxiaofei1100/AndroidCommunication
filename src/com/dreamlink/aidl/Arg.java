package com.dreamlink.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class Arg implements Parcelable {
	public static Creator<Arg> CREATOR = new Parcelable.Creator<Arg>() {

		@Override
		public Arg createFromParcel(Parcel source) {
			// TODO Auto-generated method stub
			return new Arg(source);
		}

		@Override
		public Arg[] newArray(int size) {
			// TODO Auto-generated method stub
			return new Arg[size];
		}
	};
	public int userID;
	public String userName;
	public String userIP;

	public Arg() {
		// TODO Auto-generated constructor stub
	}

	public Arg(Parcel source) {
		// TODO Auto-generated constructor stub
		readFromParcel(source);
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeInt(userID);
		dest.writeString(userName);
		dest.writeString(userIP);
	}

	private void readFromParcel(Parcel in) {
		userID = in.readInt();
		userName = in.readString();
		userIP = in.readString();
	}
}
