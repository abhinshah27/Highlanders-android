/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import io.realm.RealmObject;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.activities_and_fragments.ViewAllTagsActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_chat.ChatRoomsFragment;
import rs.highlande.highlanders_app.activities_and_fragments.activities_create_post.CreatePostActivityMod;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.global_search.GlobalSearchFragment;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile.ProfileActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile.ProfileFragment;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile.ProfileHelper;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.timeline.LandscapeMediaActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.timeline.OnTimelineFragmentInteractionListener;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.timeline.PostOverlayActionActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.timeline.TimelineFragment;
import rs.highlande.highlanders_app.base.BasicInteractionListener;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.base.OnBackPressedListener;
import rs.highlande.highlanders_app.models.HLNotifications;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.models.Post;
import rs.highlande.highlanders_app.models.Tag;
import rs.highlande.highlanders_app.models.chat.ChatRoom;
import rs.highlande.highlanders_app.services.SendFCMTokenService;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.FullScreenHelper;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;
import rs.highlande.highlanders_app.utility.helpers.NotificationAndRequestHelper;
import rs.highlande.highlanders_app.utility.helpers.PostBottomSheetHelper;
import rs.highlande.highlanders_app.utility.helpers.RealTimeCommunicationHelperKotlin;
import rs.highlande.highlanders_app.utility.realm.RealmUtils;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;
import rs.highlande.highlanders_app.websocket_connection.ServerMessageReceiver;
import rs.highlande.highlanders_app.widgets.HLViewPagerNoScroll;

public class HomeActivity extends HLActivity implements View.OnClickListener, BasicInteractionListener,
		ViewPager.OnPageChangeListener, OnMissingConnectionListener,
		OnServerMessageReceivedListener,
		GestureDetector.OnGestureListener,
		OnTimelineFragmentInteractionListener,
		NotificationAndRequestHelper.OnNotificationHelpListener {

	public static final int PAGER_ITEM_GLOBAL_SEARCH = 0;
	public static final int PAGER_ITEM_TIMELINE = 1;
	public static final int PAGER_ITEM_PROFILE = 2;
	public static final int PAGER_ITEM_CHATS = 3;

	private static int currentPagerItem = -1;

	/**
	 * Serves for TIMELINE pagination purpose.
	 */
	private int lastPageID = 1;

	private View rootView;

	private View bottomBar;
	private View l1, l2, l3, l4;
	private ImageView ib1,ib2, ib3, ib4;
	private TransitionDrawable td1, td2, td3, td4;
	private ImageView main;
	private View notificationsDot;
	private View notificationsDotChat;

	private HLViewPagerNoScroll viewPager;
	private HLHomePagerAdapter adapter;

	private PostBottomSheetHelper bottomSheetHelper;

	private GestureDetector mDetector;
	private MediaHelper mediaHelper;

	private Integer lastAdapterPosition = null;

	private FullScreenHelper fullScreenListener;
	private FullScreenHelper.FullScreenType fullScreenSavedState;
	private FullScreenHelper.RestoreFullScreenStateListener fsStateListener;

	private OnBackPressedListener backListener;

	private NotificationAndRequestHelper notificationHelper;
	private boolean hasOrFromNotification = false, hasChatRelatedNotification = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(rootView = LayoutInflater.from(this).inflate(R.layout.activity_home, null, false));
		setRootContent(R.id.root_content);

		/* SETS ENTER FADE TRANSITION */
		if (Utils.hasLollipop()) {
//			getWindow().setEnterTransition(new Fade());
			getWindow().setExitTransition(new Fade());
		}

//		mDetector = new GestureDetector(this, this);
		fullScreenListener = new FullScreenHelper(this, false);
		mediaHelper = new MediaHelper();

		notificationHelper = new NotificationAndRequestHelper(this, this);

		bottomBar = findViewById(R.id.bottom_bar);

//		fullScreenListener.setBottomBarExpHeight(bottomBar.getHeight());
//		fullScreenListener.setToolbarExpHeight(toolbar.getHeight());

		viewPager = findViewById(R.id.pager);
		adapter = new HLHomePagerAdapter(getSupportFragmentManager());
		viewPager.addOnPageChangeListener(this);

		configurePostOptionsSheets();

		manageIntent();
		configureBottomBar(bottomBar);
	}

	@Override
	protected void onStart() {
		super.onStart();

		SendFCMTokenService.startService(getApplicationContext());

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!RealmObject.isValid(mUser)) {
			if (!RealmUtils.isValid(realm))
				realm = RealmUtils.getCheckedRealm();
			mUser = new HLUser().readUser(realm);
		}

		if (notificationHelper != null)
			notificationHelper.updateNotifications();

		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(currentPagerItem);

		if (bottomSheetHelper != null)
			bottomSheetHelper.onResume();

		handleChatToReadDot(currentPagerItem);
		NotificationAndRequestHelper.handleDotVisibility(notificationsDot, mUser.isValid());
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (notificationHelper != null)
			notificationHelper.onPause();

		if (bottomSheetHelper != null)
			bottomSheetHelper.onPause();
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();

		if (weakTimeline != null && weakTimeline.get() != null && weakTimeline.get().hasFilterVisible()) {
			weakTimeline.get().handleFilterToggle();
		}
		else {
			if (!Utils.checkAndOpenLogin(this, mUser)) {
				switch (id) {
					case R.id.main_action_btn:
						goToCreatePost();
						break;

					case R.id.bottom_timeline:
						viewPager.setCurrentItem(PAGER_ITEM_TIMELINE);
						break;
					case R.id.bottom_profile:
						viewPager.setCurrentItem(PAGER_ITEM_PROFILE);
						break;
					case R.id.bottom_chats:
						viewPager.setCurrentItem(PAGER_ITEM_CHATS);
						break;
					case R.id.bottom_global_search:
						viewPager.setCurrentItem(PAGER_ITEM_GLOBAL_SEARCH);
						break;
				}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// caught when returning from PostOverlayActivity
			case Constants.RESULT_TIMELINE_INTERACTIONS:
				restoreFullScreenState();
				break;

			case Constants.RESULT_SINGLE_POST:
				break;

			case Constants.RESULT_SELECT_IDENTITY:
				if (currentPagerItem == PAGER_ITEM_PROFILE && weakProfile != null) {
					ProfileFragment frg = weakProfile.get();
					if (frg != null && frg.isVisible()) {
						frg.setProfileType(mUser.getProfileType());
						frg.callServer(ProfileFragment.CallType.PROFILE, null);
					}
				}
				break;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		manageIntent(intent, true);
	}

	@Override
	public void onBackPressed() {
		if (backListener != null)
			backListener.onBackPressed();
		else {

			showExitActionMessage();
		}
	}

	private void configureBottomBar(final View bar) {
		if (bar != null) {
			l1 = bar.findViewById(R.id.bottom_timeline);
			l1.setOnClickListener(this);
			l2 = bar.findViewById(R.id.bottom_profile);
			l2.setOnClickListener(this);
			l3 = bar.findViewById(R.id.bottom_chats);
			l3.setOnClickListener(this);
			l4 = bar.findViewById(R.id.bottom_global_search);
			l4.setOnClickListener(this);

			ib1 = bar.findViewById(R.id.icon_timeline);
			td1 = (TransitionDrawable) ib1.getDrawable();
			td1.setCrossFadeEnabled(true);
			ib2 = bar.findViewById(R.id.icon_profile);
			td2 = (TransitionDrawable) ib2.getDrawable();
			td2.setCrossFadeEnabled(true);
			ib3 = bar.findViewById(R.id.icon_chats);
			td3 = (TransitionDrawable) ib3.getDrawable();
			td3.setCrossFadeEnabled(true);
			ib4 = bar.findViewById(R.id.icon_global_search);
			td4 = (TransitionDrawable) ib4.getDrawable();
			td4.setCrossFadeEnabled(true);

			if (mUser.isValid()) {
				if (!hasOrFromNotification && !hasChatRelatedNotification) {
					l1.setSelected(true);
					td1.startTransition(0);
				}
				else {
					if (hasOrFromNotification) {
						l2.setSelected(true);
						setBottomBar(false, PAGER_ITEM_PROFILE);
						hasOrFromNotification = false;
					}
					else {
						l3.setSelected(true);
						setBottomBar(true, PAGER_ITEM_CHATS);
						hasChatRelatedNotification = false;
					}
				}
			}
			else {
				l4.setSelected(true);
				setBottomBar(true, PAGER_ITEM_GLOBAL_SEARCH);
			}

			main = bar.findViewById(R.id.main_action_btn);
			main.setOnClickListener(this);

			notificationsDot = bar.findViewById(R.id.notification_dot);

			notificationsDotChat = bar.findViewById(R.id.notification_dot_chat);

			/*
			bar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					fullScreenListener.setBottomBarExpHeight(bar.getHeight());
					bar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			});
			*/
		}
	}

	private void setBottomBar(boolean black, int currentPagerItem) {
		if (bottomBar != null) {
			if (ib1 != null) {
				ib1.setImageResource(black ? R.drawable.transition_timeline_black : R.drawable.transition_timeline);
				td1 = (TransitionDrawable) ib1.getDrawable();
			}
			if (ib2 != null) {
				ib2.setImageResource(black ? R.drawable.transition_aboutme_black_2 : R.drawable.transition_aboutme_2);
				td2 = (TransitionDrawable) ib2.getDrawable();
			}
			if (ib3 != null) {
				ib3.setImageResource(black ? R.drawable.transition_chats_black : R.drawable.transition_chats);
				td3 = (TransitionDrawable) ib3.getDrawable();
			}
			if (ib4 != null) {
				ib4.setImageResource(black ? R.drawable.transition_global_search_black : R.drawable.transition_global_search);
				td4 = (TransitionDrawable) ib4.getDrawable();
			}

			switch (currentPagerItem) {
				case PAGER_ITEM_TIMELINE:
					td1.setCrossFadeEnabled(true);
					td1.startTransition(0);
					td2.setCrossFadeEnabled(true);
					td3.setCrossFadeEnabled(true);
					td4.setCrossFadeEnabled(true);
					break;
				case PAGER_ITEM_PROFILE:
					td1.setCrossFadeEnabled(true);
					td2.setCrossFadeEnabled(true);
					td2.startTransition(0);
					td3.setCrossFadeEnabled(true);
					td4.setCrossFadeEnabled(true);
					break;
				case PAGER_ITEM_CHATS:
					td1.setCrossFadeEnabled(true);
					td2.setCrossFadeEnabled(true);
					td3.setCrossFadeEnabled(true);
					td3.startTransition(0);
					td4.setCrossFadeEnabled(true);
					break;
				case PAGER_ITEM_GLOBAL_SEARCH:
					td1.setCrossFadeEnabled(true);
					td2.setCrossFadeEnabled(true);
					td3.setCrossFadeEnabled(true);
					td4.setCrossFadeEnabled(true);
					td4.startTransition(0);
					break;
			}
		}
	}

	private void goToCreatePost() {
		startActivityForResult(new Intent(this, CreatePostActivityMod.class), Constants.RESULT_CREATE_POST);
	}


	@Override public void openPostSheet(@NonNull String postId, boolean isOwnPost) {
		if (bottomSheetHelper != null)
			bottomSheetHelper.openPostSheet(postId, isOwnPost);
	}

	@Override
	public void closePostSheet() {
		if (bottomSheetHelper != null)
			bottomSheetHelper.closePostSheet();
	}

	@Override
	public boolean isPostSheetOpen() {
		return isPostSheetOpen(null);
	}

	public boolean isPostSheetOpen(SlidingUpPanelLayout layout) {
		return bottomSheetHelper != null && bottomSheetHelper.isPostSheetOpen(layout);
	}


	@Override
	public void viewAllTags(Post post) {
		if (post != null) {
			if (post.hasTags()) {
				ArrayList<Tag> temp = new ArrayList<>();
				temp.addAll(post.getTags());

				Intent intent = new Intent(this, ViewAllTagsActivity.class);
				intent.putParcelableArrayListExtra(Constants.EXTRA_PARAM_1, temp);
				intent.putExtra(Constants.EXTRA_PARAM_2, true);
				startActivity(intent);
			}
		}
	}

	/*
	private void editPost() {
		goToCreatePost(true);
	}

	private void deletePost() {
		callToServer(CallType.DELETE, null, null);
	}

	private void flagPost(final FlagType type) {
		moderationDialog = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_flag_post);

		@StringRes int positive = -1;
		@StringRes int title = -1;
		@StringRes int message = -1;
		if (type != null) {
			switch (type) {
				case BLOCK:
					positive = R.string.action_block;
					title = R.string.option_post_block;
					message = R.string.flag_block_user_message;
					break;
				case HIDE:
					positive = R.string.action_hide;
					title = R.string.option_post_hide;
					message = R.string.flag_hide_post_message;
					break;
				case REPORT:
					positive = R.string.action_report;
					title = R.string.flag_report_post_title;
					message = R.string.flag_report_post_message;
					break;
			}

			if (moderationDialog != null) {
				View v = moderationDialog.getCustomView();
				if (v != null) {
					((TextView) v.findViewById(R.id.dialog_flag_title)).setText(title);
					((TextView) v.findViewById(R.id.dialog_flag_message)).setText(message);

					final View errorMessage = v.findViewById(R.id.error_empty_message);

					final EditText editText = v.findViewById(R.id.report_post_edittext);
					editText.setVisibility(type == FlagType.REPORT ? View.VISIBLE : View.GONE);
					editText.addTextChangedListener(new TextWatcher() {
						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {}

						@Override
						public void afterTextChanged(Editable s) {
							if (errorMessage.getAlpha() == 1 && s.length() > 0)
								errorMessage.animate().alpha(0).setDuration(200).start();
						}
					});

					Button positiveBtn = v.findViewById(R.id.button_positive);
					positiveBtn.setText(positive);
					positiveBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							switch (type) {
								case BLOCK:
									blockUser();
									break;
								case HIDE:
									hidePost();
									break;
								case REPORT:
									String msg = editText.getText().toString();
									if (Utils.isStringValid(msg))
										reportPost(msg);
									else
										errorMessage.animate().alpha(1).setDuration(200).start();

									break;
							}
						}
					});

					Button negativeBtn = v.findViewById(R.id.button_negative);
					negativeBtn.setText(R.string.cancel);
					negativeBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							moderationDialog.dismiss();
						}
					});
				}
				moderationDialog.show();
				bottomSheetPostOther.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
			}
		}
	}

	private void blockUser() {
		callToServer(CallType.FLAG, FlagType.BLOCK, null);
	}

	private void hidePost() {
		callToServer(CallType.FLAG, FlagType.HIDE, null);
	}

	private void reportPost(String reasons) {
		callToServer(CallType.FLAG, FlagType.REPORT, reasons);
	}

	private void callToServer(CallType type, @Nullable FlagType flagType, String reportMessage) {
		Post post = HLPosts.getInstance().getPost(postIdForActions);

		Object[] results = null;
		if (type != null) {
			try {
				if (mUser != null) {
					if (type == CallType.DELETE || (type == CallType.FLAG && flagType == FlagType.HIDE))
						results = HLServerCalls.deletePost(mUser.getId(), postIdForActions);
					else if (flagType != null && post != null) {
						switch (flagType) {
							case BLOCK:
								results = HLServerCalls.blockUnblockUsers(
										mUser.getId(),
										post.getAuthorId(),
										UnBlockUserEnum.BLOCK
								);
								break;
							case REPORT:
								results = HLServerCalls.report(
										HLServerCalls.CallType.POST,
										mUser.getId(),
										postIdForActions,
										reportMessage
								);
								break;
						}
					}

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			HLRequestTracker.getInstance(((HLApp) getApplication())).handleCallResult(this, this, results);
		}
	}
	*/

	private void configurePostOptionsSheets() {
		if (bottomSheetHelper == null) {
			bottomSheetHelper = new PostBottomSheetHelper(this);
		}
		bottomSheetHelper.configurePostOptionsSheets(rootView);
	}


	/*
	 * SERVER CALLS HANDLING
	 */
	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		if (responseObject == null || responseObject.length() == 0)
			return;
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		switch (operationId) {

		}
	}

	@Override
	public void onMissingConnection(int operationId) {}

	/*
	 * TIMELINE fragment
	 */

//	@Override
//	public void onManageTapForFullScreen(View clickedView, ValueAnimator maskOnUpper, ValueAnimator maskOnLower,
//	                                     ValueAnimator maskOffUpper, ValueAnimator maskOffLower) {
//		int id = clickedView.getId();
//		switch (id) {
//			case R.id.post_main_view:
//				if (fullScreenListener != null) {
////					if (fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.NONE)
////						fullScreenListener.goFullscreen(toolbar, bottomBar);
////					else if (fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.FULL_SCREEN &&
////							maskOnUpper != null && maskOnLower != null)
////						fullScreenListener.applyPostMask(maskOnUpper, maskOnLower);
////					else if (fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.POST_MASK &&
////							maskOffUpper != null && maskOffLower != null) {
////						fullScreenListener.exitFullscreen(toolbar, bottomBar,
////								maskOffUpper, maskOffLower);
////					}
//				}
//				break;
//
//			case R.id.button_hearts:
//				Toast.makeText(this, "Open HEARTS", Toast.LENGTH_SHORT).show();
//				break;
//
//			case R.id.button_comments:
//				Toast.makeText(this, "Open COMMENTS", Toast.LENGTH_SHORT).show();
//				break;
//
//			case R.id.button_shares:
//				Toast.makeText(this, "Open SHARES", Toast.LENGTH_SHORT).show();
//				break;
//
//			case R.id.button_pin:
//				Toast.makeText(this, "Open PIN", Toast.LENGTH_SHORT).show();
//				break;
//		}
//	}

	@Override
	public Toolbar getToolbar() {
		return null;
	}

	@Override
	public View getBottomBar() {
		return bottomBar;
	}

	@Override
	public FullScreenHelper getFullScreenListener() {
		return fullScreenListener;
	}

	@Override
	public void actionsForLandscape(@NonNull String postId, View view) {
		Intent intent = new Intent(this, LandscapeMediaActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, postId);
		if (Utils.hasLollipop()) {
			ActivityOptions options = ActivityOptions
					.makeSceneTransitionAnimation(this, view, Constants.TRANSITION_LANDSCAPE);
			startActivity(intent, options.toBundle());
		}
		else {
			startActivity(intent);
			overridePendingTransition(0, 0);
		}
	}

	@Override
	public void setLastAdapterPosition(int position) {
		this.lastAdapterPosition = position;
	}
	@Override
	public Integer getLastAdapterPosition() {
		return lastAdapterPosition;
	}

	public boolean isPostMaskUpVisible() {
		return fullScreenListener != null && fullScreenListener.getFullScreenType() == FullScreenHelper.FullScreenType.POST_MASK;
	}

	public ValueAnimator getMaskAlphaAnimation(boolean on, View mask) {
		if (fullScreenListener != null)
			return on ? fullScreenListener.getMaskAlphaAnimationOn(mask) : fullScreenListener.getMaskAlphaAnimationOff(mask);

		return null;
	}

	@Override
	public MediaHelper getMediaHelper() {
		return mediaHelper != null ? mediaHelper :(mediaHelper = new MediaHelper());
	}

	@Override
	public void goToInteractionsActivity(@NonNull String postId) {
		Intent intent = new Intent(this, PostOverlayActionActivity.class);
		intent.putExtra(Constants.EXTRA_PARAM_1, postId);
		startActivityForResult(intent, Constants.RESULT_TIMELINE_INTERACTIONS);
	}

	@Override
	public void saveFullScreenState() {
		fullScreenSavedState = fullScreenListener.getFullScreenType();
	}

	@Override
	public void setFsStateListener(FullScreenHelper.RestoreFullScreenStateListener fsStateListener) {
		this.fsStateListener = fsStateListener;
	}

	private void restoreFullScreenState() {
		if (fsStateListener != null)
			fsStateListener.restoreFullScreenState(fullScreenSavedState);
	}

	@Override
	public int getPageIdToCall() {
		return lastPageID + 1;
	}

	@Override
	public int getLastPageID() {
		return lastPageID;
	}

	@Override
	public void setLastPageID(int lastPageID) {
		this.lastPageID = lastPageID;
	}

	@Override
	public HLActivity getActivity() {
		return this;
	}

	@Override
	public void goToProfile(@NonNull String userId, boolean isInterest) {
		if (Utils.isStringValid(userId)) {
			if (userId.equals(mUser.getId()))
				viewPager.setCurrentItem(PAGER_ITEM_PROFILE);
			else {
				ProfileActivity.openProfileCardFragment(
						this,
						isInterest ?
								ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND,
						userId,
						HomeActivity.PAGER_ITEM_TIMELINE
				);
			}
		}
	}

	@Override
	protected void manageIntent() {
		manageIntent(getIntent(), false);
	}


	private void manageIntent(Intent intent, boolean newIntent) {
		if (intent != null) {
			if (intent.hasExtra(Constants.KEY_NOTIFICATION_RECEIVED)) {
				String code = intent.getStringExtra(Constants.KEY_NOTIFICATION_RECEIVED);
				if (Utils.isStringValid(code)) {
					switch (code) {
						case Constants.CODE_NOTIFICATION_GENERIC:
							hasOrFromNotification = true;
							break;
						case Constants.CODE_NOTIFICATION_CHAT_UNSENT_MESSAGES:
						case Constants.CODE_NOTIFICATION_CHAT:
							hasChatRelatedNotification = true;
					}
				}
			}

			// HomeActivity should no longer receive generic notifications
			if (hasOrFromNotification) {
				intent.putExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_PROFILE);
//				intent.putExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_NOTIFICATIONS);
				hasOrFromNotification = false;
			}
			else if (hasChatRelatedNotification) {
				intent.putExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_CHATS);
				hasChatRelatedNotification = false;
			}

			if (newIntent)
				viewPager.setCurrentItem(intent.getIntExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_TIMELINE));
			else {
				if (intent.hasExtra(Constants.EXTRA_PARAM_1)) {
					currentPagerItem = intent.getIntExtra(Constants.EXTRA_PARAM_1, PAGER_ITEM_TIMELINE);
//					if (currentPagerItem == PAGER_ITEM_NOTIFICATIONS)
					if (currentPagerItem == PAGER_ITEM_PROFILE)
						hasOrFromNotification = true;
					else if (currentPagerItem == PAGER_ITEM_CHATS)
						hasChatRelatedNotification = true;
				}
				else {
					if (!mUser.isValid())
						currentPagerItem = PAGER_ITEM_GLOBAL_SEARCH;
					else {
						if (hasOrFromNotification)
							currentPagerItem = PAGER_ITEM_PROFILE;
						else if (hasChatRelatedNotification)
							currentPagerItem = PAGER_ITEM_CHATS;
						else
							currentPagerItem = PAGER_ITEM_TIMELINE;
					}
				}
			}
		}
	}


	private void showExitActionMessage() {
		showAlert(R.string.action_exit_snack, R.string.action_leave, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}


	//region == NotificationHelper interface


//	@Override
//	public NotificationAndRequestHelper getNotificationHelper() {
//		return notificationHelper;
//	}

	@Override
	public View getBottomBarNotificationDot() {
		return notificationsDot;
	}

	@Override
	public String getUserId() {
		realm = RealmUtils.checkAndFetchRealm(realm);
		mUser = RealmUtils.checkAndFetchUser(realm, mUser);
		if (mUser.isValid())
			return mUser.getId();
		else
			return null;
	}

//	private ViewProviderForNotifications viewProvider;
//	public interface ViewProviderForNotifications {
//		TextView getUnreadTextView();
//		View getToolbarNotificationsDot();
//		View getToolbarNotificationsIcon();
//	}
//
//	public void setViewProvider(ViewProviderForNotifications viewProvider) {
//		this.viewProvider = viewProvider;
//	}

	//endregion


	//region == GestureDetector Events ==

	@Override
	public boolean onTouchEvent(MotionEvent event) {
//		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
//				fullScreenListener.goFullscreen(toolbar, bottomBar);
			}
		}, 2 * Constants.TIME_UNIT_SECOND);
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	//endregion


	private WeakReference<ProfileFragment> weakProfile;
	private WeakReference<TimelineFragment> weakTimeline;
	private WeakReference<ChatRoomsFragment> weakChatRooms;
	private class HLHomePagerAdapter extends FragmentStatePagerAdapter {

		public HLHomePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case PAGER_ITEM_GLOBAL_SEARCH:
					return GlobalSearchFragment.newInstance();

				case PAGER_ITEM_TIMELINE:
					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);

					TimelineFragment.FragmentUsageType type = TimelineFragment.FragmentUsageType.GUEST;
					String listName = "publicposts";
					if (mUser.isValid()) {
						type = TimelineFragment.FragmentUsageType.TIMELINE;
						listName = null;
					}

					weakTimeline = new WeakReference<>(
							TimelineFragment.newInstance(type, null, null, null,
									null, listName, null)
					);
					return weakTimeline.get();

				case PAGER_ITEM_PROFILE:

					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);

					weakProfile = new WeakReference<>(
							ProfileFragment.newInstance(
									mUser.getProfileType(),
									null,
									PAGER_ITEM_PROFILE
							)
					);
					return weakProfile.get();

				case PAGER_ITEM_CHATS:
					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);

					weakChatRooms = new WeakReference<>(ChatRoomsFragment.newInstance());
					return weakChatRooms.get();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 4;
		}
	}


	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

	@Override
	public void onPageSelected(int position) {
		HLApp.profileFragmentVisible = false;
		HLApp.chatRoomsFragmentVisible = false;

		handleChatToReadDot(position);
		NotificationAndRequestHelper.handleDotVisibility(notificationsDot, mUser.isValid());

		RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListenerChat(null);

		switch (position) {
			case PAGER_ITEM_TIMELINE:

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_TIMELINE_FEED);

				switch (currentPagerItem) {
					case PAGER_ITEM_PROFILE:
//						setBottomBar(false, position);
						td2.reverseTransition(100);
						break;
					case PAGER_ITEM_CHATS:
						setBottomBar(false, position);
//						td3.reverseTransition(100);
						break;
					case PAGER_ITEM_GLOBAL_SEARCH:
						setBottomBar(false, position);
//						td4.reverseTransition(100);
						break;
				}
				l1.setSelected(true);
				td1.startTransition(200);

				l2.setSelected(false);
				l3.setSelected(false);
				l4.setSelected(false);
				break;

			case PAGER_ITEM_PROFILE:

				HLApp.profileFragmentVisible = true;
				NotificationAndRequestHelper.handleDotVisibility(notificationsDot, mUser.isValid());

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_ME_PAGE);

				switch (currentPagerItem) {
					case PAGER_ITEM_TIMELINE:
//						setBottomBar(false, position);
						td1.reverseTransition(100);
						break;
					case PAGER_ITEM_CHATS:
						setBottomBar(false, position);
//						td3.reverseTransition(100);
						break;
					case PAGER_ITEM_GLOBAL_SEARCH:
						setBottomBar(false, position);
//						td4.reverseTransition(100);
						break;
				}
				l2.setSelected(true);
				td2.startTransition(200);

				l1.setSelected(false);
				l3.setSelected(false);
				l4.setSelected(false);

				if (weakProfile != null) {
					ProfileFragment frg = weakProfile.get();
//					if (frg != null && frg.isVisible())
//						frg.callServer(ProfileFragment.CallType.PROFILE, null);
				}

				if (!mUser.isActingAsInterest()) {
					HLNotifications.getInstance().callForNotifications(this, this, mUser.getId(), false);
					HLNotifications.getInstance().callForNotifRequests(this, this, mUser.getId(), false);
				}
				break;

//			case PAGER_ITEM_NOTIFICATIONS:
//
//				// logic moved to profile
//				HLApp.notificationsFragmentVisible = true;
//
//				AnalyticsUtils.trackScreen(this, AnalyticsUtils.ME_NOTIFICATION);
//
//				switch (currentPagerItem) {
//					case PAGER_ITEM_TIMELINE:
//						setBottomBar(true, position);
////						td1.reverseTransition(100);
//						break;
//					case PAGER_ITEM_PROFILE:
//						setBottomBar(true, position);
////						td2.reverseTransition(100);
//						break;
//					case PAGER_ITEM_GLOBAL_SEARCH:
////						setBottomBar(true, position);
//						td4.reverseTransition(100);
//						break;
//				}
//				l3.setSelected(true);
//				td3.startTransition(200);
//
//				l1.setSelected(false);
//				l2.setSelected(false);
//				l4.setSelected(false);
//				break;

			case PAGER_ITEM_CHATS:

				NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				if (mManager != null)
					mManager.cancel(Constants.NOTIFICATION_CHAT_ID);

				HLApp.chatRoomsFragmentVisible = true;
				notificationsDotChat.setVisibility(View.GONE);

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_CHAT_ROOMS);
				if (weakChatRooms != null && weakChatRooms.get() != null)
					RealTimeCommunicationHelperKotlin.Companion.getInstance(this).setListenerChat(weakChatRooms.get());

				switch (currentPagerItem) {
					case PAGER_ITEM_TIMELINE:
						setBottomBar(true, position);
//						td1.reverseTransition(100);
						break;
					case PAGER_ITEM_PROFILE:
						setBottomBar(true, position);
//						td2.reverseTransition(100);
						break;
					case PAGER_ITEM_GLOBAL_SEARCH:
//						setBottomBar(true, position);
						td4.reverseTransition(100);
						break;
				}
				l3.setSelected(true);
				td3.startTransition(200);

				l1.setSelected(false);
				l2.setSelected(false);
				l4.setSelected(false);
				break;

			case PAGER_ITEM_GLOBAL_SEARCH:

				AnalyticsUtils.trackScreen(this, AnalyticsUtils.HOME_GLOBAL_SEARCH);

				switch (currentPagerItem) {
					case PAGER_ITEM_TIMELINE:
						setBottomBar(true, position);
//						td1.reverseTransition(100);
						break;
					case PAGER_ITEM_PROFILE:
						setBottomBar(true, position);
//						td2.reverseTransition(100);
						break;
					case PAGER_ITEM_CHATS:
//						setBottomBar(true, position);
						td3.reverseTransition(100);
						break;
				}
				l4.setSelected(true);
				td4.startTransition(200);

				l1.setSelected(false);
				l2.setSelected(false);
				l3.setSelected(false);
				break;
		}

		currentPagerItem = position;
	}

	@Override
	public void onPageScrollStateChanged(int state) {}

	private void handleChatToReadDot(int pageSelected) {
		if (pageSelected == PAGER_ITEM_CHATS) {
			notificationsDotChat.setVisibility(View.GONE);
		} else {
			if (mUser.isValid() && ChatRoom.Companion.areThereUnreadMessages(null, realm))
				notificationsDotChat.setVisibility(View.VISIBLE);
			else
				notificationsDotChat.setVisibility(View.GONE);
		}
	}


	//region == Getters and setters ==

	public void setBackListener(OnBackPressedListener backListener) {
		this.backListener = backListener;
	}

	public PostBottomSheetHelper getBottomSheetHelper() {
		return bottomSheetHelper;
	}

	public View getNotificationsDotChat() {
		return notificationsDotChat;
	}

	public static int getCurrentPagerItem() {
		return currentPagerItem;
	}

	//	public OnActionsResultFromBottom getPostDeletedListener() {
//		return postDeletedListener;
//	}
//	public void setPostDeletedListener(OnActionsResultFromBottom postDeletedListener) {
//		this.postDeletedListener = postDeletedListener;
//	}

	//endregion

}