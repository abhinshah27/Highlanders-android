/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.HomeActivity;
import rs.highlande.highlanders_app.base.HLFragment;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.helpers.NotificationAndRequestHelper;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NotificationsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NotificationsFragment extends HLFragment {

	public static final String LOG_TAG = NotificationsFragment.class.getCanonicalName();

	private NotificationAndRequestHelper mHelper;

	private OnNotificationsFragmentInteractionListener mListener;


	public NotificationsFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment InnerCircleFragment.
	 */
	public static NotificationsFragment newInstance() {
		NotificationsFragment fragment = new NotificationsFragment();
		Bundle args = new Bundle();
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

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.home_fragment_notifications, container, false);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		configureLayout(view);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.ME_NOTIFICATION);

		if (mHelper != null)
			mHelper.onResume();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mHelper != null)
			mHelper.onPause();
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		if (context instanceof OnNotificationsFragmentInteractionListener)
			mListener = (OnNotificationsFragmentInteractionListener) context;
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);

		if (context instanceof OnNotificationsFragmentInteractionListener)
			mListener = (OnNotificationsFragmentInteractionListener) context;
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {

		}
	}

	@Override
	protected void configureResponseReceiver() {}



	//region == Class custom methods ==

	@Override
	protected void configureLayout(@NonNull View view) {

		if (mHelper == null && mListener != null)
			mHelper = mListener.getNotificationHelper();

		if (mHelper != null)
			mHelper.configureNotificationLayout(view);

		view.findViewById(R.id.back_arrow).setOnClickListener(
				v -> {
					startActivity(
							new Intent(view.getContext(), HomeActivity.class) {{
								this.putExtra(Constants.EXTRA_PARAM_1, (HomeActivity.getCurrentPagerItem() == -1) ? HomeActivity.PAGER_ITEM_TIMELINE : HomeActivity.getCurrentPagerItem());
							}}
					);

					if (getActivity() != null) getActivity().finish();

//					if (getActivity() instanceof ProfileActivity)
//						getActivity().onBackPressed();
				}
		);
	}

	@Override
	protected void setLayout() {
		// bottom bar is no longer here. Dot handling no use.
//		if (mHelper != null)
//			mHelper.handleDotVisibility(false);
	}

	public Fragment getFragment() {
		return this;
	}

	public HLUser getUser() {
		return mUser;
	}

	//endregion


	public interface OnNotificationsFragmentInteractionListener {
		NotificationAndRequestHelper getNotificationHelper();
	}

}