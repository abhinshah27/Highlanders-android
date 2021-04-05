/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.settings;


import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.realm.RealmList;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.adapters.BasicAdapterInteractionsListener;
import rs.highlande.highlanders_app.adapters.SettingsCirclesFoldersAdapter;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.base.HLFragment;
import rs.highlande.highlanders_app.base.OnBackPressedListener;
import rs.highlande.highlanders_app.models.HLCircle;
import rs.highlande.highlanders_app.models.HLPosts;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.DialogUtils;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.websocket_connection.HLRequestTracker;
import rs.highlande.highlanders_app.websocket_connection.HLServerCalls;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author mbaldrighi on 3/21/2018.
 */
public class SettingsCirclesFoldersFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, OnBackPressedListener,
		BasicAdapterInteractionsListener, ListView.OnItemClickListener {

	public static final String LOG_TAG = SettingsCirclesFoldersFragment.class.getCanonicalName();

	public enum ViewType { CIRCLES, FOLDERS }
	private ViewType mViewType;

	private View title;
	private TextView addBtn;

	private ListView simpleListView;
	private List<CharSequence> simpleList = new ArrayList<>();
	private SettingsCirclesFoldersAdapter simpleListAdapter;

	private SwipeRefreshLayout srl;
	private TextView noResult;

	private MaterialDialog dialogRemove;
	private MaterialDialog dialogAddRename;

	private boolean goingToTimeline = false;

	private String selectedObject;
	private String newObjectName;


	public SettingsCirclesFoldersFragment() {
		// Required empty public constructor
	}

	public static SettingsCirclesFoldersFragment newInstance(ViewType viewType) {
		Bundle args = new Bundle();
		args.putSerializable(Constants.EXTRA_PARAM_1, viewType);
		SettingsCirclesFoldersFragment fragment = new SettingsCirclesFoldersFragment();
		fragment.setArguments(args);
		return fragment;
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_settings_circles_folders, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (getActivity() instanceof SettingsActivity)
			((SettingsActivity) getActivity()).setBackListener(this);

		if (Utils.isContextValid(getActivity()))
			simpleListAdapter = new SettingsCirclesFoldersAdapter(getActivity(), R.layout.item_settings_circle_folder,
					simpleList, mViewType, this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(
				getContext(),
				mViewType == ViewType.CIRCLES ?
						AnalyticsUtils.SETTINGS_CIRCLES : AnalyticsUtils.SETTINGS_FOLDERS
		);

		callServer(CallType.GET, null);

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (Utils.isContextValid(getActivity()))
			Utils.closeKeyboard(getActivity());

		if (goingToTimeline) {
			settingsActivityListener.setToolbarVisibility(false);
			settingsActivityListener.setBottomBarVisibility(false);
			settingsActivityListener.addRemoveTopPaddingFromFragmentContainer(false);
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				mViewType = (ViewType) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_1);
		}
	}

	@Override
	public void onBackPressed() {
		if ((dialogAddRename != null && dialogAddRename.isShowing()) ||
				(dialogRemove != null && dialogRemove.isShowing())) {

			if (dialogAddRename != null && dialogAddRename.isShowing())
				dialogAddRename.dismiss();
			if (dialogRemove != null && dialogRemove.isShowing())
				dialogRemove.dismiss();
		}
		else if (Utils.isContextValid(getActivity())) {
			if (getActivity() instanceof SettingsActivity)
				((SettingsActivity) getActivity()).setBackListener(null);

			getActivity().onBackPressed();
		}
	}



	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_add_new:
				showAddRenameDialog(null);
				break;
		}
	}

	// ListView's interface callback
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		CharSequence name = (CharSequence) parent.getAdapter().getItem(position);
		if (mViewType == ViewType.FOLDERS) {
			settingsActivityListener.showFoldersPostFragment((String) name);
			goingToTimeline = true;
		}
		else if (mViewType == ViewType.CIRCLES)
			settingsActivityListener.showInnerCircleSingleCircleFragment((String) name, null);
	}

	// Adapter's custom interface callback
	@Override
	public void onItemClick(Object object) {}

	@Override
	public void onItemClick(Object object, View view) {
		if (view.getId() == R.id.btn_edit)
			showAddRenameDialog(object.toString());
		else if (view.getId() == R.id.btn_remove)
			showDeleteDialog(object.toString());
	}

	@Override
	public HLUser getUser() {
		return mUser;
	}


	@Override
	public void handleSuccessResponse(final int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		if (responseObject == null || responseObject.length() == 0) {
			handleErrorResponse(operationId, 0);
			return;
		}

		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_GET:
			case Constants.SERVER_OP_FOLDERS_GET:
				JSONObject json = responseObject.optJSONObject(0);
				if (json != null && json.length() > 0) {
					if (mViewType == ViewType.CIRCLES)
						setData(json.optJSONArray("circles"));
					if (mViewType == ViewType.FOLDERS)
						setData(json.optJSONArray("lists"));
				}
				break;

			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE:
			case Constants.SERVER_OP_FOLDERS_CREATE:
			case Constants.SERVER_OP_FOLDERS_DELETE:
				if (simpleList != null && simpleListAdapter != null) {
					if (mType == CallType.ADD) {
						DialogUtils.closeDialog(dialogAddRename);
						if (Utils.isStringValid(newObjectName)) {
							simpleList.add(newObjectName);
							simpleListAdapter.notifyDataSetChanged();
						}
					} else if (mType == CallType.REMOVE) {

						if (mViewType == ViewType.FOLDERS)
							HLPosts.getInstance().updateDeletedFolder(selectedObject);

						DialogUtils.closeDialog(dialogRemove);
						if (Utils.isStringValid(selectedObject)) {
							simpleList.remove(selectedObject);
							simpleListAdapter.notifyDataSetChanged();
						}
					}
				}

				realm.executeTransaction(realm -> {
					if (operationId == Constants.SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE) {
						if (mType == CallType.REMOVE) {
							mUser.updateFiltersForSingleCircle(new HLCircle(selectedObject), false);
						}
					}
				});

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(CallType.GET, null),
						1000
				);
				break;

			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_RENAME:
			case Constants.SERVER_OP_FOLDERS_RENAME:
				if (Utils.areStringsValid(selectedObject, newObjectName)) {

					if (mViewType == ViewType.FOLDERS)
						HLPosts.getInstance().updateRenamedFolder(selectedObject, newObjectName);

					if (simpleList != null && simpleListAdapter != null) {
						int index = simpleList.indexOf(selectedObject);
						simpleList.remove(index);
						simpleList.add(index, newObjectName);
						simpleListAdapter.notifyDataSetChanged();
					}
				}

				DialogUtils.closeDialog(dialogAddRename);

				realm.executeTransaction(realm -> {
					if (operationId == Constants.SERVER_OP_SETTINGS_IC_CIRCLES_RENAME) {
						HLCircle circle = new HLCircle(selectedObject);

						if (mUser.getCircleObjects() != null &&
								mUser.getCircleObjects().contains(circle)) {
							int index = mUser.getCircleObjects().indexOf(circle);
							HLCircle c = mUser.getCircleObjects().get(index);
							if (c != null)
								c.setName(newObjectName);
						}

						if (mUser.getSelectedFeedFilters() != null &&
								mUser.getSelectedFeedFilters().contains(circle.getName())) {
							int index = mUser.getSelectedFeedFilters().indexOf(selectedObject);
							mUser.getSelectedFeedFilters().remove(index);
							mUser.getSelectedFeedFilters().add(index, newObjectName);
						}
					}
				});

				Utils.setRefreshingForSwipeLayout(srl, true);
				new Handler().postDelayed(
						() -> callServer(CallType.GET, null),
						1000
				);
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		@StringRes int msg = R.string.error_generic_list;
		switch (operationId) {
			case Constants.SERVER_OP_SETTINGS_IC_GET:
				break;
			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_ADD_DELETE:
			case Constants.SERVER_OP_SETTINGS_IC_CIRCLES_RENAME:
				msg = R.string.error_generic_operation;
				break;
		}

		activityListener.showAlert(msg);
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {
		title = view.findViewById(R.id.title);
		addBtn = view.findViewById(R.id.btn_add_new);
		addBtn.setOnClickListener(this);

		simpleListView = view.findViewById(R.id.circles_list);
		simpleListView.setOnItemClickListener(this);

		noResult = view.findViewById(R.id.no_result);
		srl = Utils.getGenericSwipeLayout(view, () -> {
			Utils.setRefreshingForSwipeLayout(srl, true);

			callServer(CallType.GET, null);
		});
	}

	@Override
	protected void setLayout() {

		settingsActivityListener.setToolbarVisibility(true);
		settingsActivityListener.setBottomBarVisibility(true);
		settingsActivityListener.addRemoveTopPaddingFromFragmentContainer(true);
		goingToTimeline = false;

		title.setVisibility(mViewType == ViewType.FOLDERS ? View.GONE : View.VISIBLE);

		settingsActivityListener.setToolbarTitle(mViewType == ViewType.CIRCLES ?
				R.string.settings_main_inner_circle : R.string.settings_main_lists);

		addBtn.setText(mViewType == ViewType.CIRCLES ? R.string.settings_circles_add : R.string.settings_folders_add);

		onResumeForListData();
	}


	private void setData(final JSONArray data) {
		realm.executeTransaction(realm -> {
			if (data != null && data.length() > 0) {
				RealmList<String> list = null;

				if (mViewType == ViewType.CIRCLES)
					list = mUser.getCircles();
				else if (mViewType == ViewType.FOLDERS)
					list = mUser.getFolders();

				if (list == null)
					list = new RealmList<>();
				else
					list.clear();

				if (mViewType == ViewType.CIRCLES) {
					if (mUser.getCircleObjectsWithEmpty() == null)
						mUser.setCircleObjectsWithEmpty(new RealmList<>());
					else
						mUser.getCircleObjectsWithEmpty().clear();
				}

				for (int i = 0; i < data.length(); i++) {
					if (mViewType == ViewType.CIRCLES) {
						JSONObject jName = data.optJSONObject(i);
						if (jName != null) {
							HLCircle circle = new HLCircle().deserializeToClass(jName);
							if (circle != null) {
								list.add(jName.optString("name"));
								mUser.getCircleObjectsWithEmpty().add(circle);
							}
						}
					} else if (mViewType == ViewType.FOLDERS) {
						list.add(data.optString(i));
					}
				}
			}
			else {
				if (mViewType == ViewType.CIRCLES) {
					mUser.setCircles(new RealmList<>());
					mUser.setCircleObjectsWithEmpty(new RealmList<>());
				}
				else if (mViewType == ViewType.FOLDERS)
					mUser.setFolders(new RealmList<>());

			}

			onResumeForListData();
		});
	}

	private void onResumeForListData() {
		simpleListView.setAdapter(simpleListAdapter);

		if (simpleList == null)
			simpleList = new ArrayList<>();
		else
			simpleList.clear();

		if (mViewType == ViewType.CIRCLES) {
			List<HLCircle> tmpSort = new ArrayList<>(mUser.getCircleObjectsWithEmpty());
			Collections.sort(tmpSort, HLCircle.CircleNameComparator);
			for (HLCircle c : tmpSort)
				simpleList.add(c.getNameToDisplay());
		}
		else simpleList.addAll(mUser.getFolders());

		simpleListAdapter.notifyDataSetChanged();

		if (simpleList.isEmpty()) {
			noResult.setText(mViewType == ViewType.CIRCLES ? R.string.no_result_circles : R.string.no_result_folders);
			noResult.setVisibility(View.VISIBLE);
		}
		else noResult.setVisibility(View.GONE);
	}


	public enum CallType { GET, RENAME, ADD, REMOVE }
	private CallType mType;
	private void callServer(CallType type, @Nullable Bundle bundle) {
		Object[] result = null;

		try {
			if (type == CallType.GET) {
				if (mViewType == ViewType.CIRCLES)
					result = HLServerCalls.getSettings(mUser.getUserId(), HLServerCalls.SettingType.CIRCLES);
				else if (mViewType == ViewType.FOLDERS)
					result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.GET, mUser.getUserId(), null);
			}
			else if (type == CallType.RENAME && bundle != null) {
				if (mViewType == ViewType.CIRCLES)
					result = HLServerCalls.settingsOperationsOnCircles(mUser.getUserId(), bundle, type);
				else if (mViewType == ViewType.FOLDERS)
					result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.RENAME, mUser.getUserId(), bundle);
			}
			else if ((type == CallType.ADD || type == CallType.REMOVE) && bundle != null) {
				if (mViewType == ViewType.CIRCLES)
					result = HLServerCalls.settingsOperationsOnCircles(mUser.getUserId(), bundle, type);
				else if (mViewType == ViewType.FOLDERS) {
					if (type == CallType.ADD)
						result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.CREATE, mUser.getUserId(), bundle);
					else
						result = HLServerCalls.manageLists(HLServerCalls.ListsCallType.DELETE, mUser.getUserId(), bundle);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity) {
			HLRequestTracker.getInstance(((HLApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
		}
	}


	private void showAddRenameDialog(@Nullable final String objectName) {
		if (Utils.isContextValid(getContext())) {
			dialogAddRename = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_circle_folder_rename);
			if (dialogAddRename != null) {
				View view = dialogAddRename.getCustomView();
				if (view != null) {
					final EditText newName = view.findViewById(R.id.rename_circle_edittext);

					final TextView positive = DialogUtils.setPositiveButton(
							view.findViewById(R.id.button_positive),
							Utils.isStringValid(selectedObject = objectName) ?
									R.string.action_rename : R.string.action_save,
							v -> {
								String sName = newName.getText().toString();
								if (Utils.isStringValid(sName)) {
									Bundle bundle = new Bundle();
									newObjectName = sName;

									if (mViewType == ViewType.CIRCLES) {
										if (Utils.isStringValid(objectName)) {
											bundle.putString("oldCircleName", objectName);
											bundle.putString("newCircleName", sName);
											callServer(mType = CallType.RENAME, bundle);
										} else {
											bundle.putString("circleName", sName);
											bundle.putString("operation", "a");
											callServer(mType = CallType.ADD, bundle);
										}
									} else if (mViewType == ViewType.FOLDERS) {
										if (Utils.isStringValid(objectName)) {
											bundle.putString("oldListName", objectName);
											bundle.putString("newListName", sName);
											callServer(mType = CallType.RENAME, bundle);
										} else {
											bundle.putString("listID", sName);
											callServer(mType = CallType.ADD, bundle);
										}
									}
								}
							});
					positive.setEnabled(false);

					newName.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {}

						@Override
						public void afterTextChanged(Editable s) {
							boolean condition = s != null && s.length() > 0;

							if (mViewType == ViewType.CIRCLES) {
								String ic = Constants.INNER_CIRCLE_NAME.trim().replaceAll("\\s", "");
								String family = Constants.CIRCLE_FAMILY_NAME.trim().replaceAll("\\s", "");
								condition = s != null && s.length() > 0 &&
										!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(ic) &&
										!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(family);

								if (objectName != null) {
									condition = s != null && s.length() > 0 &&
											!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(ic) &&
											!s.toString().trim().replaceAll("\\s", "").equalsIgnoreCase(family) &&
											!s.toString().trim().equals(objectName);
								}
							}
							else if (mViewType == ViewType.FOLDERS && objectName != null) {
								condition = s != null && s.length() > 0 &&
										!s.toString().trim().equals(objectName);
							}

							positive.setEnabled(condition);
						}
					});

					view.findViewById(R.id.button_negative).setOnClickListener(v -> dialogAddRename.dismiss());

					if (Utils.isStringValid(objectName)) {
						newName.setText(objectName);
						newName.setHint(R.string.settings_circle_rename_hint);
						((TextView) view.findViewById(R.id.dialog_title)).setText(
								mViewType == ViewType.FOLDERS ?
										R.string.dialog_folder_title_rename : R.string.dialog_circle_title_rename
						);
					}
					else {
						newName.setHint(R.string.dialog_create_list_hint);
						((TextView) view.findViewById(R.id.dialog_title)).setText(
								mViewType == ViewType.FOLDERS ?
										R.string.dialog_folder_title_add : R.string.dialog_circle_title_add
						);
					}
				}

				dialogAddRename.show();

				DialogUtils.openKeyboardForDialog(dialogAddRename);
			}
		}
	}


	private void showDeleteDialog(final String objectName) {
		if (Utils.isContextValid(getContext()) && Utils.isStringValid(objectName)) {
			selectedObject = objectName;
			dialogRemove = DialogUtils.createGenericAlertCustomView(getContext(), R.layout.custom_dialog_delete_circle_folder);
			if (dialogRemove != null) {
				View view = dialogRemove.getCustomView();
				if (view != null) {
					final TextView message = view.findViewById(R.id.dialog_message);

					String sMessage = getString(
							mViewType == ViewType.FOLDERS ?
									R.string.dialog_delete_folder_message : R.string.dialog_delete_circle_message,
							objectName);
					int start = sMessage.indexOf(objectName);
					int end = sMessage.lastIndexOf(objectName);

					SpannableStringBuilder spannableString = new SpannableStringBuilder(sMessage);
					spannableString.setSpan(
							new ForegroundColorSpan(
									Utils.getColor(getContext(),
											R.color.colorAccent)
							),
							start,
							end,
							Spannable.SPAN_INCLUSIVE_EXCLUSIVE
					);
					message.setText(spannableString);

					DialogUtils.setPositiveButton(view.findViewById(R.id.button_positive), R.string.action_delete, v -> {
						Bundle bundle = new Bundle();
						if (mViewType == ViewType.CIRCLES) {
							bundle.putString("circleName", objectName);
							bundle.putString("operation", "d");
						}
						else if (mViewType == ViewType.FOLDERS)
							bundle.putString("listID", objectName);

						callServer(mType = CallType.REMOVE, bundle);
					});

					view.findViewById(R.id.button_negative).setOnClickListener(v -> dialogRemove.dismiss());
				}

				dialogRemove.show();
			}
		}
	}

	//endregion

}
