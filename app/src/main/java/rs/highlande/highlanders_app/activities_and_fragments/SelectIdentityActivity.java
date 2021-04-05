/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.realm.Realm;
import io.realm.RealmList;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.adapters.BasicAdapterInteractionsListener;
import rs.highlande.highlanders_app.adapters.IdentitiesAdapter;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.models.HLIdentity;
import rs.highlande.highlanders_app.models.HLNotifications;
import rs.highlande.highlanders_app.models.HLPosts;
import rs.highlande.highlanders_app.services.GetConfigurationDataService;
import rs.highlande.highlanders_app.services.HandleChatsUpdateService;
import rs.highlande.highlanders_app.services.SubscribeToSocketService;
import rs.highlande.highlanders_app.services.SubscribeToSocketServiceChat;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;
import rs.highlande.highlanders_app.websocket_connection.HLRequestTracker;
import rs.highlande.highlanders_app.websocket_connection.HLServerCalls;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;

/**
 * Identity selection activity.
 */
public class SelectIdentityActivity extends HLActivity implements View.OnClickListener,
		BasicAdapterInteractionsListener, OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = SelectIdentityActivity.class.getCanonicalName();

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private RecyclerView mRecView;
	private List<HLIdentity> mList = new RealmList<>();
	private LinearLayoutManager llm;
	private IdentitiesAdapter mAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_identity);
		setRootContent(R.id.root_content);

		llm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
		mAdapter = new IdentitiesAdapter(mList, this);
		mAdapter.setSelectedIdentityID(mUser.getId());

		mRecView = findViewById(R.id.identities_rv);

		configureResponseReceiver();

		configureToolbar(null, null, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.ME_IDENTITIES_SELECTION);

		toolbarTitle.setText(R.string.title_activity_select_identity);
		MediaHelper.loadProfilePictureWithPlaceholder(this, mUser.getAvatarURL(), profilePicture);

		callIdentities();

		mRecView.setLayoutManager(llm);
		mRecView.setAdapter(mAdapter);

		setData();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.back_arrow) {
			setResult(RESULT_CANCELED);
			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
		}
	}

	@Override
	public void onItemClick(final Object object) {
		if (!(object instanceof HLIdentity)) return;

		MediaHelper.loadProfilePictureWithPlaceholder(this, ((HLIdentity) object).getAvatarURL(), profilePicture);
		mAdapter.setSelectedIdentityID(((HLIdentity) object).getId());
		mAdapter.notifyDataSetChanged();

		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(@NonNull Realm realm) {
				mUser.setSelectedIdentityId(mUser.getUserId().equals(((HLIdentity) object).getId()) ? null : ((HLIdentity) object).getId());
				mUser.setSelectedIdentity(mUser.getUserId().equals(((HLIdentity) object).getId()) ? null : (HLIdentity) object);

				if (mUser.getSelectedFeedFilters() == null)
					mUser.setSelectedFeedFilters(new RealmList<String>());
				else
					mUser.getSelectedFeedFilters().clear();
			}
		});

		SubscribeToSocketService.startService(this);
		GetConfigurationDataService.startService(this);
		if (!mUser.isActingAsInterest()) {
			SubscribeToSocketServiceChat.startService(this);
			HandleChatsUpdateService.startService(this);
		}

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				HLApp.resetPaginationIds();
				HLPosts.getInstance().resetCollectionsForSwitch(realm);
				HLNotifications.getInstance().resetCollectionsForSwitch();
				HLApp.identityChanged = true;

				setResult(RESULT_OK);
				finish();
				overridePendingTransition(R.anim.no_animation, R.anim.slide_out_top);
			}
		}, 300);
	}

	@Override
	public void onItemClick(Object object, View view) {}


	/*
	 * NO NEED TO OVERRIDE THIS
	 */
	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_GET_IDENTITIES_V2:
				realm.executeTransaction(new Realm.Transaction() {
					@Override
					public void execute(@NonNull Realm realm) {
						mUser.setIdentities(responseObject);

						setData();
					}
				});
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}

	@Override
	protected void manageIntent() {}


	//region == Class custom methods ==


	@Override
	protected void configureToolbar(Toolbar toolbar, String title, boolean showBack) {
		toolbar = findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);

		View back = toolbar.findViewById(R.id.back_arrow);
		back.setOnClickListener(this);
		back.setRotation(-90f);
	}

	private void callIdentities() {
		Object[] result = null;

		try {
			result = HLServerCalls.getIdentities(mUser.getUserId());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((HLApp) getApplication())).handleCallResult(this, this, result);
	}

	private void setData() {
		if (mList == null)
			mList = new RealmList<>();
		else
			mList.clear();

		mList.addAll(mUser.getIdentities());

		mAdapter.notifyDataSetChanged();
	}

	//endregion

}

