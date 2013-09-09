package com.dreamlink.communication.ui;

import java.util.LinkedList;
import java.util.List;

import com.dreamlink.communication.aidl.User;

public class UserInfoManager {
	private List<UserRecord> userList = new LinkedList<UserInfoManager.UserRecord>();
	
	public void addUserList(UserRecord userRecord){
		userList.add(userRecord);
	}
	
	public static class UserRecord{
		private int top;
		private User user;
		
		public int getTop() {
			return top;
		}
		public void setTop(int top) {
			this.top = top;
		}
		public User getUser() {
			return user;
		}
		public void setUser(User user) {
			this.user = user;
		}
		
	}
}
