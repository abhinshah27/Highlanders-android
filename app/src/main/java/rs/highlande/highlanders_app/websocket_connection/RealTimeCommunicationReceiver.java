/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.websocket_connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

import androidx.fragment.app.Fragment;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.Utils;

/**
 * @author mbaldrighi on 10/29/2017.
 */
public class RealTimeCommunicationReceiver extends BroadcastReceiver {

	private OnServerMessageReceivedListener mListener;

	private Set<String> operationIds = new HashSet<>();

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean condition = true;
		if (mListener instanceof Context)
			condition = Utils.isContextValid(((Context) mListener));
		else if (mListener instanceof Fragment)
			condition = ((Fragment) mListener).isVisible() && Utils.isContextValid(((Fragment) mListener).getContext());

		if (Utils.isContextValid(context) && condition) {
			if (intent != null && mListener != null) {
				int action = -1;
				String operationId = null;
				if (intent.hasExtra(Constants.EXTRA_PARAM_1))
					action = intent.getIntExtra(Constants.EXTRA_PARAM_1, -1);
				if (intent.hasExtra(Constants.EXTRA_PARAM_5))
					operationId = intent.getStringExtra(Constants.EXTRA_PARAM_5);

				if (checkOperationId(operationId)) {
					if (isError(intent)) {
						int statusCode = intent.getIntExtra(Constants.EXTRA_PARAM_3, -1);
						mListener.handleErrorResponse(action, statusCode);
					} else {
						JSONArray objects = null;
						if (intent.hasExtra(Constants.EXTRA_PARAM_2)) {
							String s = intent.getStringExtra(Constants.EXTRA_PARAM_2);
							if (Utils.isStringValid(s)) {
								try {
									objects = new JSONArray(s);
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							mListener.handleSuccessResponse(action, objects);
						}
					}
				}
			}
		}
	}


	private boolean isError(Intent intent) {
		return intent.hasExtra(Constants.EXTRA_PARAM_3);
	}

	private boolean checkOperationId(String operation) {
		if (!Utils.isStringValid(operation)) return false;

		if (operationIds == null)
			operationIds = new HashSet<>();

		if (!operationIds.contains(operation)) {
			operationIds.add(operation);
			return true;
		}

		return false;
	}


	public void setListener(OnServerMessageReceivedListener listener) {
		this.mListener = listener;
	}

}
