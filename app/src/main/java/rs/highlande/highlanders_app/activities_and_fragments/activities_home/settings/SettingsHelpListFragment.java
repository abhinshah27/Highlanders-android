/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.adapters.SettingsHelpListViewAdapter;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.base.HLFragment;
import rs.highlande.highlanders_app.models.SettingsHelpElement;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.websocket_connection.HLRequestTracker;
import rs.highlande.highlanders_app.websocket_connection.HLServerCalls;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;


public class SettingsHelpListFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener,
		ListView.OnItemClickListener, OnMissingConnectionListener {

	public static final String LOG_TAG = SettingsHelpListFragment.class.getCanonicalName();

	private SettingsHelpElement currentElement;

	private String title;

	private TextView titleView;

	private SwipeRefreshLayout srl;
	private ListView baseList;
	private ArrayAdapter<SettingsHelpElement> baseAdapter;
	private TextView noResult;

	private List<SettingsHelpElement> elements = new ArrayList<>();


	public SettingsHelpListFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment ProfileFragment.
	 */
	public static SettingsHelpListFragment newInstance(SettingsHelpElement element) {
		SettingsHelpListFragment fragment = new SettingsHelpListFragment();
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, element);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_settings_help_list, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (Utils.isContextValid(getActivity()))
			baseAdapter = new SettingsHelpListViewAdapter(getActivity(), R.layout.item_settings_entry, elements);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.SETTINGS_HELP_LIST);

		callForElements();
		setLayout();
	}

	private void callForElements() {
		if (currentElement != null) {
			Object[] result = null;

			try {
				result = HLServerCalls.getSettingsHelpElements(currentElement);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (getActivity() instanceof HLActivity)
				HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
						.handleCallResult(this, (HLActivity) getActivity(), result);

			return;
		}

		activityListener.showGenericError();
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

	}

	@Override
	public void onDetach() {
		super.onDetach();
	}


	@Override
	public void onClick(View v) {

	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (elements != null && !elements.isEmpty()) {
			SettingsHelpElement element = elements.get(position);

			if (element != null && element.hasNextNavigationID()) {
				switch (element.getNextNavigationID()) {
					case Constants.KEY_NAV_ID_BASE_LIST_SETTINGS:
						settingsActivityListener.showSettingsHelpListFragment(element);
						break;

					case Constants.KEY_NAV_ID_HELP_YES_NO:
						settingsActivityListener.showSettingsYesNoUIFragment(element);
						break;

					case Constants.KEY_NAV_ID_HELP_USER_GUIDE:
						settingsActivityListener.goToUserGuide();
						break;

					case Constants.KEY_NAV_ID_HELP_CONTACT:
						settingsActivityListener.showSettingsHelpContactFragment();
						break;
				}
			}
		}
	}



	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_HELP_GET_ELEMENTS:
				JSONObject obj = responseObject.optJSONObject(0);
				if (obj != null && obj.length() > 0) {
					title = obj.optString("title");
					setData(obj.optJSONArray("items"));

					setLayout();
				}
				else {
					baseList.setVisibility(View.GONE);
					noResult.setVisibility(View.VISIBLE);
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		activityListener.showGenericError();
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				currentElement = (SettingsHelpElement) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);

			if (currentElement == null)
				currentElement = new SettingsHelpElement();
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		baseList = view.findViewById(R.id.base_list);
		baseList.setOnItemClickListener(this);

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);
				callForElements();
			}
		});

		noResult = view.findViewById(R.id.no_result);

		titleView = view.findViewById(R.id.title);
	}

	@Override
	protected void setLayout() {
		settingsActivityListener.setToolbarTitle(R.string.settings_main_help);

		baseList.setAdapter(baseAdapter);

		srl.setEnabled(!currentElement.isRoot());

		titleView.setText(title);
		titleView.setVisibility(currentElement.isRoot() ? View.GONE : View.VISIBLE);
	}


	private void setData(JSONArray response) {
		if (response != null && response.length() > 0) {

			baseList.setVisibility(View.VISIBLE);
			noResult.setVisibility(View.GONE);

			if (elements == null)
				elements = new ArrayList<>();
			else
				elements.clear();

			for (int i = 0; i < response.length(); i++) {
				SettingsHelpElement element = new SettingsHelpElement().deserializeToClass(response.optJSONObject(i));
				if (element != null)
					elements.add(element);
			}

			baseAdapter.notifyDataSetChanged();
		}
	}

}
