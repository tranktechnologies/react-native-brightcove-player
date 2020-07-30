package jp.manse;

import android.support.annotation.Nullable;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.bridge.ReactMethod;
import java.util.HashMap;
import java.util.Map;


public class BrightcovePlayerManager extends SimpleViewManager<BrightcovePlayerView> {
    public static final String REACT_CLASS = "BrightcovePlayer";
    public static final int COMMAND_SEEK_TO = 1;
    public static final int COMMAND_SET_FULLSCREEN = 2;
    public static final int COMMAND_PLAY_VIDEO = 3;
    public static final int COMMAND_BIT_RATE = 4;
    public static final int SEEK_TO_LIVE=5;
    public static final int COMMAND_STOP_PLAYBACK = 6;
    public static final String EVENT_READY = "ready";
    public static final String EVENT_METADATA_LOADED = "metadata_loaded";
    public static final String EVENT_PLAY = "play";
    public static final String EVENT_PAUSE = "pause";
    public static final String EVENT_END = "end";
    public static final String EVENT_PROGRESS = "progress";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_CHANGE_DURATION = "change_duration";
    public static final String EVENT_UPDATE_BUFFER_PROGRESS = "update_buffer_progress";
    public static final String EVENT_BUFFERING_STARTED = "buffering_started";
    public static final String EVENT_BUFFERING_COMPLETED = "buffering_copleted";
    public static final String EVENT_BEFORE_ENTER_FULLSCREEN = "event_before_enter_fullscreen";
    public static final String EVENT_BEFORE_EXIT_FULLSCREEN = "event_before_exit_fullscreen";
    public static final String EVENT_ENTER_FULLSCREEN = "event_enter_fullscreen";
    public static final String EVENT_EXIT_FULLSCREEN = "event_exit_fullscreen";
    public static final String EVENT_NETWORK_CONNECTIVITY_CHANGED = "event_network_connectivity_changed";

    private ReactApplicationContext applicationContext;

    public BrightcovePlayerManager(ReactApplicationContext context) {
        super();
        this.applicationContext = context;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public BrightcovePlayerView createViewInstance(ThemedReactContext ctx) {
        BrightcovePlayerView brightcovePlayerView = new BrightcovePlayerView(ctx, applicationContext);
        return brightcovePlayerView;
    }

    @ReactProp(name = "policyKey")
    public void setPolicyKey(BrightcovePlayerView view, String policyKey) {
        view.setPolicyKey(policyKey);
    }

    @ReactProp(name = "accountId")
    public void setAccountId(BrightcovePlayerView view, String accountId) {
        view.setAccountId(accountId);
    }

    @ReactProp(name = "playerId")
    public void setPlayerId(BrightcovePlayerView view, String playerId) {
        view.setPlayerId(playerId);
    }

    @ReactProp(name = "videoId")
    public void setVideoId(BrightcovePlayerView view, String videoId) {
        view.setVideoId(videoId);
    }

    @ReactProp(name = "referenceId")
    public void setReferenceId(BrightcovePlayerView view, String referenceId) {
        view.setReferenceId(referenceId);
    }

    @ReactProp(name = "videoToken")
    public void setVideoToken(BrightcovePlayerView view, String videoToken) {
        view.setVideoToken(videoToken);
    }

    @ReactProp(name = "autoPlay")
    public void setAutoPlay(BrightcovePlayerView view, boolean autoPlay) {
        view.setAutoPlay(autoPlay);
    }

    @ReactProp(name = "play")
    public void setPlay(BrightcovePlayerView view, boolean play) {
        view.setPlay(play);
    }

    @ReactProp(name = "disableDefaultControl")
    public void setDefaultControlDisabled(BrightcovePlayerView view, boolean disableDefaultControl) {
        view.setDefaultControlDisabled(disableDefaultControl);
    }

    @ReactProp(name = "volume")
    public void setVolume(BrightcovePlayerView view, float volume) {
        view.setVolume(volume);
    }

    @ReactProp(name = "bitRate")
    public void setBitRate(BrightcovePlayerView view, float bitRate) {
        view.setBitRate((int)bitRate);
    }

    @ReactProp(name = "playbackRate")
    public void setPlaybackRate(BrightcovePlayerView view, float playbackRate) {
        view.setPlaybackRate(playbackRate);
    }

    @ReactProp(name = "fullscreen")
    public void setFullscreen(BrightcovePlayerView view, boolean fullscreen) {
        view.setFullscreen(fullscreen);
    }

    public void playVideo(BrightcovePlayerView view, boolean play) {
           view.setPlay(play);
       }

    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "seekTo", COMMAND_SEEK_TO,
                "setFullscreen", COMMAND_SET_FULLSCREEN,
                "playVideo", COMMAND_PLAY_VIDEO,
                "setBitRate", COMMAND_BIT_RATE,
                "seekToLive", SEEK_TO_LIVE,
                "stopPlayback", COMMAND_STOP_PLAYBACK
        );
    }

    @Override
    public void receiveCommand(BrightcovePlayerView view, int commandType, @Nullable ReadableArray args) {
        Assertions.assertNotNull(view);
        Assertions.assertNotNull(args);
        switch (commandType) {
            case SEEK_TO_LIVE: {
                view.seekToLive();
            return;
            }
            case COMMAND_SEEK_TO: {
                view.seekTo((int)(args.getDouble(0) * 1000));
                return;
            }
            case COMMAND_SET_FULLSCREEN: {
                if (args.getBoolean(0)) {
                    view.dispatchEnterFullScreenClickEvent();
                } else {
                    view.dispatchExitFullScreenClickEvent();
                }
                return;
            }
            case COMMAND_PLAY_VIDEO:{
                playVideo(view,args.getBoolean(0));
                return;
            }
            case COMMAND_BIT_RATE: {
                view.setBitRate((int)args.getDouble(0));
                return;
            }
            case  COMMAND_STOP_PLAYBACK:{
                view.stopPlayback();
                return;
            }
        }
    }

    @Override
    public @Nullable Map <String,Object> getExportedCustomDirectEventTypeConstants() {
        Map<String, Object> map = new HashMap<>();
        map.put(EVENT_READY, (Object) MapBuilder.of("registrationName", "onReady"));
        map.put(EVENT_METADATA_LOADED, (Object) MapBuilder.of("registrationName", "onMetadataLoaded"));
        map.put(EVENT_PLAY, (Object) MapBuilder.of("registrationName", "onPlay"));
        map.put(EVENT_PAUSE, (Object) MapBuilder.of("registrationName", "onPause"));
        map.put(EVENT_END, (Object) MapBuilder.of("registrationName", "onEnd"));
        map.put(EVENT_PROGRESS, (Object) MapBuilder.of("registrationName", "onProgress"));
        map.put(EVENT_CHANGE_DURATION, (Object) MapBuilder.of("registrationName", "onChangeDuration"));
        map.put(EVENT_UPDATE_BUFFER_PROGRESS, (Object) MapBuilder.of("registrationName", "onUpdateBufferProgress"));
        map.put(EVENT_BUFFERING_STARTED, (Object) MapBuilder.of("registrationName", "onBufferingStarted"));
        map.put(EVENT_BUFFERING_COMPLETED, (Object) MapBuilder.of("registrationName", "onBufferingCompleted"));
        map.put(EVENT_BEFORE_ENTER_FULLSCREEN, (Object) MapBuilder.of("registrationName", "onBeforeEnterFullscreen"));
        map.put(EVENT_BEFORE_EXIT_FULLSCREEN, (Object) MapBuilder.of("registrationName", "onBeforeExitFullscreen"));
        map.put(EVENT_ENTER_FULLSCREEN, (Object) MapBuilder.of("registrationName", "onEnterFullscreen"));
        map.put(EVENT_EXIT_FULLSCREEN, (Object) MapBuilder.of("registrationName", "onExitFullscreen"));
        map.put(EVENT_ERROR, (Object) MapBuilder.of("registrationName", "onError"));
        map.put(EVENT_NETWORK_CONNECTIVITY_CHANGED, (Object) MapBuilder.of("registrationName", "onNetworkConnectivityChange"));
        return map;
    }
}
