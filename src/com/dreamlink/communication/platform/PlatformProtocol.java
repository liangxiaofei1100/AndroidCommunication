package com.dreamlink.communication.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.aidl.HostInfo;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.lib.util.ArrayUtil;

public class PlatformProtocol {

	private PlatformManager mPlatformManagerService;
	/**
	 * 下面这些CMD不一定全部都会用
	 * 
	 * 一下CMD代码全部在网络通信中使用
	 * */
	public final String CMD_HEAD = "PlatformManager@";
	public final int GROUP_INFO_CHANGE_CMD_CODE = 0;
	public final int CREATE_HOST_CMD_CODE = 1;
	public final int CREATE_HOST_ACK_CMD_CODE = -1;
	public final int JOIN_GROUP_CMD_CODE = 2;
	public final int JOIN_GROUP_ACK_CMD_CODE = -2;
	public final int REMOVE_USER_CMD_CODE = 3;
	public final int REMOVE_USER_ACK_CMD_CODE = -3;
	public final int EXIT_GROUP_CMD_CODE = 4;
	public final int EXIT_GROUP_ACK_CMD_CODE = -4;
	public final int Start_GROUP_CMD_CODE = 5;
	public final int FINISH_GROUP_BUSINESS_CMD_CODE = 6;
	public final int CANCEL_HOST_CMD_CODE = 7;
	public final int CANCEL_HOST_ACK_CMD_CODE = -7;
	public final int REGISTER_ACK_CMD_CODE = -8;
	public final int REGISTER_CMD_CODE = 8;
	public final int UNREGISTER_ACK_CMD_CODE = -9;
	public final int UNREGISTER_CMD_CODE = 9;
	public final int GET_ALL_HOST_INFO_CMD_CODE = 10;
	public final int GROUP_MEMBER_UPDATE_CMD_CODE = 11;
	public final int GET_ALL_GROUP_MEMBER_CMD_CODE = 12;
	public final int MESSAGE_CMD_CODE = 13;
	public final int RECEIVER_MESSAGE_CMD_CODE = -13;

	public PlatformProtocol(PlatformManager platformManagerService) {
		mPlatformManagerService = platformManagerService;
	}

	public boolean decodePlatformProtocol(byte[] sourceData, User user) {
		int leng = CMD_HEAD.getBytes().length;
		String tem_head = new String(Arrays.copyOfRange(sourceData, 0, leng));
		if (!CMD_HEAD.endsWith(tem_head)) {
			return false;
		}

		byte[] cmdByte = Arrays.copyOfRange(sourceData, leng, leng + 4);
		leng += 4;
		int cmdCode = ArrayUtil.byteArray2Int(cmdByte);
		Log.e("ArbiterLiu", "***********************************" + cmdCode);
		byte[] data = Arrays.copyOfRange(sourceData, leng, sourceData.length);
		switch (cmdCode) {
		case GROUP_INFO_CHANGE_CMD_CODE:
			if (UserManager.getInstance().getLocalUser().getUserID() == -1) {
				/** group owner send group change to host manager */
				hostChangeForManager(data);
			} else {
				/** host manager send to all user the host info change */
				hostChangeForUser(data);
			}
			break;
		case CREATE_HOST_CMD_CODE:
			createHost(data);
			break;
		case CREATE_HOST_ACK_CMD_CODE:
			createHostAck(data);
			break;
		case CANCEL_HOST_CMD_CODE:
			cancelHost(data, user);
			break;
		case CANCEL_HOST_ACK_CMD_CODE:
			break;
		case JOIN_GROUP_CMD_CODE:
			joinGroup(data, user);
			break;
		case JOIN_GROUP_ACK_CMD_CODE:
			recevierJoinAck(data);
			break;
		case REMOVE_USER_CMD_CODE:
			removeUser(data);
			break;
		case REMOVE_USER_ACK_CMD_CODE:
			break;
		case EXIT_GROUP_CMD_CODE:
			exitGroup(data, user);
			break;
		case EXIT_GROUP_ACK_CMD_CODE:
			break;
		case Start_GROUP_CMD_CODE:
			break;
		case REGISTER_ACK_CMD_CODE:
			break;
		case REGISTER_CMD_CODE:
			break;
		case GET_ALL_HOST_INFO_CMD_CODE:
			getAllHostInfo(data, user);
			break;
		case GROUP_MEMBER_UPDATE_CMD_CODE:
			groupMemberUpdate(data);
			break;
		case GET_ALL_GROUP_MEMBER_CMD_CODE:
			getGroupMember(data, user);
			break;
		case FINISH_GROUP_BUSINESS_CMD_CODE:
			break;
		case MESSAGE_CMD_CODE:
			groupMessagePass(data, user);
			break;
		case RECEIVER_MESSAGE_CMD_CODE:
			break;
		default:
			break;
		}
		return true;
	}

	private void joinGroup(byte[] data, User user) {
		int hostId = ArrayUtil.byteArray2Int(data);
		mPlatformManagerService.requestJoinGroup(hostId, user);
	}

	/**
	 * 信息格式：cmdCode+info.信息长度在传送的时候会编码，这里避免这些数据重复
	 * */
	public byte[] encodePlatformProtocol(int cmdCode, byte[] sourceData) {
		byte[] target = ArrayUtil.int2ByteArray(cmdCode);
		if (sourceData != null)
			target = ArrayUtil.join(target, sourceData);
		target = ArrayUtil.join(CMD_HEAD.getBytes(), target);
		return target;
	}

	private void createHost(byte[] data) {
		try {
			HostInfo hostInfo = (HostInfo) ArrayUtil.byteArrayToObject(data);
			if (hostInfo != null) {
				mPlatformManagerService.requestCreateHost(hostInfo);
			} else {
				Log.e("ArbiterLiu", "createHost receiver data is exception");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void createHostAck(byte[] data) {
		HostInfo hostInfo = (HostInfo) ArrayUtil.byteArrayToObject(data);
		if (hostInfo != null) {
			mPlatformManagerService.createHostAck(hostInfo);
		} else {
			Log.e("ArbiterLiu", "createHostAck receiver data is exception");
		}
	}

	private void hostChangeForManager(byte[] data) {
		try {
			HostInfo hostInfo = (HostInfo) ArrayUtil.byteArrayToObject(data);
			if (hostInfo != null) {
				mPlatformManagerService.updateHostInfo(hostInfo);
			} else {
				Log.e("ArbiterLiu",
						"hostChangeForManager receiver data is exception");
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void hostChangeForUser(byte[] data) {
		@SuppressWarnings("unchecked")
		ConcurrentHashMap<Integer, HostInfo> allHostInfo = (ConcurrentHashMap<Integer, HostInfo>) ArrayUtil
				.byteArrayToObject(data);
		if (allHostInfo != null) {
			mPlatformManagerService.receiverAllHostInfo(allHostInfo);
		} else {
			Log.e("ArbiterLiu", "hostChangeForUser receiver data is exception");
		}
	}

	private void getAllHostInfo(byte[] data, User user) {
		int appId = ArrayUtil.byteArray2Int(data);
		mPlatformManagerService.requestAllHost(appId, user);
	}

	private void recevierJoinAck(byte[] data) {
		byte flag = data[0];
		HostInfo hostInfo = (HostInfo) ArrayUtil.byteArrayToObject(Arrays
				.copyOfRange(data, 1, data.length));
		if (flag == 1) {
			mPlatformManagerService.receiverJoinAck(true, hostInfo);
		} else {
			mPlatformManagerService.receiverJoinAck(false, hostInfo);
		}
	}

	private void groupMemberUpdate(byte[] data) {
		int hostId = ArrayUtil.byteArray2Int(Arrays.copyOfRange(data, 0, 4));
		@SuppressWarnings("unchecked")
		ArrayList<Integer> userList = (ArrayList<Integer>) ArrayUtil
				.byteArrayToObject(Arrays.copyOfRange(data, 4, data.length));
		if (userList != null) {
			mPlatformManagerService.receiverGroupMemberUpdat(hostId, userList);
		} else {
			Log.e("ArbiterLiu", "groupMemberUpdate receiver data is exception");
		}

	}

	private void cancelHost(byte[] data, User user) {
		try {
			HostInfo hostInfo = (HostInfo) ArrayUtil.byteArrayToObject(data);
			mPlatformManagerService.receiverCancelHost(hostInfo, user);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void removeUser(byte[] data) {
		try {
			HostInfo hostInfo = (HostInfo) ArrayUtil.byteArrayToObject(data);
			mPlatformManagerService.receiverRemoveUser(hostInfo);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void exitGroup(byte[] data, User user) {
		try {
			int hostId = ArrayUtil.byteArray2Int(data);
			mPlatformManagerService.requestExitGroup(hostId, user);
		} catch (Exception e) {
			// TODO: handle exception
			Log.e("ArbiterLiu", "" + e.toString());
		}
	}

	private void getGroupMember(byte[] data, User user) {
		try {
			int hostId = ArrayUtil.byteArray2Int(data);
			mPlatformManagerService.requetsGetGroupMember(hostId, user);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void groupMessagePass(byte[] data, User user) {
		byte flag = data[0];
		int hostId = ArrayUtil.byteArray2Int(Arrays.copyOfRange(data, 1, 5));
		byte[] targetData = Arrays.copyOfRange(data, 5, data.length);
		if (flag == 1) {
			mPlatformManagerService
					.receiverData(targetData, user, true, hostId);
		} else {
			mPlatformManagerService.receiverData(targetData, user, false,
					hostId);
		}
	}
}
