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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;

import im.ene.toro.media.PlaybackInfo;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.base.HLActivity;
import rs.highlande.highlanders_app.utility.AnalyticsUtils;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.LogUtils;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.caches.AudioVideoCache;
import rs.highlande.highlanders_app.utility.helpers.HLMediaType;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;

/**
 * @author mbaldrighi on 4/26/2018.
 */
public class VideoViewActivity extends HLActivity implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener,
		AudioManager.OnAudioFocusChangeListener, MediaController.MediaPlayerControl {

	public static final String LOG_TAG = VideoViewActivity.class.getCanonicalName();

	private String urlToLoad, thumbnail, transitionName;
	private Uri uriToLoad;

	private AudioManager audioManager;
	private AudioFocusRequest audioRequest;

	private ImageView mThumbnailView;
	private View progressView, mThumbnailLayout;
	private TextView progressMessage;

	private MediaPlayer mMediaPlayer;
	private MediaController mMediaController;
	private boolean mediaPlayerPrepared, surfaceCreated;

	private long lastPosition;

	private PlaybackInfo playBackInfo;

	final Object mFocusLock = new Object();

	private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	private BecomingNoisyReceiver noisyAudioStreamReceiver = new BecomingNoisyReceiver();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_view);
		setRootContent(R.id.root_content);

		View decorView = getWindow().getDecorView();
		// Hide both the navigation bar and the status bar.
		// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		decorView.setSystemUiVisibility(uiOptions);

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

		findViewById(R.id.play_btn).setVisibility(View.GONE);
		findViewById(R.id.pause_layout).setVisibility(View.GONE);

		SurfaceView videoView = findViewById(R.id.video_view);

		if (Utils.hasLollipop())
			videoView.setTransitionName(transitionName);

		videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder surfaceHolder) {
				LogUtils.d(LOG_TAG, "First surface created!");
				surfaceCreated = true;

				if (mMediaPlayer != null) {
					mMediaPlayer.setDisplay(surfaceHolder);
					try {
						if (mediaPlayerPrepared) {
							mMediaPlayer.setScreenOnWhilePlaying(true);
							start();
						}
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {}

			@Override
			public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
				LogUtils.d(LOG_TAG, "Surface destroyed!");
			}
		});
		playVideo();

		findViewById(R.id.close_btn).setOnClickListener(v -> onBackPressed());
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.FEED_VIDEO_VIEWER);
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

		super.onStop();
	}

	@Override
	public void onBackPressed() {
		Intent in = new Intent();

		// TODO: 10/25/2018    for now needed only in ChatMessageFragment
		in.putExtra(Constants.EXTRA_PARAM_1, playBackInfo);
		setResult(RESULT_OK, in);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mMediaController.show();
		return false;
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
				playBackInfo = intent.getParcelableExtra(Constants.EXTRA_PARAM_5);

			Object obj = AudioVideoCache.Companion.getInstance(this).getMedia(urlToLoad, HLMediaType.VIDEO);
			if (obj instanceof Uri)
				uriToLoad = ((Uri) obj);
		}
	}


	private void playVideo() {
		try {
			mMediaController = new MediaController(this);

			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(this, uriToLoad != null ? uriToLoad : Uri.parse(urlToLoad));
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.prepareAsync();
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	//region == Media Section ==

	@Override
	public void onPrepared(MediaPlayer mp) {
		mMediaPlayer = mp;
		mediaPlayerPrepared = true;

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
					mMediaPlayer.seekTo((int) lastPosition);
				}
				else {
					MediaHelper.loadPictureWithGlide(VideoViewActivity.this, thumbnail, mThumbnailView);
					mThumbnailLayout.setVisibility(View.VISIBLE);
				}

				mMediaPlayer.setOnBufferingUpdateListener(VideoViewActivity.this);
				registerReceiver(noisyAudioStreamReceiver, intentFilter);
//				LocalBroadcastManager.getInstance(this).registerReceiver(noisyAudioStreamReceiver, intentFilter);

				mThumbnailLayout.setVisibility(View.GONE);

				mMediaController.setMediaPlayer(this);
				mMediaController.setAnchorView(findViewById(R.id.video_view));
				mMediaController.setEnabled(true);

				if (surfaceCreated) {
					mMediaPlayer.setScreenOnWhilePlaying(true);
					start();
				}

				new Handler().post(() -> mMediaController.show());
			}
			else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
				mPlaybackDelayed = true;
				mPlaybackNowAuthorized = false;
			}
		}
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		mediaPlayer.seekTo(0);
		new Handler().post(() -> mMediaController.show());

		try {
//			LocalBroadcastManager.getInstance(this).unregisterReceiver(noisyAudioStreamReceiver);
			unregisterReceiver(noisyAudioStreamReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		progressMessage.setText(progressMessage.getContext().getString(R.string.buffering_video_perc, String.valueOf(percent)));
		if (percent == 100)
			progressView.setVisibility(View.GONE);
	}

	private boolean hasStarted;
	@Override
	public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
		if (i == MediaPlayer.MEDIA_INFO_BUFFERING_END && hasStarted) {
			progressView.setVisibility(View.GONE);
			hasStarted = false;
			return true;
		} else if (i == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
			progressView.setVisibility(View.VISIBLE);
			hasStarted = true;
		}

		return false;
	}

	public void start() {
		if (mMediaPlayer != null && mediaPlayerPrepared) {
			if (lastPosition > 0)
				mMediaPlayer.seekTo((int) lastPosition);

			mMediaPlayer.start();
		}
	}

	public void pause() {
		lastPosition = getCurrentPosition();
		if (playBackInfo != null)
			playBackInfo.setResumePosition(lastPosition);

		if (mMediaPlayer != null && mMediaPlayer.isPlaying())
			mMediaPlayer.pause();
	}

	public void release() {
		pause();

		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	public int getDuration() {
		return mMediaPlayer != null ?  mMediaPlayer.getDuration() : 0;
	}

	public int getCurrentPosition() {
		return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
	}

	public void seekTo(int i) {
		if (mMediaPlayer != null)
			mMediaPlayer.seekTo(i);
	}

	public boolean isPlaying() {
		return mMediaPlayer != null && mMediaPlayer.isPlaying();
	}

	public int getBufferPercentage() {
		return 0;
	}

	public boolean canPause() {
		return true;
	}

	public boolean canSeekBackward() {
		return true;
	}

	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		return 0;
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


	private class BecomingNoisyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()) ||
					(Utils.hasLollipop() && AudioManager.ACTION_HEADSET_PLUG.equals(intent.getAction()))) {

				if (mMediaPlayer != null)
					mMediaPlayer.pause();
			}
		}
	}

	public synchronized void playerDuck(boolean duck) {
		if (mMediaPlayer != null) {
			// Reduce the volume by half when ducking - otherwise play at full volume.
			mMediaPlayer.setVolume(duck ? 0.5f : 1.0f, duck ? 0.5f : 1.0f);
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

}
