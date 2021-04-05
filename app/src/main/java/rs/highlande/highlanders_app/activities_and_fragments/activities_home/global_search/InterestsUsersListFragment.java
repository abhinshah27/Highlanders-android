/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.global_search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.realm.Realm;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.HomeActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile.ProfileActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile.ProfileHelper;
import rs.highlande.highlanders_app.adapters.SearchAdapter;
import rs.highlande.highlanders_app.base.BasicInteractionListener;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.base.HLFragment;
import rs.highlande.highlanders_app.base.OnBackPressedListener;
import rs.highlande.highlanders_app.models.HLUserGeneric;
import rs.highlande.highlanders_app.models.Interest;
import rs.highlande.highlanders_app.models.enums.GlobalSearchTypeEnum;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.LoadMoreResponseHandlerTask;
import rs.highlande.highlanders_app.utility.LoadMoreScrollListener;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.websocket_connection.HLRequestTracker;
import rs.highlande.highlanders_app.websocket_connection.HLServerCalls;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public class InterestsUsersListFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
		SearchAdapter.OnItemClickListenerForGlobalSearch, LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = InterestsUsersListFragment.class.getCanonicalName();

	private String query, title;
	private GlobalSearchTypeEnum returnType;

	private TextView toolbarTitle;

	private SwipeRefreshLayout srl;

	private RecyclerView baseRecView;
	private LinearLayoutManager llm;
	private SearchAdapter baseAdapter;
	private List<Object> mItems = new ArrayList<>();
	private TextView noResult;

	private int pageIdToCall = 0;
	private boolean fromLoadMore;
	private int newItemsCount;

	private boolean serverCalled = false;


	public InterestsUsersListFragment() {}

	public static InterestsUsersListFragment newInstance(String query, GlobalSearchTypeEnum returnType,
	                                                     String title) {
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, query);
		args.putSerializable(Constants.EXTRA_PARAM_2, returnType);
		args.putString(Constants.EXTRA_PARAM_3, title);
		InterestsUsersListFragment fragment = new InterestsUsersListFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_global_search_lists, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
		baseAdapter = new SearchAdapter(mItems, this, true);
		baseAdapter.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.GLOB_VIEW_MORE);

		callGlobalActions();
		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		pageIdToCall = 0;
	}


	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.back_arrow && Utils.isContextValid(getActivity()))
			getActivity().onBackPressed();
	}

	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, boolean isInterest) {
		String id = null;
		if (object instanceof HLUserGeneric)
			id = ((HLUserGeneric) object).getId();
		if (object instanceof Interest)
			id = ((Interest) object).getId();

		if (Utils.isStringValid(id)) {
			ProfileActivity.openProfileCardFragment(
					getContext(),
					isInterest ? ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND,
					id,
					HomeActivity.PAGER_ITEM_GLOBAL_SEARCH
			);
		}
	}

	@Override
	public void onBackPressed() {}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);
		Utils.setRefreshingForSwipeLayout(srl, false);

		newItemsCount = responseObject != null ? responseObject.length() : 0;

		if (operationId == Constants.SERVER_OP_SEARCH_GLOBAL_DETAIL) {
			if (fromLoadMore) {
				new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.GLOBAL_SEARCH,
						null, null).execute(responseObject);

//				fromLoadMore = false;
			}
			else
				try {
					setData(responseObject, false);
				} catch (JSONException e) {
					e.printStackTrace();
				}
		}

		serverCalled = false;

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
		Utils.setRefreshingForSwipeLayout(srl, false);

		baseRecView.setVisibility(View.GONE);
		noResult.setVisibility(View.VISIBLE);

		serverCalled = false;
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);

		handleErrorResponse(operationId, 0);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				query = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				returnType = (GlobalSearchTypeEnum) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				title = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
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
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		}

		srl = Utils.getGenericSwipeLayout(view, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				pageIdToCall = 0;
				Utils.setRefreshingForSwipeLayout(srl, true);
				callGlobalActions();
			}
		});

		baseRecView = view.findViewById(R.id.base_list);
		baseRecView.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				fromLoadMore = true;
				callGlobalActions();
			}
		});
		noResult = view.findViewById(R.id.no_result);
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(title);

		baseRecView.setLayoutManager(llm);
		baseRecView.setAdapter(baseAdapter);

		noResult.setText(R.string.error_generic_list);
	}

	private void callGlobalActions() {
		if (!serverCalled) {
			Object[] result = null;

			try {
				result = HLServerCalls.actionsOnGlobalSearch(mUser.getId(), query,
						HLServerCalls.GlobalSearchAction.GET_DETAIL, returnType.toString(), ++pageIdToCall);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if (getActivity() instanceof HLActivity)
				HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
						.handleCallResult(this, ((HLActivity) getActivity()), result);
		}
	}


	private void setData(JSONArray response, boolean background) throws JSONException {
		if (pageIdToCall == 1 && (response == null || response.length() == 0)) {
			baseRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		baseRecView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		if (pageIdToCall == 1) {
			if (mItems == null)
				mItems = new ArrayList<>();
			else
				mItems.clear();
		}

		for (int i = 0; i < response.length(); i++) {
			JSONObject jObj = response.getJSONObject(i);
			Object obj;
			if (returnType == GlobalSearchTypeEnum.INTERESTS)
				obj = new Interest().deserializeToClass(jObj);
			else
				obj = new HLUserGeneric().deserializeToClass(jObj);

			mItems.add(obj);
		}

		if (!background)
			baseAdapter.notifyDataSetChanged();
	}


	//region == LOAD MORE INTERFACE METHODS ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return baseAdapter;
	}

	@Override
	public void setData(Realm realm) {}

	@Override
	public void setData(JSONArray array) {
		try {
			setData(array, true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isFromLoadMore() {
		return fromLoadMore;
	}

	@Override
	public void resetFromLoadMore() {
		fromLoadMore = false;
	}

	@Override
	public int getLastPageId() {
		return pageIdToCall - 1;
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}

	//endregion

}
