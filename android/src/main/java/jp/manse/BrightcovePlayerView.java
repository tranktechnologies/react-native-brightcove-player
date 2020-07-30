package jp.manse;

import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import com.brightcove.player.display.ExoPlayerVideoDisplayComponent;
import com.brightcove.player.edge.Catalog;
import com.brightcove.player.edge.OfflineCatalog;
import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.mediacontroller.BrightcoveMediaController;
import com.brightcove.player.mediacontroller.BrightcoveMediaControlRegistry;
import com.brightcove.player.mediacontroller.buttons.SeekButtonController;
import com.brightcove.player.model.Video;
import com.brightcove.player.analytics.Analytics;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;

import java.util.HashMap;
import java.util.Map;

import jp.manse.util.AudioFocusManager;
import jp.manse.util.NetworkChangeReceiver;
import jp.manse.util.NetworkUtil;

public class BrightcovePlayerView extends RelativeLayout implements LifecycleEventListener,
        AudioFocusManager.AudioFocusChangedListener, NetworkChangeReceiver.NetworkChangeListener {
    private ThemedReactContext context;
    private ReactApplicationContext applicationContext;
    private BrightcoveExoPlayerVideoView playerVideoView;
    private BrightcoveMediaController mediaController;
    private String policyKey;
    private String accountId;
    private String playerId;
    private String videoId;
    private String referenceId;
    private String videoToken;
    private Catalog catalog;
	private Analytics analytics;
    private OfflineCatalog offlineCatalog;
	private Map<String, Object> mediaInfo;
    private boolean autoPlay = true;
    private boolean playing = false;
    private int bitRate = 0;
    private float playbackRate = 1;
	private static final int SEEK_AMOUNT = 10000; // In milliseconds
    private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();
    private AudioFocusManager audioFocusManager;
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean isNetworkForcedPause = false;
    private boolean isRegisteredConnectivityChanged = false;
    private boolean hostActive = true;

    public BrightcovePlayerView(ThemedReactContext context, ReactApplicationContext applicationContext) {
        super(context);
        this.context = context;
        this.applicationContext = applicationContext;
        this.applicationContext.addLifecycleEventListener(this);
        this.setBackgroundColor(Color.BLACK);

        this.playerVideoView = new BrightcoveExoPlayerVideoView(this.context.getCurrentActivity());

        this.addView(this.playerVideoView);
        this.playerVideoView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.playerVideoView.finishInitialization();
        this.mediaController = new BrightcoveMediaController(this.playerVideoView);
        this.playerVideoView.setMediaController(this.mediaController);
        this.requestLayout();
        setupLayout();
        ViewCompat.setTranslationZ(this, 9999);

		// Change the seek amounts
		final BrightcoveMediaControlRegistry registry = this.playerVideoView.getBrightcoveMediaController().getMediaControlRegistry();
        ((SeekButtonController) registry.getButtonController(R.id.rewind)).setSeekDefault(SEEK_AMOUNT);

        // Implement the analytics to the  Brightcove player
        this.analytics = this.playerVideoView.getAnalytics();

        // Create AudioFocusManager instance and register BrightcovePlayerView as a listener
        this.audioFocusManager = new AudioFocusManager(this.context);
        this.audioFocusManager.registerListener(this);

        // Create Network Change Broadcast receiver and register this class to listen to network status changes
        this.networkChangeReceiver = new NetworkChangeReceiver();
        registerConnectivityChange();

        EventEmitter eventEmitter = this.playerVideoView.getEventEmitter();
        eventEmitter.on(EventType.VIDEO_SIZE_KNOWN, new EventListener() {
            @Override
            public void processEvent(Event e) {
                fixVideoLayout();
                updateBitRate();
                updatePlaybackRate();
            }
        });
        eventEmitter.on(EventType.READY_TO_PLAY, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_READY, event);
            }
        });

        eventEmitter.on(EventType.DID_SET_VIDEO, new EventListener() {
            @Override
            public void processEvent(Event e) {
				WritableMap mediaInfo = Arguments.createMap();
				mediaInfo.putString("title", BrightcovePlayerView.this.mediaInfo.get("name").toString());

                WritableMap event = Arguments.createMap();
				event.putMap("mediainfo", mediaInfo);

                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_METADATA_LOADED, event);
            }
        });

        eventEmitter.on(EventType.DID_PLAY, new EventListener() {
            @Override
            public void processEvent(Event e) {
                BrightcovePlayerView.this.playing = true;
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_PLAY, event);
                // When the playback starts, request focus to stop any background audio
                audioFocusManager.requestFocus();
            }
        });
        eventEmitter.on(EventType.DID_PAUSE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                BrightcovePlayerView.this.playing = false;
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_PAUSE, event);
                // When the playback stops, release the audio focus
                audioFocusManager.abandonFocus();
            }
        });
        eventEmitter.on(EventType.COMPLETED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_END, event);
            }
        });
        eventEmitter.on(EventType.PROGRESS, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                Integer playhead = (Integer) e.properties.get(Event.PLAYHEAD_POSITION);
                event.putDouble("currentTime", playhead / 1000d);
                Integer duration = (Integer) e.properties.get(Event.VIDEO_DURATION);
                event.putDouble("duration", duration / 1000d);
                Integer liveEdge = BrightcovePlayerView.this.playerVideoView.getVideoDisplay().getLiveEdge();
                Boolean isInLiveEdge = BrightcovePlayerView.this.playerVideoView.getVideoDisplay().isInLiveEdge();
                event.putDouble("liveEdge", liveEdge / 1000d);
                event.putBoolean("isInLiveEdge", isInLiveEdge );

                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_PROGRESS, event);
            }
        });
        eventEmitter.on(EventType.ENTER_FULL_SCREEN, new EventListener() {
            @Override
            public void processEvent(Event e) {
                mediaController.show();
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_BEFORE_ENTER_FULLSCREEN, Arguments.createMap());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_ENTER_FULLSCREEN, event);
            }
        });
        eventEmitter.on(EventType.EXIT_FULL_SCREEN, new EventListener() {
            @Override
            public void processEvent(Event e) {
                mediaController.show();
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_BEFORE_EXIT_FULLSCREEN, Arguments.createMap());
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_EXIT_FULLSCREEN, event);
            }
        });
        eventEmitter.on(EventType.VIDEO_DURATION_CHANGED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                Integer duration = (Integer) e.properties.get(Event.VIDEO_DURATION);
                WritableMap event = Arguments.createMap();
                event.putDouble("duration", duration / 1000d);
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_CHANGE_DURATION, event);
            }
        });
        eventEmitter.on(EventType.BUFFERED_UPDATE, new EventListener() {
            @Override
            public void processEvent(Event e) {
                Integer percentComplete = (Integer) e.properties.get(Event.PERCENT_COMPLETE);
                WritableMap event = Arguments.createMap();
                event.putDouble("bufferProgress", percentComplete / 100d);
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_UPDATE_BUFFER_PROGRESS, event);
            }
        });
      	eventEmitter.on(EventType.BUFFERING_STARTED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext
                        .getJSModule(RCTEventEmitter.class)
                        .receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_BUFFERING_STARTED, event);
            }
        });
        eventEmitter.on(EventType.BUFFERING_COMPLETED, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap event = Arguments.createMap();
                ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
                reactContext
                        .getJSModule(RCTEventEmitter.class)
                        .receiveEvent(BrightcovePlayerView.this.getId(), BrightcovePlayerManager.EVENT_BUFFERING_COMPLETED, event);
            }
        });
		// Emits all the errors back to the React Native
      	eventEmitter.on(EventType.ERROR, new EventListener() {
            @Override
            public void processEvent(Event e) {
                WritableMap error = mapToRnWritableMap(e.properties);
                emitError(error);
            }
        });
    }

	/**
	 * Dispatch the event to the player to ENTER the full screen state
	 */
    public void dispatchEnterFullScreenClickEvent() {
        this.playerVideoView.getEventEmitter().emit(EventType.ENTER_FULL_SCREEN);
    }

	/**
	 * Dispatch the event to the player to EXIT the full screen state
	 */
    public void dispatchExitFullScreenClickEvent() {
        this.playerVideoView.getEventEmitter().emit(EventType.EXIT_FULL_SCREEN);
    }

	/**
	 * Emits the errors back to React Native application
	 * @param error {WritableMap} - The error object with the information of the error
	 */
	private void emitError(WritableMap error) {
        ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(
                        BrightcovePlayerView.this.getId(),
                        BrightcovePlayerManager.EVENT_ERROR,
                        error
                );
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustMediaControllerDimensions();
    }

    private void adjustMediaControllerDimensions() {
        mediaController.show();
        mediaController.getBrightcoveControlBar().setVisibility(VISIBLE);
        mediaController.getBrightcoveControlBar().setMinimumWidth(getWidth());
        mediaController.getBrightcoveControlBar().setAlign(true);
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
        this.loadVideo();
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
		this.analytics.setAccount(accountId);
        this.loadVideo();
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
		this.analytics.setDestination("bcsdk://" + playerId);
        this.loadVideo();
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
        this.referenceId = null;
        this.loadVideo();
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
        this.videoId = null;
        this.loadVideo();
    }

    public void setVideoToken(String videoToken) {
        this.videoToken = videoToken;
        this.loadVideo();
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public void setPlay(boolean play) {
        if (this.playing && play) return;
        if (play) {
            this.playerVideoView.start();
        } else {
            this.playerVideoView.pause();
        }
    }

    public void setDefaultControlDisabled(boolean disabled) {
        this.mediaController.hide();
        this.mediaController.setShowHideTimeout(disabled ? 1 : 4000);
    }

    public void setFullscreen(boolean fullscreen) {
        this.mediaController.show();
        WritableMap event = Arguments.createMap();
        event.putBoolean("fullscreen", fullscreen);
        ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcovePlayerView.this.getId(), fullscreen ?
                BrightcovePlayerManager.EVENT_ENTER_FULLSCREEN : BrightcovePlayerManager.EVENT_EXIT_FULLSCREEN, event);
    }

    public void setVolume(float volume) {
        Map<String, Object> details = new HashMap<>();
        details.put(Event.VOLUME, volume);
        this.playerVideoView.getEventEmitter().emit(EventType.SET_VOLUME, details);
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
        this.updateBitRate();
    }

    public void setPlaybackRate(float playbackRate) {
        if (playbackRate == 0) return;
        this.playbackRate = playbackRate;
        this.updatePlaybackRate();
    }

    public void seekTo(int time) {
        this.playerVideoView.seekTo(time);
    }
    public void seekToLive() {
        this.playerVideoView.seekToLive();
    }

    //We need to stop the player to avoid a potential memory leak.
    public void stopPlayback() {
        if(this.playerVideoView != null){
            this.playerVideoView.stopPlayback();
            this.playerVideoView.clear();
        }
    }

    private void updateBitRate() {
        ExoPlayerVideoDisplayComponent videoDisplay = ((ExoPlayerVideoDisplayComponent) this.playerVideoView.getVideoDisplay());
        ExoPlayer player = videoDisplay.getExoPlayer();
        DefaultTrackSelector trackSelector = videoDisplay.getTrackSelector();
        if (player == null) return;
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;
        Integer rendererIndex = null;
        for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0 && player.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                rendererIndex = i;
                break;
            }
        }

        if (rendererIndex == null) return;
        if (bitRate == 0) {
            trackSelector.clearSelectionOverrides(rendererIndex);
            return;
        }
        int resultBitRate = -1;
        int targetGroupIndex = -1;
        int targetTrackIndex = -1;
        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            TrackGroup group = trackGroups.get(groupIndex);
            if (group != null) {
                for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                    Format format = group.getFormat(trackIndex);
                    if (format != null && mappedTrackInfo.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex)
                            == RendererCapabilities.FORMAT_HANDLED) {
                        if (resultBitRate == -1 ||
                                (resultBitRate > bitRate ? (format.bitrate < resultBitRate) :
                                        (format.bitrate <= bitRate && format.bitrate > resultBitRate))) {
                            targetGroupIndex = groupIndex;
                            targetTrackIndex = trackIndex;
                            resultBitRate = format.bitrate;
                        }
                    }
                }
            }
        }
        if (targetGroupIndex != -1 && targetTrackIndex != -1) {
            trackSelector.setSelectionOverride(rendererIndex, trackGroups,
                    new DefaultTrackSelector.SelectionOverride(targetGroupIndex, targetTrackIndex));
        }
    }

    private void updatePlaybackRate() {
        ExoPlayer expPlayer = ((ExoPlayerVideoDisplayComponent) this.playerVideoView.getVideoDisplay()).getExoPlayer();
        if (expPlayer != null) {
            expPlayer.setPlaybackParameters(new PlaybackParameters(playbackRate, 1f));
        }
    }

    private void loadVideo() {
        if (this.videoToken != null && !this.videoToken.equals("")) {
            this.offlineCatalog = new OfflineCatalog(this.context, this.playerVideoView.getEventEmitter(), this.accountId, this.policyKey);
            try {
                Video video = this.offlineCatalog.findOfflineVideoById(this.videoToken);
                if (video != null) {
                    playVideo(video);
                }
            } catch (Exception e) {
            }
            return;
        }
        VideoListener listener = new VideoListener() {
            @Override
            public void onVideo(Video video) {
                if((BrightcovePlayerView.this.videoId != null && BrightcovePlayerView.this.videoId.equals(video.getId())) || 
                        (BrightcovePlayerView.this.referenceId != null && BrightcovePlayerView.this.referenceId.equals(video.getReferenceId()))){
                    if(BrightcovePlayerView.this.hostActive){
                        Log.e("Brightcove Player",video.getId()+"Play");
                        BrightcovePlayerView.this.mediaInfo = video.getProperties();
                        playVideo(video);
                    }
                }
            }

            @Override
            public void onError(String s) {
                WritableMap error = Arguments.createMap();
                error.putString("error_code", "CATALOG_FETCH_ERROR");
                error.putString("errorMessage", s);
                emitError(error);
            }
        };

        this.catalog = new Catalog(this.playerVideoView.getEventEmitter(), this.accountId, this.policyKey);

		if (this.accountId != null) {
			if (this.videoId != null) {
				this.catalog.findVideoByID(this.videoId, listener);
			} else if (this.referenceId != null) {
				this.catalog.findVideoByReferenceID(this.referenceId, listener);
			}
		}
    }

    private void playVideo(Video video) {
        BrightcovePlayerView.this.playerVideoView.stopPlayback();
        BrightcovePlayerView.this.playerVideoView.clear();
        BrightcovePlayerView.this.playerVideoView.add(video);
        if (BrightcovePlayerView.this.autoPlay) {
            BrightcovePlayerView.this.playerVideoView.start();
        }
    }

    private void fixVideoLayout() {
        int viewWidth = this.getMeasuredWidth();
        int viewHeight = this.getMeasuredHeight();
        SurfaceView surfaceView = (SurfaceView) this.playerVideoView.getRenderView();
        surfaceView.measure(viewWidth, viewHeight);
        int surfaceWidth = surfaceView.getMeasuredWidth();
        int surfaceHeight = surfaceView.getMeasuredHeight();
        int leftOffset = (viewWidth - surfaceWidth) / 2;
        int topOffset = (viewHeight - surfaceHeight) / 2;
        surfaceView.layout(leftOffset, topOffset, leftOffset + surfaceWidth, topOffset + surfaceHeight);
    }

    private void printKeys(Map<String, Object> map) {
    }

    // Converts MAP into React WritableMap
    // Also, doesn't work with recursive maps or arrays
    private WritableMap mapToRnWritableMap(Map<String, Object> map) {
        WritableMap writableMap = Arguments.createMap();
        for (String key : map.keySet()) {
            Object val = map.get(key);

            if (val instanceof String) {
                writableMap.putString(key, (String)val);
            } else if (val instanceof Integer) {
                writableMap.putInt(key, (Integer)val);
            } else if (val instanceof Boolean) {
                writableMap.putBoolean(key, (Boolean)val);
            } else if (val instanceof Double) {
                writableMap.putDouble(key, (Double)val);
            }
        }

        return writableMap;
    }


    @Override
    public void onHostResume() {
         Log.e("BrightcovePlayer","Host Resumed");
	    // Register to audio focus changes when the screen resumes
        this.hostActive = true;
	    audioFocusManager.registerListener(this);
        registerConnectivityChange();
    }

    @Override
    public void onHostPause() {
        Log.e("BrightcovePlayer","Host Paused");
        // Unregister from audio focus changes when the screen goes in the background
        this.hostActive = false;
        audioFocusManager.unregisterListener();
        unregisterConnectivityChange();
    }

    @Override
    protected void onAttachedToWindow() {
        Log.e("BrightcovePlayer","Attach to Window");
        this.hostActive = true;
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.e("BrightcovePlayer","Detach Form Window");
        // For safety, clear listeners in onDetachedFromWindow too since when the back button or home toolbar button are
        // clicked, onHostPause does not get executed
        this.hostActive = false;
        super.onDetachedFromWindow();
        // Unregister from audio focus changes when the screen goes in the background
        audioFocusManager.unregisterListener();
        unregisterConnectivityChange();
    }

    @Override
    public void onHostDestroy() {
        this.playerVideoView.destroyDrawingCache();
        this.playerVideoView.clear();
        this.removeAllViews();
        this.applicationContext.removeLifecycleEventListener(this);
    }

    // A view with elements that have a visibility to gone on the initial render won't be displayed after you've set
    // its visibility to visible. view.isShown() will return true, but it will not be there or it will be there but not
    // really re-layout. This workaround somehow draws the child views manually
    // https://github.com/facebook/react-native/issues/17968

    private void setupLayout() {

        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                manuallyLayoutChildren();
                getViewTreeObserver().dispatchOnGlobalLayout();
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    private void manuallyLayoutChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    @Override
    public void audioFocusChanged(boolean hasFocus) {
	    // Pause the video when it looses focus
	    if (!hasFocus && playerVideoView.isPlaying()) {
	        playerVideoView.pause();
        }
    }

    @Override
    public void onConnected() {
        // When network is regained, if this is not and offline video (VideoToken check), set the network forced pause flag to false and start playback
        if (isNetworkForcedPause && (videoToken == null || videoToken.isEmpty())) {
            isNetworkForcedPause = false;
            onNetworkConnectivityChange(NetworkUtil.STATUS_RECONNECTED);
            this.playerVideoView.start();
        }
    }

    @Override
    public void onDisconnected() {
        // When network is disconnected, if this is not and offline video (VideoToken check), then set a flag that the video will be forcebly paused when
        // the playback of the remaining buffered part of the video ends
        if (videoToken == null || videoToken.isEmpty()) {
            isNetworkForcedPause = true;
            onNetworkConnectivityChange(NetworkUtil.STATUS_STALLED);
        }
    }

    private void registerConnectivityChange() {
        // Register this class to listen to network change events
        // Register the network change receiver
        if (!isRegisteredConnectivityChanged) {
            isRegisteredConnectivityChanged = true;
            this.networkChangeReceiver.registerListener(this);
            IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            this.context.registerReceiver(this.networkChangeReceiver, intentFilter);
        }
    }

    private void unregisterConnectivityChange() {
        // Unregister this class from listenning to network change events
        // Unregister the network change receiver
        if (isRegisteredConnectivityChanged) {
            isRegisteredConnectivityChanged = false;
            this.networkChangeReceiver.unregisterListener();
            this.context.unregisterReceiver(this.networkChangeReceiver);
        }
    }

    private void onNetworkConnectivityChange(String networkStatus) {
        // Send an event to React with the network status
        WritableMap event = Arguments.createMap();
        event.putString("status", networkStatus);
        ReactContext reactContext = (ReactContext) BrightcovePlayerView.this.getContext();
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(
                        BrightcovePlayerView.this.getId(),
                        BrightcovePlayerManager.EVENT_NETWORK_CONNECTIVITY_CHANGED,
                        event
                );
    }
}
