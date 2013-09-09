package com.dreamlink.communication.util;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.util.UserTree.UserNode;

public class RoutePolicy {
	private UserManager userManager;
	private List<UserNode> list;

	public RoutePolicy() {

	}

	private UserTree userTree = UserTree.getInstance();

	public User connectPolicy(User user) {
		if (userManager == null) {
			userManager = UserManager.getInstance();
		}
		if (userManager.getLocalUser().getUserID() != -1) {
			/** if it is not main server */
			return null;
		} else if (userTree.headNode.child.size() < 1) {
			/** connect number is little */
			return null;
		}
		if (!user.getSystemInfo().mIsWiFiDirectSupported) {
			/** not wifi-direct device */
			return null;
		}
		return queryBestDirectUser();
	}

	private User queryBestDirectUser() {
		for (UserNode node : userTree.headNode.child) {
			if (node.user.getSystemInfo().mIsWiFiDirectSupported) {
				if (list == null) {
					list = new ArrayList<UserTree.UserNode>();
				}
				list.add(node);
			}
		}
		if (list != null && list.size() != 0) {
			UserNode tem = null;
			for (UserNode n : list) {
				if (tem != null) {
					if (tem.child.size() > n.child.size()) {
						tem = n;
					}
				} else {
					tem = n;
				}
			}
			return tem.user;
		}
		return null;
	}
}
