/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.realm.Realm;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.LogUtils;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.realm.RealmUtils;
import rs.highlande.highlanders_app.websocket_connection.HLServerCalls;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;

/**{@link Service} subclass whose duty is to send the client's FCM token to the backend.
 * @author mbaldrighi on 11/28/2017.
 */
public class SendFCMTokenService extends Service implements OnServerMessageReceivedListener {

	public static final String LOG_TAG = SendFCMTokenService.class.getCanonicalName();

	private ServerMessageReceiver receiver;

	private String newToken;

	public static void startService(Context context) {
		startService(context, null);
	}

	public static void startService(Context context, String newToken) {
		try {
			Intent intent = new Intent(context, SendFCMTokenService.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, newToken);
			context.startService(intent);
		} catch (IllegalStateException e) {
			LogUtils.e(LOG_TAG, "Cannot start background service: " + e.getMessage(), e);
		}
	}


	@Override
	public void onCreate() {
		super.onCreate();
		if (receiver == null)
			receiver = new ServerMessageReceiver();
		receiver.setListener(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.BROADCAST_FCM_TOKEN);
		filter.addAction(Constants.BROADCAST_SERVER_RESPONSE);
//		registerReceiver(receiver, filter);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (receiver == null)
			receiver = new ServerMessageReceiver();
		receiver.setListener(this);
//		registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_FCM_TOKEN));
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BROADCAST_FCM_TOKEN));

		if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_1))
			newToken = intent.getStringExtra(Constants.EXTRA_PARAM_1);

		if (Utils.isStringValid(newToken))
			sendToken(newToken);
		else
			Utils.getFcmToken(this, this);

		return Service.START_STICKY;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		try {
//			unregisterReceiver(receiver);
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
		} catch (IllegalArgumentException e) {
			LogUtils.d(LOG_TAG, e.getMessage());
		}

		super.onDestroy();
	}


	public void sendToken(String newToken) {
		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			HLUser user = new HLUser().readUser(realm);
			String id = user != null ? user.getUserId() : "";
			if (Utils.areStringsValid(id, newToken)) {
				callSendToken(id, newToken);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			RealmUtils.closeRealm(realm);
		}
	}

	private void callSendToken(@NonNull String userId, @NonNull String token) {
		Object[] results = null;
		try {
			results = HLServerCalls.sendNotificationToken(userId, token);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (FCMNotNeededException f) {
			f.printStackTrace();
			stopSelf();
			return;
		}

		if (results == null || !((boolean) results[0])) {
			HLApp.fcmTokenSent = false;
			try {
//				unregisterReceiver(receiver);
				LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			} catch (IllegalArgumentException e) {
				LogUtils.d(LOG_TAG, e.getMessage());
			}
		}
	}


	//region == Receiver Callback ==

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {

		switch (operationId) {
			case Constants.SERVER_OP_FCM_TOKEN:
				try {
//					unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}

				HLApp.fcmTokenSent = true;

				LogUtils.d(LOG_TAG, "SEND FCM TOKEN SUCCESS");
				stopSelf();
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {

		HLApp.fcmTokenSent = false;

		switch (operationId) {
			case Constants.SERVER_OP_FCM_TOKEN:
				try {
//					unregisterReceiver(receiver);
					LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
					LogUtils.d(LOG_TAG, e.getMessage());
				}

				LogUtils.e(LOG_TAG, "SEND FCM TOKEN FAILED");
				stopSelf();
				break;
		}
	}

	//endregion

}
