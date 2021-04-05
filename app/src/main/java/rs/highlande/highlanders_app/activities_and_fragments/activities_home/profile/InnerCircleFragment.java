/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.HomeActivity;
import rs.highlande.highlanders_app.adapters.CirclesAdapter;
import rs.highlande.highlanders_app.adapters.ContactsAdapter;
import rs.highlande.highlanders_app.base.BasicInteractionListener;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.base.HLFragment;
import rs.highlande.highlanders_app.models.HLCircle;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.models.ProfileContactToSend;
import rs.highlande.highlanders_app.models.enums.SearchTypeEnum;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.LoadMoreResponseHandlerTask;
import rs.highlande.highlanders_app.utility.LoadMoreScrollListener;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;
import rs.highlande.highlanders_app.utility.helpers.SearchHelper;
import rs.highlande.highlanders_app.websocket_connection.HLRequestTracker;
import rs.highlande.highlanders_app.websocket_connection.HLServerCalls;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InnerCircleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InnerCircleFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener,
		SearchHelper.OnQuerySubmitted, InviteHelper.OnInviteActionListener,
		CirclesAdapter.OnInnerCircleActionListener, LoadMoreResponseHandlerTask.OnDataLoadedListener {

	public static final String LOG_TAG = InnerCircleFragment.class.getCanonicalName();

	private boolean showContacts = false;

	private String userId, userName, userAvatar;
	private String query;

	private TextView toolbarTitle;
	private ImageView profilePicture;

	private View progress;
	private TextView progrMessage;

	private RecyclerView circlesRv;
	private TextView noResult;
	private RecyclerView contactsRv;
	private View circlesTab, contactsTab, contactsLayout;

	private EditText searchBox;

	private boolean isUser;

	// CIRCLES
	private List<HLCircle> circlesListToShow = new ArrayList<>();
	private Map<String, HLCircle> circlesListLoaded = new ConcurrentHashMap<>();
	private CirclesAdapter circlesAdapter;
	private LinearLayoutManager circlesLlm;

	private SwipeRefreshLayout srl, srlContacts;

	private boolean fromLoadMore;
	private int newItemsCount;


	// CONTACTS
	private List<ProfileContactToSend> contactsToShow = new ArrayList<>();
	private List<ProfileContactToSend> contactsRetrieved = new ArrayList<>();
	private LongSparseArray<ProfileContactToSend> contactsTask = new LongSparseArray<>();
	private ContactsAdapter contactsAdapter;
	private LinearLayoutManager contactsLlm;
	private boolean contactsLoaded;
	private boolean contactsInterrupted;

	private Integer scrollPositionContacts, scrollPositionCircles;

	private InviteHelper inviteHelper;

	private AsyncTask<JSONArray, Void, List<ProfileContactToSend>> handleContactsTask;
	private AsyncTask<Void, Void, Void> getContactTask;
	private AsyncTask<List<ProfileContactToSend>, Void, Void> getPicturesTask;

	private SearchHelper mSearchHelper;


	public InnerCircleFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InnerCircleFragment.
	 */
	public static InnerCircleFragment newInstance(String userId, String userName, String userAvatar,
	                                              boolean switchToContacts) {
		InnerCircleFragment fragment = new InnerCircleFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_PARAM_1, userId);
		args.putString(Constants.EXTRA_PARAM_2, userName);
		args.putString(Constants.EXTRA_PARAM_3, userAvatar);
		args.putBoolean(Constants.EXTRA_PARAM_4, switchToContacts);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		mSearchHelper = new SearchHelper(this);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_profile_inner_circle, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);


		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		triggerBackgroundOperations();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_INNER_CIRCLE);

		configureResponseReceiver();
		setLayout();
	}

	@Override
	public void onPause() {

		onSaveInstanceState(new Bundle());

		scrollPositionContacts = contactsLlm.findFirstCompletelyVisibleItemPosition();
		scrollPositionCircles = circlesLlm.findFirstCompletelyVisibleItemPosition();

		if (handleContactsTask != null && !handleContactsTask.isCancelled())
			handleContactsTask.cancel(true);
		if (getContactTask != null && !getContactTask.isCancelled())
			getContactTask.cancel(true);
		if (getPicturesTask != null && !getPicturesTask.isCancelled())
			getPicturesTask.cancel(true);

		contactsInterrupted = true;
		progress.setVisibility(View.GONE);

		Utils.closeKeyboard(searchBox);

		super.onPause();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		contactsLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
		circlesLlm = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);

		if (inviteHelper == null)
			inviteHelper = new InviteHelper(getContext(), this);

		contactsAdapter = new ContactsAdapter(contactsToShow, inviteHelper);
		contactsAdapter.setHasStableIds(true);

		circlesAdapter = new CirclesAdapter(circlesListToShow, this);
		circlesAdapter.setHasStableIds(true);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.profile_tab_inner_circle:

				scrollPositionContacts = contactsLlm.findFirstCompletelyVisibleItemPosition();
				scrollPositionCircles = circlesLlm.findFirstCompletelyVisibleItemPosition();

				showContacts = false;
				setLayout();
				break;
			case R.id.profile_tab_contacts:
				showContacts = true;
				setLayout();
				break;

			case R.id.back_arrow:
				if (getActivity() != null)
					getActivity().onBackPressed();
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
//		if (context instanceof OnProfileFragmentInteractionListener) {
//			mListener = (OnProfileFragmentInteractionListener) context;
//		} else {
//			throw new RuntimeException(context.toString()
//					+ " must implement OnFragmentInteractionListener");
//		}
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
//		if (context instanceof OnProfileFragmentInteractionListener) {
//			mListener = (OnProfileFragmentInteractionListener) context;
//		} else {
//			throw new RuntimeException(context.toString()
//					+ " must implement OnFragmentInteractionListener");
//		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
//		mListener = null;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(Constants.EXTRA_PARAM_1, userId);
		outState.putString(Constants.EXTRA_PARAM_2, userName);
		outState.putString(Constants.EXTRA_PARAM_3, userAvatar);
		outState.putBoolean(Constants.EXTRA_PARAM_4, showContacts);
		outState.putString(Constants.EXTRA_PARAM_5, query);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				userId = savedInstanceState.getString(Constants.EXTRA_PARAM_1);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2))
				userName = savedInstanceState.getString(Constants.EXTRA_PARAM_2);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_3))
				userAvatar = savedInstanceState.getString(Constants.EXTRA_PARAM_3);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_4))
				showContacts = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_4, false);

			if (Utils.isStringValid(userId) && RealmObject.isValid(mUser))
				isUser = userId.equals(mUser.getId());
		}
	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0) {
			newItemsCount = 0;
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLES_PROFILE:

				newItemsCount = responseObject.length();

				if (fromLoadMore) {
					new LoadMoreResponseHandlerTask(this, LoadMoreResponseHandlerTask.Type.CIRCLES,
							null, null).execute(responseObject);
				}
				else handleCircleResponse(responseObject);

				break;

			case Constants.SERVER_OP_MATCH_USER_HL:
				handleContactsTask = new HandleContactsTask(contactsRetrieved, contactsAdapter).execute(responseObject);
				break;

			case Constants.SERVER_OP_INVITE_EMAIL:
				Toast.makeText(getContext(), R.string.invite_success_email, Toast.LENGTH_SHORT).show();
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLES_PROFILE:
				activityListener.showAlert(R.string.error_generic_update);
				break;

			case Constants.SERVER_OP_MATCH_USER_HL:
				progress.setVisibility(View.GONE);
				break;

			case Constants.SERVER_OP_INVITE_EMAIL:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);

		if (operationId == Constants.SERVER_OP_MATCH_USER_HL)
			progress.setVisibility(View.GONE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == Constants.PERMISSIONS_REQUEST_CONTACTS) {
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

				new Handler().postDelayed(() -> triggerBackgroundOperations(), 300);
			}
			else {
				callForCircles();
				progress.setVisibility(View.GONE);
			}
		}

		// not used anymore
//		else if (requestCode == Constants.PERMISSIONS_REQUEST_SMS) {
//			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//				inviteHelper.sendSMS();
//			}
//		}
	}


	//region == LOAD MORE INTERFACE METHODS ==

	@Override
	public BasicInteractionListener getActivityListener() {
		return activityListener;
	}

	@Override
	public RecyclerView.Adapter getAdapter() {
		return circlesAdapter;
	}

	@Override
	public void setData(Realm realm) {}

	@Override
	public void setData(JSONArray array) {
		setData(true);
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
		if (circlesListLoaded == null)
			circlesListLoaded = new HashMap<>();

		return (circlesListLoaded.size() / Constants.PAGINATION_AMOUNT);
	}

	@Override
	public int getNewItemsCount() {
		return newItemsCount;
	}

	//endregion



	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		configureToolbar(toolbar);

		view.findViewById(R.id.focus_catcher).requestFocus();

		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
		if (searchBox != null)
			searchBox.setHint(R.string.profile_search_box_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		configureContactsProgress(view);

		circlesRv = view.findViewById(R.id.inner_circle_page_ic);
		circlesRv.addOnScrollListener(new LoadMoreScrollListener() {
			@Override
			public void onLoadMore() {
				callForCircles();
				fromLoadMore = true;
			}
		});
		noResult = view.findViewById(R.id.no_result);
		contactsRv = view.findViewById(R.id.inner_circle_page_contacts);

		srl = Utils.getGenericSwipeLayout(view, () -> {
			scrollPositionCircles = null;
			Utils.setRefreshingForSwipeLayout(srl, true);
			callForCircles();
		});

		contactsLayout = view.findViewById(R.id.inner_circle_page_contacts_layout);
		srlContacts = view.findViewById(R.id.swipe_refresh_layout_contacts);
		srlContacts.setDistanceToTriggerSync(200);
		srlContacts.setColorSchemeResources(R.color.colorAccent);
		srlContacts.setOnRefreshListener(() -> {
			scrollPositionContacts = null;

			Utils.setRefreshingForSwipeLayout(srlContacts, true);

			// starts contacts handling
			if (contactsRetrieved == null)
				contactsRetrieved = new ArrayList<>();
			else
				contactsRetrieved.clear();
			getContactTask = new GetContactsTask().execute();
		});

		circlesTab = view.findViewById(R.id.profile_tab_inner_circle);
		circlesTab.setOnClickListener(this);

		contactsTab = view.findViewById(R.id.profile_tab_contacts);
		contactsTab.setOnClickListener(this);
		contactsTab.setVisibility(isUser ? View.VISIBLE : View.GONE);
	}

	private void configureContactsProgress(View view) {
		if (view != null) {
			progress = view.findViewById(R.id.progress_contacts);
			progrMessage = view.findViewById(R.id.progress_message);
		}
	}

	@Override
	protected void setLayout() {
		toolbarTitle.setText(R.string.inner_circle);

		MediaHelper.loadProfilePictureWithPlaceholder(getContext(), userAvatar, profilePicture);

		contactsTab.setSelected(showContacts);
		circlesTab.setSelected(!showContacts);

		// due to the hierarchy hiding the RecyclerViews isn't enough.
		srl.setVisibility(showContacts ? View.GONE : View.VISIBLE);
		contactsLayout.setVisibility(showContacts ? View.VISIBLE : View.GONE);

		progress.setVisibility(!contactsInterrupted && !contactsLoaded && showContacts ? View.VISIBLE : View.GONE);

		if (Utils.isStringValid(query))
			searchBox.setText(query);

		if (showContacts) {
			noResult.setText(R.string.no_phone_contacts);

			if (contactsLoaded && contactsRetrieved != null && !contactsRetrieved.isEmpty()) {
				noResult.setVisibility(View.GONE);
				contactsRv.setLayoutManager(contactsLlm);
				contactsRv.setAdapter(contactsAdapter);

				if (searchBox.getText().length() > 0) {
					onQueryReceived(searchBox.getText().toString());
					return;
				}

				if (contactsToShow == null)
					contactsToShow = new ArrayList<>();
				else
					contactsToShow.clear();

				contactsToShow.addAll(contactsRetrieved);
				Collections.sort(contactsToShow);
				contactsAdapter.notifyDataSetChanged();
			}
			else
				noResult.setVisibility(View.VISIBLE);
		}
		else {
			circlesRv.setLayoutManager(circlesLlm);
			circlesRv.setAdapter(circlesAdapter);
			setData(false);
		}

		if (scrollPositionContacts != null && scrollPositionContacts > -1 && contactsLlm != null)
			contactsLlm.scrollToPosition(scrollPositionContacts);
		if (scrollPositionCircles != null && scrollPositionCircles > -1 && circlesLlm != null)
			circlesLlm.scrollToPosition(scrollPositionCircles);
	}

	@Override
	public void onQueryReceived(@NonNull final String query) {
		this.query = query;

		if (!query.isEmpty()) {
			if (showContacts) {
				if (contactsRetrieved != null && !contactsRetrieved.isEmpty()) {
					contactsToShow.clear();
					Collections.sort(contactsRetrieved);
					contactsToShow.addAll(Stream.of(contactsRetrieved)
							.filter(contact -> contact.getName()
									.toLowerCase()
									.contains(query.toLowerCase()))
							.collect(Collectors.toList()));

					contactsAdapter.notifyDataSetChanged();
				}
			} else {
				profileActivityListener.showSearchFragment(query, SearchTypeEnum.INNER_CIRCLE, userId,
						userName, userAvatar);
				searchBox.setText("");
			}
		}
		else {
            if (showContacts && contactsToShow != null && contactsRetrieved != null) {
            	contactsToShow.clear();
                contactsToShow.addAll(contactsRetrieved);
                contactsAdapter.notifyDataSetChanged();
            }
        }
	}

	@Override
	public Fragment getFragment() {
		return this;
	}

	private void configureToolbar(Toolbar toolbar) {
		if (toolbar != null) {
			toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
			profilePicture = toolbar.findViewById(R.id.profile_picture);
		}
	}


	@Override
	public void sendEmail(String email) {
		Object[] result = null;

		try {
			result = HLServerCalls.inviteWithEmail(mUser.getId(), email);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
		}
	}

	@Override
	public String getUserName() {
		return mUser.getUserCompleteName();
	}

	@Override
	public void goToViewMore(@NonNull String circleName) {
		profileActivityListener.showCircleViewMoreFragment(circleName, userId, userName, userAvatar);
	}

	@Override
	public void goToProfile(@Nullable ProfileHelper.ProfileType type, @NonNull String userId) {
		if (userId.equals(mUser.getId()) && Utils.isContextValid(getActivity())) {
			Intent intent = new Intent(getContext(), HomeActivity.class);
			intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
			startActivity(intent);
			getActivity().finish();
		}
		else {
			profileActivityListener.showProfileCardFragment(type, userId);
		}
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}

	/**
	 * Triggers two different tasks:
	 * <p>
	 *      - calls server for user's circles to display in the first view.
	 *      <p>
	 *      - starts contacts handling first retrieving Cursor with contacts data and then handling
	 *      server's matching operation.
	 */
	private void triggerBackgroundOperations() {
		callForCircles();

		if (isUser && (contactsRetrieved == null || contactsRetrieved.isEmpty())) {
			if (contactsRetrieved == null)
				contactsRetrieved = new ArrayList<>();
			// starts contacts handling
			getContactTask = new GetContactsTask().execute();
		}
	}

	private void callForCircles() {
		Utils.setRefreshingForSwipeLayout(srl, true);

		Object[] result = null;
		try {
			result = HLServerCalls.getCirclesForProfile(userId, getLastPageId() + 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}


	private void handleCircleResponse(final JSONArray response) {
		realm.executeTransaction(realm -> {
			HLCircle circle;
			JSONObject circles = response.optJSONObject(0);
			if (circles != null) {
				if (mUser.getCircleObjects() == null)
					mUser.setCircleObjects(new RealmList<>());
				else
					mUser.getCircleObjects().clear();

				Iterator<String> iter = circles.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					circle = new HLCircle().deserializeToClass(circles.optJSONObject(key));
					circle.setNameToDisplay(key);

					circlesListLoaded.put(circle.getName(), circle);
					mUser.getCircleObjects().add(circle);
				}

				mUser.updateFilters();

				setLayout();
			}
		});
	}

	private void setData(boolean background) {
		if (circlesListToShow == null)
			circlesListToShow = new ArrayList<>();
		else
			circlesListToShow.clear();

		if (circlesListLoaded.isEmpty()) {
			noResult.setText(R.string.no_people_in_ic);
			noResult.setVisibility(View.VISIBLE);
			circlesRv.setVisibility(View.GONE);
		}
		else {
			noResult.setVisibility(View.GONE);
			circlesRv.setVisibility(View.VISIBLE);

			List<HLCircle> circles = new ArrayList<>(circlesListLoaded.values());
			Collections.sort(circles, HLCircle.CircleSortOrderComparator);
			circlesListToShow.addAll(circles);

			if (!background)
				circlesAdapter.notifyDataSetChanged();
		}
	}

	private void getContacts2() {
		if (!mayRequestContacts()) {
			progress.setVisibility(View.GONE);
			return;
		}
		long start = System.currentTimeMillis();

		String[] projection = {
				ContactsContract.Data.MIMETYPE,
				ContactsContract.Data.CONTACT_ID,
				ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Contactables.DATA,
				ContactsContract.CommonDataKinds.Contactables.TYPE,
		};
		String selection = ContactsContract.Data.MIMETYPE + " in (?, ?)";
		String[] selectionArgs = {
				ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
				ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
		};
		String sortOrder = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;

		Uri uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
		// we could also use Uri uri = ContactsContract.Data.CONTENT_URI;

		// ok, let's work...
		final Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);

		int counter = 0;
		if (cursor != null) {

			final int mimeTypeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
			final int idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
			final int nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			final int dataIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.DATA);
			final int typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.TYPE);

			while (cursor.moveToNext()) {
				final long id = cursor.getLong(idIdx);
				ProfileContactToSend addressBookContact = contactsTask.get(id);
				if (addressBookContact == null) {
					addressBookContact = new ProfileContactToSend(getContext(), contactsAdapter,
							id, cursor.getString(nameIdx));
					contactsTask.put(id, addressBookContact);
				}

				int type = cursor.getInt(typeIdx);
				String data = cursor.getString(dataIdx);
				String mimeType = cursor.getString(mimeTypeIdx);
				if (mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
					// mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
					addressBookContact.addEmail(data);
				} else {
					// mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
					addressBookContact.addPhone(data);
				}
			}
			long ms = System.currentTimeMillis() - start;
			cursor.close();
		}
		
		if (contactsTask != null && contactsTask.size() > 0) {
			for (int i = 0; i < contactsTask.size(); i++) {
				long id = contactsTask.keyAt(i);
				ProfileContactToSend c = contactsTask.get(id);
				if (c != null && (c.hasEmails() || c.hasPhones()))
					contactsRetrieved.add(c);
			}
		}
		else {
			progress.setVisibility(View.GONE);
			if (getContactTask != null && !getContactTask.isCancelled())
				getContactTask.cancel(true);
			contactsInterrupted = true;
			return;
		}

		// call to server
		Object[] result = null;
		try {
			result = HLServerCalls.matchContactsWithHLUsers(userId, contactsRetrieved);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}

	}


	private boolean mayRequestContacts() {
		if (!Utils.hasMarshmallow() || Utils.hasApplicationPermission(getActivity(), android.Manifest.permission.READ_CONTACTS)) {
			return true;
		}

		Utils.askRequiredPermissionForFragment(this, android.Manifest.permission.READ_CONTACTS, Constants.PERMISSIONS_REQUEST_CONTACTS);
		return false;
	}

	//endregion


	//region == CUSTOM INNER CLASSES ==

	//region ++ AsyncTasks ++

	private class GetContactsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// starts progress
			if (showContacts) {
				if (srlContacts.isRefreshing())
					srlContacts.setRefreshing(false);
				progress.setVisibility(View.VISIBLE);
			}
			progrMessage.setText(R.string.initializing_query);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			getContacts2();
			return null;
		}
	}

	private class HandleContactsTask extends AsyncTask<JSONArray, Void, List<ProfileContactToSend>> {

		private List<ProfileContactToSend> contacts;
		private ContactsAdapter adapter;

		HandleContactsTask(List<ProfileContactToSend> contacts, ContactsAdapter adapter) {
			this.contacts = contacts;
			this.adapter = adapter;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			progrMessage.setText(R.string.contacting_server);
		}

		@Override
		protected List<ProfileContactToSend> doInBackground(JSONArray... arrays) {
			try {
				if (arrays[0] != null && arrays[0].length() > 0) {
					JSONArray indexes = arrays[0].optJSONObject(0).optJSONArray("indexes");

					if (indexes != null && indexes.length() > 0 &&
							contacts != null && !contacts.isEmpty() &&
							contacts.size() >= indexes.length()) {
						for (int i = indexes.length() - 1; i >=0 ; i--) {
							contacts.remove(indexes.getInt(i));
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return contacts;
		}

		@Override
		protected void onPostExecute(List<ProfileContactToSend> contacts) {
			super.onPostExecute(contacts);

			if (contactsToShow == null)
				contactsToShow = new ArrayList<>();
			else
				contactsToShow.clear();

			Collections.sort(contacts);
			contactsToShow.addAll(contacts);

			if (Utils.isContextValid(getActivity()) && showContacts) {
				contactsRv.setLayoutManager(contactsLlm);
				contactsRv.setAdapter(adapter);
				adapter.notifyDataSetChanged();
			}

			contactsLoaded = true;
			contactsInterrupted = false;
			progress.setVisibility(View.GONE);

			getPicturesTask = new GetPictureTask(contactsAdapter, getContext()).execute(contacts);
		}
	}

	static class GetPictureTask extends AsyncTask<List<ProfileContactToSend>, Void, Void> {

		private ContactsAdapter mAdapter;
		private WeakReference<Context> context;

		GetPictureTask(ContactsAdapter mAdapter, Context context) {
			this.mAdapter = mAdapter;
			this.context = new WeakReference<>(context);
		}

		@Override
		protected Void doInBackground(List<ProfileContactToSend>[] lists) {
			List<ProfileContactToSend> list = lists[0];
			if (list != null && !list.isEmpty()) {
				Iterator<ProfileContactToSend> iter = list.iterator();
				// solving ConcurrentException
				while (iter.hasNext()) {
					ProfileContactToSend c = iter.next();
					if (c != null) {
						c.setPhoto(context.get());
					}
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			if (mAdapter != null)
				mAdapter.notifyDataSetChanged();
		}
	}

	//endregion

	//endregion
}