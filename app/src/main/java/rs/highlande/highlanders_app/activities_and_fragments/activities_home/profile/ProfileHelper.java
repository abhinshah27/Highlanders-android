/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import io.realm.Realm;
import io.realm.RealmList;
import kotlin.Pair;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.activities_and_fragments.activities_chat.ChatActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_create_post.CreatePostActivityMod;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.HomeActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.settings.SettingsActivity;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.wishes.WishesAccessActivity;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.base.HLApp;
import rs.highlande.highlanders_app.models.HLNotifications;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.models.HLUserGeneric;
import rs.highlande.highlanders_app.models.Interest;
import rs.highlande.highlanders_app.models.chat.ChatRoom;
import rs.highlande.highlanders_app.models.enums.RequestsStatusEnum;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;
import rs.highlande.highlanders_app.utility.helpers.NotificationAndRequestHelper;
import rs.highlande.highlanders_app.utility.realm.RealmUtils;
import rs.highlande.highlanders_app.voiceVideoCalls.NotificationUtils;
import rs.highlande.highlanders_app.voiceVideoCalls.tmp.VoiceVideoCallType;
import rs.highlande.highlanders_app.websocket_connection.HLRequestTracker;
import rs.highlande.highlanders_app.websocket_connection.HLServerCallsChat;
import rs.highlande.highlanders_app.websocket_connection.OnMissingConnectionListener;
import rs.highlande.highlanders_app.websocket_connection.OnServerMessageReceivedListener;

/**
 * @author mbaldrighi on 12/4/2017.
 */
public class ProfileHelper implements Serializable, View.OnClickListener, OnServerMessageReceivedListener,
		OnMissingConnectionListener
		/*NotificationAndRequestHelper.OnNotificationHelpListener*//* HomeActivity.ViewProviderForNotifications*/ {

	public static final String LOG_TAG = ProfileHelper.class.getCanonicalName();

	private static final float WEIGHT_ME_FRIEND_SHOW = .3333333333333333f;
	private static final float WEIGHT_ME_FRIEND_NO = .5f;

	private ImageView mainBackground;

	private Toolbar toolbar1;
	private View notificationBtn;
	private View notificationDot;
	private TextView notificationsCount;
	private View settingsBtn;

	private Toolbar toolbar2;
	private View toolbarDots;

	private View bottomBar;
	private View l1, l2, l3, l4;
	private TransitionDrawable td1, td2, td3, td4;
	private View bottomBarNotificationDot;

	/* MAIN CARD */
	private ViewGroup usersCard;
	private ImageView profilePicture;
	private ImageView wallPicture;
	private TextView personName, personAboutMe;
	private TextView inviteBtn;
	private View heartsSection;
	private View heartsTotalLayout;
	private View heartsAvailLayout;
	private TextView heartsTotal;
	private TextView heartsAvailable;
	private ImageView wishesButton;
	private View lowerLayout, heartsNameLayout;

	/* card friends */
	private View friendHideable;
	private TextView heartsGiven;
	private TextView heartsReceived;

	/* BOTTOM SECTION */
	private View layoutButtons;
	private View buttonsMeFriend;
	private TextView meFriendDiary, icMeFriend, intMeFriend;
	private View buttonsNotFriend;

	/* INTEREST HANDLING */
	private View interestCard;
	private ImageView interestPicture;
	private ImageView interestWall;
	private TextView unFollowBtn;
	private View claimInterestStatus;
	private View preferredLabel;
	private TextView interestName;
	private TextView interestDescription;
	//	private View interestHeartsLayout;
	private TextView interestHeartsTotal;
	private TextView interestFollowers;
	private View interestButtonsSection;
	private TextView interestBtnSimilar;
	private View interestHeartsFollowersLayout;

	private View chatCallsLayout;
	private View groupChat, groupVoice, groupVideo;
	private ChatRoom chatRoom = null;

	public enum ProfileType implements Serializable {
		ME, FRIEND, NOT_FRIEND,
		INTEREST_ME, INTEREST_CLAIMED, INTEREST_NOT_CLAIMED
	}

	private ProfileType mType;

	private HLUserGeneric userGen;
	private Interest interest;

//	private NotificationAndRequestHelper notificationsHelper;

	private OnProfileInteractionsListener mListener;

	private String callsToken = null;


	public ProfileHelper(OnProfileInteractionsListener listener) {
		this.mListener = listener;

//		if (mListener instanceof HomeActivity)
//			((HomeActivity) mListener).setViewProvider(this);
//		else
//		if (mListener instanceof ProfileFragment &&
//				((ProfileFragment) mListener).getActivity() instanceof HLActivity) {
//
//			this.notificationsHelper = new NotificationAndRequestHelper(
//					(HLActivity) ((ProfileFragment) mListener).getActivity(),
//					this
//			);
//		}

	}

	public void configureLayout(View view) {
		if (view != null) {
			mainBackground = view.findViewById(R.id.profile_background);

			// TOOLBAR #1
			toolbar1 = view.findViewById(R.id.toolbar_1);
			notificationBtn = toolbar1.findViewById(R.id.notification_btn_layout);
			notificationBtn.setOnClickListener(this);
			notificationDot = toolbar1.findViewById(R.id.notification_dot);
			notificationsCount = toolbar1.findViewById(R.id.notifications_count);
			settingsBtn = toolbar1.findViewById(R.id.settings_btn);
			settingsBtn.setOnClickListener(this);

			// TOOLBAR #2
			toolbar2 = view.findViewById(R.id.toolbar_2);
			toolbar2.findViewById(R.id.back_arrow).setOnClickListener(this);
			toolbarDots = toolbar2.findViewById(R.id.dots);
			toolbarDots.setOnClickListener(this);

			// BOTTOM BAR
			bottomBar = view.findViewById(R.id.bottom_bar);
			configureBottomBar(bottomBar);

			usersCard = view.findViewById(R.id.card_profile);
			configureUsersCard(usersCard);
			interestCard = view.findViewById(R.id.card_interest);
			configureInterestCard(interestCard);

			layoutButtons = view.findViewById(R.id.layout_options);

			// for USERS
			buttonsMeFriend = view.findViewById(R.id.screen_selection_me_friend);
			meFriendDiary = view.findViewById(R.id.me_friend_diary);
			buttonsNotFriend = view.findViewById(R.id.screen_selection_not_friend);

			meFriendDiary.setOnClickListener(this);
			View notFriendDiary = view.findViewById(R.id.not_friend_diary);
			notFriendDiary.setOnClickListener(this);

			icMeFriend = view.findViewById(R.id.me_friend_inner_C);
			icMeFriend.setOnClickListener(this);
			View icNotFriend = view.findViewById(R.id.not_friend_inner_c);
			icNotFriend.setOnClickListener(this);

			intMeFriend = view.findViewById(R.id.me_friend_inner_interests);
			intMeFriend.setOnClickListener(this);


			// for INTERESTS
			interestButtonsSection = view.findViewById(R.id.screen_selection_interest);
			View interestBtnDiary = view.findViewById(R.id.interest_diary);
			interestBtnDiary.setOnClickListener(this);
			View interestBtnFollowers = view.findViewById(R.id.interest_followers);
			interestBtnFollowers.setOnClickListener(this);

			interestBtnSimilar = view.findViewById(R.id.interest_initiatives_similar);
			interestBtnSimilar.setOnClickListener(this);
		}
	}

	private void configureUsersCard(final ViewGroup view) {
		profilePicture = view.findViewById(R.id.profile_picture);
		profilePicture.setOnClickListener(this);
		profilePicture.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Context context = profilePicture.getContext();
				if (context instanceof Activity &&
						(mType == ProfileType.ME || mType == ProfileType.INTEREST_ME)) {
					Utils.openIdentitySelection((Activity) context, null);
					return true;
				}
				return false;
			}
		});

		wallPicture = view.findViewById(R.id.wall_image);
		personName = view.findViewById(R.id.name);
		personName.setOnClickListener(this);
		personAboutMe = view.findViewById(R.id.about_me);
		inviteBtn = view.findViewById(R.id.invite_to_circle);
		inviteBtn.setOnClickListener(this);
		heartsSection = view.findViewById(R.id.hearts_section);
		heartsTotalLayout = view.findViewById(R.id.hearts_total_layout);
		heartsAvailLayout = view.findViewById(R.id.hearts_avail_layout);
		heartsTotal = view.findViewById(R.id.count_heart_total);
		heartsAvailable = view.findViewById(R.id.count_heart_available);
		friendHideable = view.findViewById(R.id.layout_hideable_friend);
		heartsGiven = view.findViewById(R.id.count_heart_given);
		heartsReceived = view.findViewById(R.id.count_heart_received);

		wishesButton = new ImageView(view.getContext());
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				Utils.dpToPx(50f, usersCard.getResources()),
				Utils.dpToPx(50f, usersCard.getResources()),
				Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
		);
		wishesButton.setLayoutParams(lp);
		wishesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Context context = wishesButton.getContext();
				if (Utils.isContextValid(context)) {
					context.startActivity(new Intent(context, WishesAccessActivity.class));

					if (context instanceof Activity)
						((Activity) context).overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
				}

			}
		});
		usersCard.addView(wishesButton);

		chatCallsLayout = LayoutInflater.from(view.getContext()).inflate(R.layout.layout_user_profile_chat_calls_fab, view, false);
		FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) chatCallsLayout.getLayoutParams();
		lp1.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		if (chatCallsLayout != null) {
			chatCallsLayout.setLayoutParams(lp1);
			View btnChat = chatCallsLayout.findViewById(R.id.btnTextChat);
			btnChat.setOnClickListener(v -> {
				if (userGen != null) {
					// friend != null
					if (userGen.hasValidChatRoom()) {

						Pair<Boolean, ChatRoom> pair = ChatRoom.checkRoom(userGen.getChatRoomID(), ((HLActivity) mListener.getProfileFragment().getActivity()).getRealm());
						if (pair.getFirst()) {
							// the room exists and we are both in: just open
							ChatActivity.openChatMessageFragment(
									chatCallsLayout.getContext(),
									userGen.getChatRoomID(),
									userGen.getCompleteName(),
									userGen.getAvatarURL(),
									false
							);
						}
						else {
							// likely I deleted the room. it has to be re-initialized, already has ID
							initializeNewRoom(false);
						}
					}
					else {
						// need to initialize new ChatRoom instance on server, no ID to pass
						initializeNewRoom(true);
					}
				}
			});
			View btnVoice = chatCallsLayout.findViewById(R.id.btnCallVoice);
			btnVoice.setOnClickListener(v -> goToCallActivity(VoiceVideoCallType.VOICE));
			View btnVideo = chatCallsLayout.findViewById(R.id.btnCallVideo);
			btnVideo.setOnClickListener(v -> goToCallActivity(VoiceVideoCallType.VIDEO));

			groupChat = chatCallsLayout.findViewById(R.id.groupChat);
			groupVoice = chatCallsLayout.findViewById(R.id.groupVoice);
			groupVideo = chatCallsLayout.findViewById(R.id.groupVideo);

			usersCard.addView(chatCallsLayout);
		}

		lowerLayout = this.usersCard.findViewById(R.id.lower_section);
		lowerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new CardGlobalLayoutObserver(lowerLayout, wishesButton, chatCallsLayout, usersCard));

		heartsNameLayout = view.findViewById(R.id.name_hearts_layout);

		view.setOnClickListener(this);
	}

	private void configureInterestCard(View view) {
		interestPicture = view.findViewById(R.id.interest_profile_picture);
		interestPicture.setOnClickListener(this);
		interestPicture.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Context context = interestPicture.getContext();
				if (context instanceof Activity &&
						(mType == ProfileType.ME || mType == ProfileType.INTEREST_ME)) {
					Utils.openIdentitySelection((Activity) context, null);
					return true;
				}
				return false;
			}
		});

		interestWall = view.findViewById(R.id.wall_image);
		unFollowBtn = view.findViewById(R.id.follow_unfollow_btn);
		unFollowBtn.setOnClickListener(this);
		claimInterestStatus = view.findViewById(R.id.claim_status);

		preferredLabel = view.findViewById(R.id.preferred_icon);
		interestName = view.findViewById(R.id.interest_name);
		interestName.setOnClickListener(this);
		interestDescription = view.findViewById(R.id.interest_headline);
//		interestHeartsLayout = view.findViewById(R.id.hearts_total_layout);
		interestHeartsTotal = view.findViewById(R.id.count_heart_total);
		interestFollowers = view.findViewById(R.id.count_followers);

		interestHeartsFollowersLayout = view.findViewById(R.id.hearts_followers_section);

		view.setOnClickListener(this);
	}

	private void configureBottomBar(final View bar) {
		if (bar != null) {
			l1 = bar.findViewById(R.id.bottom_timeline);
			l1.setOnClickListener(this);
			l2 = bar.findViewById(R.id.bottom_profile);
			l2.setOnClickListener(this);
			l2.setSelected(true);
			l3 = bar.findViewById(R.id.bottom_chats);
			l3.setOnClickListener(this);
			l4 = bar.findViewById(R.id.bottom_global_search);
			l4.setOnClickListener(this);

			ImageView ib1 = bar.findViewById(R.id.icon_timeline);
			td1 = (TransitionDrawable) ib1.getDrawable();
			td1.setCrossFadeEnabled(true);
			ImageView ib2 = bar.findViewById(R.id.icon_profile);
			td2 = (TransitionDrawable) ib2.getDrawable();
			td2.setCrossFadeEnabled(true);
			td2.startTransition(0);
			ImageView ib3 = bar.findViewById(R.id.icon_chats);
			td3 = (TransitionDrawable) ib3.getDrawable();
			td3.setCrossFadeEnabled(true);
			ImageView ib4 = bar.findViewById(R.id.icon_global_search);
			td4 = (TransitionDrawable) ib4.getDrawable();
			td4.setCrossFadeEnabled(true);

			ImageView main = bar.findViewById(R.id.main_action_btn);
			main.setOnClickListener(this);

			bottomBarNotificationDot = bar.findViewById(R.id.notification_dot);
		}
	}

	private void setBottomBar(int currentSelItem) {
		if (currentSelItem > -1) {
			switch (currentSelItem) {
				case HomeActivity.PAGER_ITEM_GLOBAL_SEARCH:
					l1.setSelected(false);
					l2.setSelected(false);
					l3.setSelected(false);
					l4.setSelected(true);
					td4.startTransition(0);
					td1.resetTransition();
					td2.resetTransition();
					td3.resetTransition();
					break;
				case HomeActivity.PAGER_ITEM_TIMELINE:
					l1.setSelected(true);
					l2.setSelected(false);
					l3.setSelected(false);
					l4.setSelected(false);
					td1.startTransition(0);
					td4.resetTransition();
					td2.resetTransition();
					td3.resetTransition();
					break;
				case HomeActivity.PAGER_ITEM_PROFILE:
					l1.setSelected(false);
					l2.setSelected(true);
					l3.setSelected(false);
					l4.setSelected(false);
					td2.startTransition(0);
					td4.resetTransition();
					td1.resetTransition();
					td3.resetTransition();
					break;
				case HomeActivity.PAGER_ITEM_CHATS:
					l1.setSelected(false);
					l2.setSelected(false);
					l3.setSelected(true);
					l4.setSelected(false);
					td3.startTransition(0);
					td4.resetTransition();
					td2.resetTransition();
					td1.resetTransition();
					break;
			}
		}
		else {
			l1.setSelected(false);
			l2.setSelected(false);
			l3.setSelected(false);
			l4.setSelected(false);
			td3.resetTransition();
			td4.resetTransition();
			td2.resetTransition();
			td1.resetTransition();
		}
	}

	public void setLayout(@NonNull ProfileType type, @Nullable HLUserGeneric userGen,
	                      @Nullable Interest interest, int bottomBarSelItem) {
		mType = type;

		boolean condition = HLNotifications.getInstance().getUnreadCount(true) > 0 && mListener.getUser().isValid();
		notificationDot.setVisibility(condition ? View.VISIBLE : View.GONE);

		toolbar1.setVisibility((type == ProfileType.ME || type == ProfileType.INTEREST_ME) ? View.VISIBLE : View.GONE);
		toolbar2.setVisibility((type == ProfileType.ME || type == ProfileType.INTEREST_ME) ? View.GONE : View.VISIBLE);
		toolbarDots.setVisibility(mListener.getUser().isActingAsInterest() ? View.GONE : View.VISIBLE);

		int margin = bottomBar.getContext().getResources().getDimensionPixelSize(R.dimen.bottom_bar_height);
		if (type == ProfileType.ME || type == ProfileType.INTEREST_ME) {
			bottomBar.setVisibility(View.GONE);

			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) layoutButtons.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			lp.setMargins(0, 0, 0, margin);
			layoutButtons.setLayoutParams(lp);
		}
		else {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) layoutButtons.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			layoutButtons.setLayoutParams(lp);
		}

		switch (type) {
			case ME:
			case FRIEND:
			case NOT_FRIEND:
				buttonsNotFriend.setVisibility(View.VISIBLE);
				buttonsMeFriend.setVisibility(View.GONE);
				interestButtonsSection.setVisibility(View.GONE);
				usersCard.setVisibility(View.VISIBLE);
				interestCard.setVisibility(View.GONE);
				setLayoutForUsers(type, userGen);
				break;

			case INTEREST_ME:
			case INTEREST_CLAIMED:
			case INTEREST_NOT_CLAIMED:
				buttonsNotFriend.setVisibility(View.GONE);
				buttonsMeFriend.setVisibility(View.GONE);
				interestButtonsSection.setVisibility(View.VISIBLE);
				usersCard.setVisibility(View.GONE);
				interestCard.setVisibility(View.VISIBLE);
				if (interest != null)
					setLayoutForInterests(type, interest);
				break;
		}
	}

	private void setLayoutForUsers(@NonNull ProfileType type, @Nullable HLUserGeneric userCard) {
		String name = userCard != null ? userCard.getName() : "";
		String aboutMe = userCard != null ? userCard.getAboutDescription() : "";

		if (type == ProfileType.ME) {
			HLUser user = mListener.getUser();

			if (user.getAvatar() != null) {
				Drawable d = new BitmapDrawable(profilePicture.getResources(), user.getAvatar());
				profilePicture.setImageDrawable(d);
			} else {
				if (Utils.isStringValid(user.getAvatarURL()))
					loadPictureWithConversion(user.getAvatarURL(), profilePicture, PictureType.PROFILE);
			}

			if (user.getWall() != null) {
				Drawable d = new BitmapDrawable(mainBackground.getResources(), user.getWall());
				wallPicture.setImageDrawable(d);
				mainBackground.setImageDrawable(d);
			}
			else if (Utils.isStringValid(user.getCoverPhotoURL())) {
				loadPictureWithConversion(user.getCoverPhotoURL(), wallPicture, PictureType.WALL);
			}

			heartsTotal.setText(Utils.getReadableCount(heartsTotal.getResources(), user.getTotHearts()));
			heartsAvailLayout.setVisibility(View.VISIBLE);
			heartsAvailable.setText(Utils.getReadableCount(heartsAvailable.getResources(), user.getTotHeartsAvailable()));

			inviteBtn.setVisibility(View.GONE);

			friendHideable.setVisibility(View.GONE);

			buttonsMeFriend.setVisibility(View.VISIBLE);
			buttonsNotFriend.setVisibility(View.GONE);

			meFriendDiary.setText(R.string.profile_diary_me);

			name = user.getCompleteName();
			aboutMe = user.getAboutDescription();

			if (wishesButton != null) {
				wishesButton.setImageResource(R.drawable.ic_wishes_button);
				wishesButton.setVisibility(View.VISIBLE);
			}
			if (chatCallsLayout != null)
				chatCallsLayout.setVisibility(View.GONE);
		}
		else if (userCard != null) {
			this.userGen = userCard;
			String avatar = userCard.getAvatarURL();
			String wall = userCard.getWallImageLink();
			MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
					avatar, profilePicture);

			loadPictureWithConversion(wall, wallPicture, PictureType.WALL);

			type = userCard.isFriend() ? ProfileType.FRIEND : ProfileType.NOT_FRIEND;

			inviteBtn.setVisibility(
					(
							!HLApp.getSocketConnection().isConnected() || type == ProfileType.FRIEND ||
									!userCard.canBeInvitedToIC() ||
									mListener.getUser().isActingAsInterest()
					) ? View.GONE : View.VISIBLE
			);

			LinearLayout.LayoutParams heartsLp = (LinearLayout.LayoutParams) heartsNameLayout.getLayoutParams();
			heartsLp.gravity = Gravity.CENTER_VERTICAL;
			heartsNameLayout.setLayoutParams(heartsLp);


			friendHideable.setVisibility(type == ProfileType.FRIEND ? View.VISIBLE : View.GONE);
			buttonsMeFriend.setVisibility(type == ProfileType.FRIEND ? View.VISIBLE : View.GONE);
			buttonsNotFriend.setVisibility(type == ProfileType.FRIEND ? View.GONE : View.VISIBLE);
			heartsSection.setVisibility(type == ProfileType.FRIEND ? View.VISIBLE : View.GONE);
			heartsTotalLayout.setVisibility(type == ProfileType.FRIEND ? View.VISIBLE : View.GONE);
			heartsAvailLayout.setVisibility(View.GONE);

			if (type == ProfileType.FRIEND) {
				heartsTotal.setText(Utils.getReadableCount(heartsTotal.getResources(), userCard.getTotHearts()));
				heartsGiven.setText(
						heartsGiven.getContext().getString(
								R.string.profile_hearts_given,
								Utils.getReadableCount(heartsGiven.getResources(), userCard.getHeartsGiven())
						)
				);
				heartsReceived.setText(
						heartsReceived.getContext().getString(
								R.string.profile_hearts_received,
								Utils.getReadableCount(heartsGiven.getResources(), userCard.getHeartsReceived())
						)
				);
				heartsAvailable.setVisibility(View.GONE);

				float weightMeFriend = userCard.isShowInnerCircle() ? WEIGHT_ME_FRIEND_SHOW : WEIGHT_ME_FRIEND_NO;
				icMeFriend.setVisibility(userCard.isShowInnerCircle() ? View.VISIBLE : View.GONE);

				LinearLayout.LayoutParams lp6 = (LinearLayout.LayoutParams) meFriendDiary.getLayoutParams();
				lp6.weight = weightMeFriend;
				meFriendDiary.setLayoutParams(lp6);
				meFriendDiary.setText(R.string.profile_diary_friend);

				LinearLayout.LayoutParams lp7 = (LinearLayout.LayoutParams) intMeFriend.getLayoutParams();
				lp7.weight = weightMeFriend;
				intMeFriend.setLayoutParams(lp7);

				if (chatCallsLayout != null) {
					groupChat.setVisibility(userGen.canChat() ? View.VISIBLE : View.GONE);
					groupVoice.setVisibility(userGen.canAudiocall() ? View.VISIBLE : View.GONE);
					groupVideo.setVisibility(
							Utils.hasDeviceCamera(chatCallsLayout.getContext()) && userGen.canVideocall() ?
									View.VISIBLE : View.GONE
					);
					chatCallsLayout.setVisibility(View.VISIBLE);
				}
			}
			else chatCallsLayout.setVisibility(View.GONE);

			inviteBtn.setText(userCard.getRightStringForStatus());
			inviteBtn.setEnabled(userCard.getRequestsStatus() != RequestsStatusEnum.PENDING);

			if (wishesButton != null)
				wishesButton.setVisibility(View.GONE);
		}

		personName.setText(name);

//		lowerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new CardGlobalLayoutObserver(wishesButton, usersCard));
		if (!Utils.isStringValid(aboutMe) || aboutMe.equalsIgnoreCase(Constants.NOT_DEFINED_YET)) {
			personAboutMe.setVisibility(View.GONE);
		}
		else {
			personAboutMe.setText(aboutMe);
			personAboutMe.setVisibility(View.VISIBLE);
		}

		heartsNameLayout.setPadding(
				0,
				Utils.dpToPx(/*type == ProfileType.ME ? 30f : 10f*/ 30f, heartsNameLayout.getResources()),
				0,
				0
		);
	}

	private void setLayoutForInterests(@NonNull ProfileType type, @NonNull Interest interest) {
		this.interest = interest;
		boolean isHighlanders = Utils.isStringValid(interest.getId()) && interest.getId().equals(Constants.ID_INTEREST_HIGHLANDERS);

		if (Utils.isStringValid(interest.getAvatarURL()))
			loadPictureWithConversion(interest.getAvatarURL(), interestPicture, PictureType.PROFILE);

		if (Utils.isStringValid(interest.getWallPictureURL()))
			loadPictureWithConversion(interest.getWallPictureURL(), interestWall, PictureType.WALL);

		interestName.setText(interest.getName());
		interestDescription.setText(interest.getHeadline());
		interestDescription.setVisibility(Utils.isStringValid(interest.getHeadline()) ? View.VISIBLE : View.GONE);
		interestFollowers.setText(interest.getFollowersWithNumber(interestFollowers.getResources()));
		interestHeartsTotal.setText(interest.getHeartsWithNumber(interestHeartsTotal.getResources()));

		toolbarDots.setVisibility(interest.isFollowed() ? View.VISIBLE : View.GONE);
		unFollowBtn.setText(interest.isFollowed() ? R.string.action_unfollow : R.string.action_follow);

		preferredLabel.setVisibility((interest.isPreferred() && !mListener.getUser().isActingAsInterest()) ?
				View.VISIBLE : View.INVISIBLE);

		if (type == ProfileType.INTEREST_ME) {
			// TODO: 1/15/2018    fill in IF NECESSARY
			unFollowBtn.setVisibility(View.GONE);
			preferredLabel.setVisibility(View.INVISIBLE);
		}
		else if (type == ProfileType.INTEREST_CLAIMED) {
			unFollowBtn.setVisibility(mListener.getUser().isActingAsInterest() ? View.GONE : View.VISIBLE);
		}

		interestHeartsFollowersLayout.setVisibility(isHighlanders ? View.GONE : View.VISIBLE);

		claimInterestStatus.setVisibility(interest.isClaimedByYou() && interest.isClaimedPending() ? View.VISIBLE : View.GONE);
	}

	void showAuthorizationRequestResult(boolean success) {
		if (inviteBtn != null) {
            userGen.setRequestsStatus(success ? RequestsStatusEnum.PENDING : RequestsStatusEnum.NOT_AVAILABLE);
            inviteBtn.setText(userGen.getRightStringForStatus());
            inviteBtn.setEnabled(userGen.getRequestsStatus() != RequestsStatusEnum.PENDING);
            inviteBtn.setVisibility(View.VISIBLE);
		}
	}

	void handleMissingConnectionForInviteButton() {
        userGen.setRequestsStatus(RequestsStatusEnum.NOT_AVAILABLE);
        inviteBtn.setVisibility(View.GONE);
	}


	private void loadPictureWithConversion(final String pictureUrl, final ImageView view, final PictureType type) {
		if (Utils.isStringValid(pictureUrl) && view != null) {
			Glide.with(view).asBitmap().load(pictureUrl).into(new SimpleTarget<Bitmap>() {

				@Override
				public void onLoadFailed(@Nullable Drawable errorDrawable) {
					super.onLoadFailed(errorDrawable);
				}

				@Override
				public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
					Drawable d = new BitmapDrawable(view.getResources(), resource);

					if (Utils.isContextValid(view.getContext())) {
						if (Utils.hasLollipop())
							view.setImageDrawable(d);
						else
							MediaHelper.roundPictureCorners(view, d, 8f, false);
					}
					if (type == PictureType.WALL && mainBackground != null)
						MediaHelper.blurWallPicture(mainBackground.getContext(), d, mainBackground);
//  					mainBackground.setImageDrawable(d);


					/*
					Realm realm = RealmUtils.getCheckedRealm();
					try {
						HLUser user = new HLUser().readUser(realm);
						if (user != null) {
							if ((type == PictureType.PROFILE && user.getAvatarBase64() == null) ||
									(type == PictureType.WALL && user.getWallBase64() == null))
								new ConvertPictureToBase64(type).execute(resource);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (RealmUtils.isValid(realm))
							realm.close();
					}
					*/

				}
			});
		}
	}

	private void callServer() {
		Object[] result = null;
		try {
			roomInitializationCalled = true;
			result = HLServerCallsChat.initializeNewRoom(chatRoom);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((HLApp) mListener.getProfileFragment().getActivity().getApplication()))
				.handleCallResult(this, (HLActivity) mListener.getProfileFragment().getActivity(), result, true, false);
	}


	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.back_arrow) {
			// only accessible button in GUEST MODE
			mListener.onBackClick();
		}
		else if (view.getContext() instanceof Activity &&
				!Utils.checkAndOpenLogin(((Activity) view.getContext()), mListener.getUser())) {

			switch (view.getId()) {
				// toolbar #1
				case R.id.notification_btn_layout:
					mListener.onNotificationClick();
					break;
				case R.id.settings_btn:
					SettingsActivity.openSettingsMainFragment(view.getContext());
					break;

				// toolbar #2
//				case R.id.back_arrow:
//					mListener.onBackClick();
//					break;
				case R.id.dots:
					if (mType == ProfileType.INTEREST_NOT_CLAIMED || mType == ProfileType.INTEREST_CLAIMED)
						mListener.onDotsClick(view, preferredLabel);
					else if (mType == ProfileType.FRIEND || mType == ProfileType.NOT_FRIEND)
						mListener.onDotsClickUser(view);
					break;

				// users card
				case R.id.card_profile:
				case R.id.profile_picture:
					if (mType != ProfileType.NOT_FRIEND)
						mListener.goToMyUserDetails();
					break;
				case R.id.invite_to_circle:
					// disables button as soon as it's tapped to prevent other requests
					showAuthorizationRequestResult(true);
					mListener.inviteToInnerCircle();
					break;
				case R.id.me_friend_diary:
				case R.id.not_friend_diary:
					mListener.goToDiary();
					break;
				case R.id.me_friend_inner_C:
				case R.id.not_friend_inner_c:
					mListener.goToInnerCircle();
					break;
				case R.id.me_friend_inner_interests:
					mListener.goToInnerInterests();
					break;

				// interest card
				case R.id.card_interest:
				case R.id.interest_profile_picture:
					mListener.goToInterestDetails();
					break;
				case R.id.follow_unfollow_btn:
					if (interest != null && interest.isFollowed())
						mListener.unfollowInterest();
					else
						mListener.followInterest();
					break;
				case R.id.claim_button:
					mListener.goToClaimPage();
					break;

				case R.id.interest_diary:
					mListener.goToDiaryForInterest();
					break;
				case R.id.interest_followers:
					mListener.goToFollowers();
					break;
				case R.id.interest_initiatives_similar:
					// only SIMILAR now exist
//					if (interest != null && interest.isClaimed())
//						mListener.goToInitiatives();
//					else
					mListener.goToSimilar();
					break;

				case R.id.main_action_btn:
				case R.id.bottom_global_search:
				case R.id.bottom_profile:
				case R.id.bottom_chats:
				case R.id.bottom_settings:
					onBottomBarClick(view);
					break;
			}
		}
	}


	private void onBottomBarClick(View view) {
		Context context = view.getContext();
		Intent intent = new Intent(context, HomeActivity.class);
		int page = 0;
		switch(view.getId()) {
			case R.id.main_action_btn:
				if (context instanceof Activity) {
					Intent createPostIntent = new Intent(context, CreatePostActivityMod.class);
					((Activity) context).startActivityForResult(createPostIntent, Constants.RESULT_CREATE_POST);
				}
				return;

			case R.id.bottom_global_search:
				page = HomeActivity.PAGER_ITEM_GLOBAL_SEARCH;
				break;
			case R.id.bottom_profile:
				page = HomeActivity.PAGER_ITEM_PROFILE;
				break;
			case R.id.bottom_chats:
				page = HomeActivity.PAGER_ITEM_CHATS;
				break;

			// TODO: 4/22/2018    understand what to do with bottom bat TIMELINE cta

//			case R.id.bottom_settings:
//				page = HomeActivity.PAGER_ITEM_SETTINGS;
//				break;
		}

		intent.putExtra(Constants.EXTRA_PARAM_1, page);

		context.startActivity(intent);
		if (context instanceof Activity)
			((Activity) context).finish();
	}

	// TODO: 4/23/2018     TEMPORARILY DISABLED
	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {

		if (operationId == Constants.SERVER_OP_GET_NOTIFICATION_COUNT) {
			NotificationAndRequestHelper.handleDotVisibility(bottomBarNotificationDot, mListener.getUser().isValid());
			NotificationAndRequestHelper.handleDotVisibility(bottomBarNotificationDot, mListener.getUser().isValid());
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {}

	@Override
	public void onMissingConnection(int operationId) {

	}

	void handleRoomInitialization(JSONArray responseObject) {
		JSONObject j = responseObject.optJSONObject(0);
		if (j != null) {
			chatRoom = ChatRoom.getRoom(j);
			chatRoom.setOwnerID(mListener.getUser().getUserId());
		}

		((HLActivity) mListener.getProfileFragment().getActivity()).getRealm().executeTransaction(realm -> realm.insertOrUpdate(chatRoom));

		if (chatRoom.isValid()) {
			ChatActivity.openChatMessageFragment(
					chatCallsLayout.getContext(),
					Objects.requireNonNull(chatRoom.getChatRoomID()),
					userGen.getCompleteName(),
					userGen.getAvatarURL(),
					false
			);
		}
	}

	private boolean roomInitializationCalled = false;
	private void initializeNewRoom(boolean noID) {
		RealmList<String> ids = new RealmList<>();
		ids.add(userGen.getId());
		chatRoom = new ChatRoom(
				mListener.getUser().getUserId(),
				ids,
				noID ? null : userGen.getChatRoomID()
		);

		callServer();
	}

	void setCallsToken(String callsToken) {
		this.callsToken = callsToken;
	}

	private void goToCallActivity(VoiceVideoCallType callType) {
		if (mListener.getProfileFragment().getActivity() instanceof HLActivity) {
			NotificationUtils.sendCallNotificationAndGoToActivity(
					((HLActivity) mListener.getProfileFragment().getActivity()),
					mListener.getUser().getUserId(),
					mListener.getUser().getUserCompleteName(),
					userGen,
					callType
			);
		}

	}


	//region == Getters and setters ==

	public boolean isRoomInitializationCalled() {
		return roomInitializationCalled;
	}

	public void setRoomInitializationCalled(boolean roomInitializationCalled) {
		this.roomInitializationCalled = roomInitializationCalled;
	}

	//endregion


	//region - Interface to Home for notifications -

//	@Override
//	public String getUserId() {
//		return null;
//	}

	public View getBottomBarNotificationDot() {
		return bottomBarNotificationDot;
	}

	//endregion



	public interface OnProfileInteractionsListener {
		@NonNull HLUser getUser();
		void goToMyUserDetails();
		void goToDiary();
		void goToInnerCircle();
		void goToInnerInterests();
		void inviteToInnerCircle();
		void onNotificationClick();
		void onBackClick();
		void onDotsClick(View dots, View preferredLabel);
		void onDotsClickUser(View dots);

		ProfileFragment getProfileFragment();

		void goToInterestDetails();
		void goToInitiatives();
		void goToDiaryForInterest();
		void goToSimilar();
		void goToClaimPage();
		void goToFollowers();
		void followInterest();
		void unfollowInterest();
	}


	private enum PictureType { PROFILE, WALL }
	private static class ConvertPictureToBase64 extends AsyncTask<Bitmap, Void, Void> {

		private PictureType type;

		ConvertPictureToBase64(PictureType type) {
			this.type = type;
		}

		@Override
		protected Void doInBackground(final Bitmap... bitmaps) {
			if (bitmaps[0] != null) {
				final String encoded = Utils.encodeBitmapToBase64(bitmaps[0]);

				if (Utils.isStringValid(encoded)) {
					Realm realm = RealmUtils.getCheckedRealm();
					if (RealmUtils.isValid(realm)) {
						realm.executeTransaction(new Realm.Transaction() {
							@Override
							public void execute(@NonNull Realm realm) {
								HLUser user = new HLUser().readUser(realm);
								if (type == PictureType.PROFILE) {
									user.setAvatarBase64(encoded);
									user.setAvatar(bitmaps[0]);
								}
								else if (type == PictureType.WALL) {
									user.setWallBase64(encoded);
									user.setWall(bitmaps[0]);
								}
							}
						});
					}
				}
			}

			return null;
		}
	}


	private class CardGlobalLayoutObserver implements ViewTreeObserver.OnGlobalLayoutListener {

		private WeakReference<View> lowerLayout;
		private WeakReference<ImageView> wishesButton;
		private WeakReference<View> chatCallsLayout;
		private WeakReference<ViewGroup> usersCard;

		public CardGlobalLayoutObserver(View lowerLayout, ImageView wishesButton, View chatCallsLayout, ViewGroup usersCard) {
			this.lowerLayout = new WeakReference<>(lowerLayout);
			this.wishesButton = new WeakReference<>(wishesButton);
			this.chatCallsLayout = new WeakReference<>(chatCallsLayout);
			this.usersCard = new WeakReference<>(usersCard);
		}

		@Override
		public void onGlobalLayout() {

			int height = 0;
			if (lowerLayout != null && lowerLayout.get() != null)
				height = lowerLayout.get().getHeight();

//			LogUtils.d(LOG_TAG, "Hearts/Names layout height: " + height);

			if (wishesButton != null && wishesButton.get() != null) {
				if (height > 0) {
					FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) wishesButton.get().getLayoutParams();
					lp.bottomMargin = height - Utils.dpToPx(28f, usersCard.get().getResources());
					wishesButton.get().setLayoutParams(lp);
					wishesButton.get().requestLayout();

				}
				else wishesButton.get().setVisibility(View.GONE);
			}

			if (chatCallsLayout != null && chatCallsLayout.get() != null) {
				if (height > 0) {
					FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) chatCallsLayout.get().getLayoutParams();
					lp.bottomMargin = height - Utils.dpToPx(18f, usersCard.get().getResources());
					chatCallsLayout.get().setLayoutParams(lp);
					chatCallsLayout.get().requestLayout();
				}
				else chatCallsLayout.get().setVisibility(View.GONE);
			}
		}
	}

}
