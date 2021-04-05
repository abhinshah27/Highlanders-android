/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.jetbrains.annotations.NotNull;

import androidx.constraintlayout.widget.ConstraintLayout;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.LogUtils;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.caches.AudioVideoCache;
import rs.highlande.highlanders_app.utility.helpers.HLMediaType;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;
import rs.highlande.highlanders_app.utility.helpers.ShareHelper;

/**
 * @author mbaldrighi on 4/26/2018.
 */
public class VideoViewActivityExo extends HLActivity
		implements AudioManager.OnAudioFocusChangeListener, Player.EventListener,
		ShareHelper.ShareableProvider {

	public static final String LOG_TAG = VideoViewActivityExo.class.getCanonicalName();

	private String urlToLoad, thumbnail, transitionName;
	private Uri uriToLoad;
	private String postOrMessageId;
	private boolean fromChat;

	private AudioManager audioManager;
	private AudioFocusRequest audioRequest;

	private ImageView mThumbnailView;
	private View progressView, mThumbnailLayout;
	private TextView progressMessage;

	private SimpleExoPlayer exoPlayer;
	private boolean exoIsPlaying;

	private long lastPosition;
	private boolean wantsLandscape;

	final Object mFocusLock = new Object();

	private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	private BecomingNoisyReceiver noisyAudioStreamReceiver = new BecomingNoisyReceiver();

	private View closeBtn;
	private int safeCutout = 0;

	private ShareHelper mShareHelper;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_view_exo_constr);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.progress);

		View decorView = getWindow().getDecorView();
		// Hide both the navigation bar and the status bar.
		// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);
        setImmersiveValue(true);

		if (Utils.hasPie()) {
			WindowManager.LayoutParams params = getWindow().getAttributes();
			params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

			findViewById(R.id.root_content).setOnApplyWindowInsetsListener((v, insets) -> {
				if (insets.getDisplayCutout() == null)
					params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;

				this.safeCutout = insets.getSystemWindowInsetTop();
				manageCutout();

				return insets.consumeDisplayCutout();
			});
		}

		manageIntent();

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		if (audioManager != null) {
			if (Utils.hasOreo()) {
				AudioAttributes mPlaybackAttributes = new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
						.build();
				audioRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
						.setAudioAttributes(mPlaybackAttributes)
						.setAcceptsDelayedFocusGain(true)
						.setOnAudioFocusChangeListener(this, new Handler())
						.build();
			}
		}

		mThumbnailView = findViewById(R.id.video_view_thumbnail);
		MediaHelper.loadPictureWithGlide(this, thumbnail, mThumbnailView);
		mThumbnailLayout = findViewById(R.id.video_view_thumbnail_layout);
		mThumbnailLayout.setVisibility(lastPosition > 0 ? View.GONE : View.VISIBLE);

		progressView = findViewById(R.id.progress_layout);
		progressMessage = findViewById(R.id.progress_message);

		exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector());

		PlayerView videoView = findViewById(R.id.video_view);
		if (Utils.hasLollipop())
			videoView.setTransitionName(transitionName);

		if (wantsLandscape) videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
		videoView.setPlayer(exoPlayer);

		playVideo();

		closeBtn = findViewById(R.id.close_btn);
		closeBtn.setOnClickListener(v -> onBackPressed());
		findViewById(R.id.share_btn).setOnClickListener(v -> mShareHelper.initOps(fromChat));

		mShareHelper = new ShareHelper(this, this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.FEED_VIDEO_VIEWER);

		mShareHelper.onResume();

		manageCutout();
	}

	@Override
	protected void onPause() {
		if (!Utils.hasNougat()) release();

		super.onPause();
	}

	@Override
	protected void onStop() {
		if (Utils.hasNougat()) release();

		if (Utils.hasOreo())
			audioManager.abandonAudioFocusRequest(audioRequest);
		else
			audioManager.abandonAudioFocus(this);

		try {
			unregisterReceiver(noisyAudioStreamReceiver);
//			LocalBroadcastManager.getInstance(this).unregisterReceiver(noisyAudioStreamReceiver);
		} catch (IllegalArgumentException e) {
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}

		mShareHelper.onStop();

		super.onStop();
	}

	@Override
	public void onBackPressed() {
		Intent in = new Intent();
		in.putExtra(Constants.EXTRA_PARAM_1, getCurrentPosition());
		setResult(RESULT_OK, in);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1))
				urlToLoad = intent.getStringExtra(Constants.EXTRA_PARAM_1);
			if (intent.hasExtra(Constants.EXTRA_PARAM_2))
				thumbnail = intent.getStringExtra(Constants.EXTRA_PARAM_2);
			if (intent.hasExtra(Constants.EXTRA_PARAM_3))
				transitionName = intent.getStringExtra(Constants.EXTRA_PARAM_3);
			if (intent.hasExtra(Constants.EXTRA_PARAM_4))
				lastPosition = intent.getLongExtra(Constants.EXTRA_PARAM_4, 0);
			if (intent.hasExtra(Constants.EXTRA_PARAM_5))
				wantsLandscape = intent.getBooleanExtra(Constants.EXTRA_PARAM_5, false);
			if (intent.hasExtra(Constants.EXTRA_PARAM_6))
				postOrMessageId = intent.getStringExtra(Constants.EXTRA_PARAM_6);
			if (intent.hasExtra(Constants.EXTRA_PARAM_7))
				fromChat = intent.getBooleanExtra(Constants.EXTRA_PARAM_7, false);

			Object obj = AudioVideoCache.Companion.getInstance(this).getMedia(urlToLoad, HLMediaType.VIDEO);
			if (obj instanceof Uri)
				uriToLoad = ((Uri) obj);
		}
	}


	private void manageCutout() {
		int padding = getResources().getDimensionPixelSize(R.dimen.activity_margin);
		ConstraintLayout.LayoutParams lp1 = ((ConstraintLayout.LayoutParams) closeBtn.getLayoutParams());
		lp1.topMargin = safeCutout;
		closeBtn.setLayoutParams(lp1);
		closeBtn.setPaddingRelative(padding, padding, padding, padding);

		// shareBn is automatically positioned because of its constraints (linked to closeBtn)

	}


	private void playVideo() {

		int res = Utils.hasOreo() ?
				audioManager.requestAudioFocus(audioRequest) :
				audioManager.requestAudioFocus(focusChange -> {
					// Ignore
				}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		synchronized(mFocusLock) {
			if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
				mPlaybackNowAuthorized = false;
			}
			else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				mPlaybackNowAuthorized = true;
				if (lastPosition > 0) {
					mThumbnailLayout.setVisibility(View.GONE);
					seekTo((int) lastPosition);
				}
				else {
					MediaHelper.loadPictureWithGlide(VideoViewActivityExo.this, thumbnail, mThumbnailView);
					mThumbnailLayout.setVisibility(View.VISIBLE);
				}

				registerReceiver(noisyAudioStreamReceiver, intentFilter);
//				LocalBroadcastManager.getInstance(this).registerReceiver(noisyAudioStreamReceiver, intentFilter);

				mThumbnailLayout.setVisibility(View.GONE);
			}
			else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
				mPlaybackDelayed = true;
				mPlaybackNowAuthorized = false;
			}
		}


		try {
			// Produces DataSource instances through which media data is loaded.
			DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
					Util.getUserAgent(this, getString(R.string.app_name)));
			// This is the MediaSource representing the media to be played.
			MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
					.createMediaSource(uriToLoad != null ? uriToLoad : Uri.parse(urlToLoad));
			// Prepare the player with the source.
			exoPlayer.prepare(videoSource);
			exoPlayer.setPlayWhenReady(true);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	//region == Media Section ===

	public void start() {
		if (exoPlayer != null) {
			if (lastPosition > 0)
				seekTo((int) lastPosition);

			exoPlayer.setPlayWhenReady(true);
		}
	}

	public void pause() {
		lastPosition = getCurrentPosition();

		if (exoPlayer != null)
			exoPlayer.setPlayWhenReady(false);
	}

	public void release() {
		pause();

		if (exoPlayer != null) {
			exoPlayer.stop();
			exoPlayer.release();
			exoPlayer = null;
		}
	}

	public long getDuration() {
		return exoPlayer != null ?  exoPlayer.getDuration() : 0;
	}

	public long getCurrentPosition() {
		return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
	}

	public void seekTo(int i) {
		if (exoPlayer != null)
			exoPlayer.seekTo(i);
	}

	public boolean isPlaying() {
		return exoIsPlaying;
	}

	public int getBufferPercentage() {
		return exoPlayer != null ? exoPlayer.getBufferedPercentage() : 0;
	}

	//endregion


	boolean mPlaybackDelayed = false;
	boolean mPlaybackNowAuthorized = false;
	private boolean mResumeOnFocusGain = false;
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				if (mPlaybackDelayed || mResumeOnFocusGain) {
					synchronized(mFocusLock) {
						mPlaybackDelayed = false;
						mResumeOnFocusGain = false;
					}
					playerDuck(false);
					start();
				}
				break;

			case AudioManager.AUDIOFOCUS_LOSS:
				synchronized(mFocusLock) {
					mResumeOnFocusGain = false;
					mPlaybackDelayed = false;
				}
				pause();
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				synchronized(mFocusLock) {
					mResumeOnFocusGain = true;
					mPlaybackDelayed = false;
				}
				pause();
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				playerDuck(true);
				break;
		}
	}


	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

		switch (playbackState) {
			case Player.STATE_READY:
				exoIsPlaying = playWhenReady;
				if (exoIsPlaying)
					progressView.setVisibility(View.GONE);

				break;

			case Player.STATE_BUFFERING:
				int percent = getBufferPercentage();
				progressMessage.setText(progressMessage.getContext().getString(R.string.buffering_video_perc, String.valueOf(percent)));

				if (percent == 100)
					progressView.setVisibility(View.GONE);
				else
					progressView.setVisibility(View.VISIBLE);
				break;

			case Player.STATE_ENDED:
				seekTo(0);
				break;
		}
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

	@Override
	public void onPlayerError(ExoPlaybackException error) {}

	@Override
	public void onPositionDiscontinuity(int reason) {}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

	@Override
	public void onSeekProcessed() {}



	private class BecomingNoisyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()) ||
					(Utils.hasLollipop() && AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction()))) {

				if (exoPlayer != null)
					exoPlayer.setPlayWhenReady(false);
			}
		}
	}

	public synchronized void playerDuck(boolean duck) {
		if (exoPlayer != null) {
			// Reduce the volume by half when ducking - otherwise play at full volume.
			exoPlayer.setVolume(duck ? 0.5f : 1.0f);
		}
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
//			Toast.makeText(VideoViewActivity.this, "YAY!", Toast.LENGTH_SHORT).show();
			registerReceiver(noisyAudioStreamReceiver, intentFilter);
//			LocalBroadcastManager.getInstance(this).registerReceiver(noisyAudioStreamReceiver, intentFilter);
			return true;
		}
		else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
//			Toast.makeText(VideoViewActivity.this, "YAY!", Toast.LENGTH_SHORT).show();
			try {
				unregisterReceiver(noisyAudioStreamReceiver);
//				LocalBroadcastManager.getInstance(this).unregisterReceiver(noisyAudioStreamReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			return true;
		}

		return super.dispatchKeyEvent(event);
	}



	private class DispatcherMediaController extends MediaController {

		public DispatcherMediaController(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public DispatcherMediaController(Context context, boolean useFastForward) {
			super(context, useFastForward);
		}

		public DispatcherMediaController(Context context) {
			super(context);
		}


		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {

			if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
//				Toast.makeText(VideoViewActivity.this, "YAY!", Toast.LENGTH_SHORT).show();
				registerReceiver(noisyAudioStreamReceiver, intentFilter);
//				LocalBroadcastManager.getInstance(VideoViewActivity.this).registerReceiver(noisyAudioStreamReceiver, intentFilter);
				return true;
			}
			else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
//				Toast.makeText(VideoViewActivity.this, "YAY!", Toast.LENGTH_SHORT).show();
				try {
					unregisterReceiver(noisyAudioStreamReceiver);
//					LocalBroadcastManager.getInstance(VideoViewActivity.this).unregisterReceiver(noisyAudioStreamReceiver);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				return true;
			}

			return super.dispatchKeyEvent(event);
		}
	}


	//region == SHARE ==

	@org.jetbrains.annotations.Nullable
	@Override
	public View getProgressView() {
		return null;
	}

	@Override
	public void afterOps() {}

	@NotNull
	@Override
	public String getUserID() {
		return mUser.getId();
	}

	@NotNull
	@Override
	public String getPostOrMessageID() {
		return postOrMessageId;
	}

	//endregion

}
