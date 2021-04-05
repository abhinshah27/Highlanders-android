package rs.highlande.highlanders_app.base;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieListener;
import com.airbnb.lottie.LottieTask;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.multidex.MultiDexApplication;
import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Kit;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import rs.highlande.highlanders_app.BuildConfig;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.models.HLPosts;
import rs.highlande.highlanders_app.models.HLUser;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.LogUtils;
import rs.highlande.highlanders_app.utility.RingerChangedReceiver;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.caches.AudioVideoCache;
import rs.highlande.highlanders_app.utility.caches.PicturesCache;
import rs.highlande.highlanders_app.utility.helpers.RealTimeCommunicationHelperKotlin;
import rs.highlande.highlanders_app.utility.realm.HLMigration;
import rs.highlande.highlanders_app.utility.realm.RealmUtils;
import rs.highlande.highlanders_app.websocket_connection.HLSocketConnection;

/**
 * @author mbaldrighi on 9/1/2017.
 */
public class HLApp extends MultiDexApplication implements OnApplicationContextNeeded {

	public static final String LOG_TAG = HLApp.class.getCanonicalName();

	public static int pageIdPosts = 1;
	public static int pageIdHearts = 1;
	public static int pageIdComments = 1;
	public static int pageIdShares = 1;

	public static Boolean refreshAllowed = null;
	public static boolean fromMediaTaking = false;

	public static boolean fcmTokenSent = false;
	public static boolean subscribedToSocket = false;
	public static boolean subscribedToSocketChat = false;

	public static boolean profileFragmentVisible = false;
	public static boolean chatRoomsFragmentVisible = false;

	public static boolean canPlaySounds = false;
	public static boolean canVibrate = false;

	/**
	 * 	Attempts to regulate message visualization after reconnecting
	 */
	public static boolean justComeFromBackground = false;


	/**
	 * Field needed to reset notifications maps after identity switch.
	 */
	public static boolean identityChanged = false;

	public static RealmConfiguration realmConfig;
	public static List<Realm> openRealms = new ArrayList<>();

	public static LottieComposition siriComposition;

	public static boolean isForeground;

	private static GoogleAnalytics sAnalytics;
	private static Tracker sTracker;

	private RingerChangedReceiver ringerReceiver;


	@Override
	public void onCreate() {
		super.onCreate();

		/* SOCKET CONNECTION */
		HLSocketConnection.init();
		HLSocketConnection.getInstance().setContextListener(this);


		/* BACKGROUND MANAGER */
		BackForegroundManager.init(this);
		BackForegroundManager.getInstance().registerListener(appActivityListener);

		ringerReceiver = new RingerChangedReceiver();


		/* REALM.IO */
		Realm.init(this);
		realmConfig = new RealmConfiguration.Builder()
//				.deleteRealmIfMigrationNeeded()
				.schemaVersion(8)
				.migration(new HLMigration())
				.build();
		Realm.setDefaultConfiguration(realmConfig);
		HLPosts.getInstance().init();


		/* EMOJI SUPPORT */
		FontRequest emojiRequest = new FontRequest(
				getString(R.string.font_provider_authority),
				getString(R.string.font_provider_package),
				getString(R.string.font_query_emoji),
				R.array.com_google_android_gms_fonts_certs);
		EmojiCompat.Config config = new FontRequestEmojiCompatConfig(this, emojiRequest)
				.setReplaceAll(true)
				.registerInitCallback(new EmojiCompat.InitCallback() {
					@Override
					public void onInitialized() {
						super.onInitialized();
						LogUtils.d(LOG_TAG, "Emoji Font initialization SUCCESS");
					}

					@Override
					public void onFailed(@Nullable Throwable throwable) {
						super.onFailed(throwable);
						LogUtils.e(LOG_TAG, "Emoji Font initialization FAILED", throwable);
					}
				});
		EmojiCompat.init(config);


		/* FABRIC */
		Kit crashlytics;
		if (BuildConfig.USE_CRASHLYTICS) crashlytics = new Crashlytics();
		else {
			crashlytics = new Crashlytics.Builder()
					.core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
					.build();
		}
		Fabric.with(this, crashlytics);


		if (Utils.hasOreo())
			createDefaultNotificationChannel();


		/* 	WAVE ANIMATION */
		//here we create a new thread with message queue and start it
		initLottieAnimation(this, null);
//		final HandlerThread mHandlerThread = new HandlerThread("lottie");
//		mHandlerThread.start();
//		new Handler(mHandlerThread.getLooper()).post(new Runnable() {
//			@Override
//			public void run() {
//				LottieCompositionFactory.Factory.fromAssetFileName(HLApp.this, "siri.json",
//						new OnCompositionLoadedListener() {
//							@Override
//							public void onCompositionLoaded(@Nullable LottieComposition composition) {
//								setSiriComposition(composition);
//								mHandlerThread.quitSafely();
//							}
//						});
//			}
//		});


		/* ANALYTICS */
		sAnalytics = GoogleAnalytics.getInstance(this);


		/* REAL-TIME COMMUNICATION */
		RealTimeCommunicationHelperKotlin.Companion.getInstance(this);
	}


	public static boolean hasValidUserSession(@NonNull Realm realm) {
		return RealmUtils.hasTableObject(realm, HLUser.class);
	}


	public void closeSocketConnection() {
		getSocketConnection().closeConnection();
	}

	public boolean isSocketConnected() {
		return getSocketConnection().isConnected() || getSocketConnection().isConnectedChat();
	}

	public void reconnect() {
		getSocketConnection().openConnection(false);
		getSocketConnection().openConnection(true);
	}

	public static void resetPaginationIds() {
		refreshAllowed = true;
		pageIdPosts = 1;
		pageIdHearts = 1;
		pageIdComments = 1;
		pageIdShares = 1;
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void createDefaultNotificationChannel() {
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (mNotificationManager != null) {
			// The id of the channel.
			String id = getString(R.string.notif_channel_default);
			// The user-visible name of the channel.
			CharSequence name = getString(R.string.app_name);
			// The user-visible description of the channel.
			String description = getString(R.string.notif_channel_default_description);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;

			NotificationChannel mChannel = new NotificationChannel(id, name, importance);
			// Configure the notification channel.
			mChannel.setDescription(description);
			mChannel.enableLights(true);
			// Sets the notification light color for notifications posted to this
			// channel, if the device supports this feature.
			mChannel.setLightColor(Color.RED);
//		    mChannel.enableVibration(true);
			mChannel.setVibrationPattern(new long[]{0,
					Constants.VIBE_LONG, 200,
					Constants.VIBE_LONG, 200});
			mNotificationManager.createNotificationChannel(mChannel);
		}
	}


	synchronized public Tracker getDefaultTracker() {
		// To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
		if (sTracker == null) {
			sTracker = sAnalytics.newTracker(R.xml.global_tracker);
		}

		return sTracker;
	}


	private void registerReceivers() {
		registerReceiver(ringerReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
	}

	private void unregisterReceivers() {
		unregisterReceiver(ringerReceiver);
	}

	public static void initLottieAnimation(Context context, @Nullable LottieListener<LottieComposition> listener) {
		LottieTask<LottieComposition> task = LottieCompositionFactory.fromRawRes(context, R.raw.lottie_wave);
		task.addListener(
				listener == null ?
						(result -> siriComposition = result) :
						listener
		);
	}


	private BackForegroundManager.BackForegroundListener appActivityListener =
			new BackForegroundManager.BackForegroundListener() {

				public void onBecameForeground() {
					LogUtils.i(LOG_TAG, "Became Foreground");

					registerReceivers();

					isForeground = true;

					reconnect();
					refreshAllowed = refreshAllowed != null && !fromMediaTaking;
					fromMediaTaking = false;

					pageIdPosts = 1;

					justComeFromBackground = true;
					new Handler().postDelayed(
							() -> justComeFromBackground = false,
							400
					);

				}

				public void onBecameBackground() {
					LogUtils.i(LOG_TAG, "Became Background");

					unregisterReceivers();

					subscribedToSocket = false;
					subscribedToSocketChat = false;
					isForeground = false;

					if (isSocketConnected())
						closeSocketConnection();
					if (refreshAllowed == null)
						refreshAllowed = true;
				}
			};

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);

		// Determine which lifecycle or system event was raised.
		switch (level) {

			case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

				break;

			case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
			case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
			case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

				PicturesCache.Companion.getInstance(this).flushCache();
				AudioVideoCache.Companion.getInstance(this).flushCache();

				break;

			case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
			case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
			case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:

                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

				PicturesCache.Companion.getInstance(this).flushCache();
				AudioVideoCache.Companion.getInstance(this).flushCache();

				break;

			default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
				break;
		}
	}


	//region == Getters and setters ==

	public static HLSocketConnection getSocketConnection() { return HLSocketConnection.getInstance(); }

	@Override
	public Context getHLContext() { return this; }

	public LottieComposition getSiriComposition() {
		return siriComposition;
	}
	public void setSiriComposition(LottieComposition siriComposition) {
		HLApp.siriComposition = siriComposition;
	}

	//endregion
}
