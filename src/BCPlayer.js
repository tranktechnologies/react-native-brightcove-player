import React, {Component} from 'react'
import {
    Animated,
    AppState,
    AppStateStatus,
    BackHandler,
    Dimensions,
    Platform,
    StatusBar,
    StyleSheet,
    View,
    TouchableOpacity,
    SafeAreaView,
    ActivityIndicator
} from 'react-native'
import BrightcovePlayer from './BrightcovePlayer'
import Orientation from 'react-native-orientation'
import withEvents from './Events'
import {ControlBar} from "./ControlBar"
import {ScreenButtons} from "./screenButtons"
import {QualityOverlayButtons} from "./qualityOverlayButtons"
import {PlaybackSpeed} from "./playbackSpeed"
import {PlaybackSpeedButtons} from "./playbackSpeedButtons"
import {FadeInAnim, FadeOutAnim} from "./fade-anim";
import {ToggleIcon} from "./ToggleIcon";
import {QualityControl} from "./qualityControl";
import PlayerEventTypes from "./PlayerEventTypes";

const FORWARD_CONTROL = 5;
const qualityContent = ['Auto', 'High', 'Medium', 'Data Saver'];
const quality = [0, 750000, 500000, 120000];
const playbackSpeedContent = ['0.5x', 'Normal', '1.5x', '1.75x', '2x'];
const playbackSpeed = [0.5, 1, 1.5, 1.75, 2];

// Wraps the Brightcove player with special Events
const BrightcovePlayerWithEvents = withEvents(BrightcovePlayer)

const Win = Dimensions.get('window')
const backgroundColor = '#000'

const styles = StyleSheet.create({
    background: {
        backgroundColor,
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 98
    },
    fullScreen: {
        ...StyleSheet.absoluteFillObject
    },
    image: {
        ...StyleSheet.absoluteFillObject,
        width: undefined,
        height: undefined,
        zIndex: 99
    },
    topMenu: {
        zIndex: 100,
        position: 'absolute',
        width: '100%',
        height: '100%',
        backgroundColor: '#00000080'
    },
    topSubMenu : {
        display: 'flex',
        flexDirection: 'row-reverse',
        zIndex:1000
    },
    bottomBar: {
        zIndex: 1900,
        position: 'absolute',
        width: '100%',

    },
    loader: {
        zIndex: 99,
        position: 'absolute',
        width: '100%',
        height: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
    },
    loaderContainer: {
        width: 40,
        height: 40,
    },
    controlsVisibility: {
        zIndex: 10000,
        position: 'absolute',
        width: '100%',
        height: '100%'
    }
})

class BCPlayer extends Component {
    constructor(props) {
        super(props)
        this.state = {
            paused: !props.autoPlay,
            fullScreen: false,
            inlineHeight: Win.width * 0.5625,
            percentageTracked: {Q1: false, Q2: false, Q3: false, Q4: false},
            mediainfo: null,
            onRotate: false,
            appState: AppState.currentState,
            muted: false,
            loading: false,
            duration: 0,
            progress: 0,
            bufferProgress: 0,
            currentTime: 0,
            seeking: false,
            renderError: false,
            qualityControlMenu: false,
            playbackSpeedMenu: false,
            bitRate: 0,
            playbackRate: 1,
            showControls: true,
            showClickOverlay: false,
            seconds: 0,
            controlsOverlayClicked: false,
            isInLiveEdge: true,
            liveEdge: 0,
            selectedQualityIndex: 0,
            selectedPlaybackIndex: 1,
            completed: false,
        }
        this.quality = props.quality || quality;
        this.animInline = new Animated.Value(Win.width * 0.5625)
        this.animFullscreen = new Animated.Value(Win.width * 0.5625)
        this.BackHandler = this.BackHandler.bind(this)
        this._handleAppStateChange = this._handleAppStateChange.bind(this)
        this.onAnimEnd = this.onAnimEnd.bind(this)
        this.onRotated = this.onRotated.bind(this)
    }

    // componentWillMount() {
        // The getOrientation method is async. It happens sometimes that
        // you need the orientation at the moment the JS runtime starts running on device.
        // `getInitialOrientation` returns directly because its a constant set at the
        // beginning of the JS runtime.
        // Remember to remove listener
    // }

    componentDidMount() {
        this.setState({paused: false})
        Dimensions.addEventListener('change', this.onRotated)
        BackHandler.addEventListener('hardwareBackPress', this.BackHandler)
        AppState.addEventListener('change', this._handleAppStateChange)
        this.setTimer()
    }

    setTimer() {
        this.timer = setInterval(() => {
            switch (true) {
                case this.state.seeking:
                    // do nothing
                    break
                case this.state.controlsOverlayClicked:
                    this.setState({seconds: 0, controlsOverlayClicked: false})
                    break
                case this.state.showClickOverlay:
                    break
                case this.state.seconds > 3:
                    this.setState({
                        showControls: false,
                        seconds: 0
                    })
                    break
                default:
                    this.setState({seconds: this.state.seconds + 1})
            }
        }, 1000)
    }

    _handleAppStateChange(nextAppState: AppStateStatus) {
        if (
            this.state.appState.match(/inactive|background/) &&
            nextAppState === 'active') {
            this.player && this.player.playVideo(true)
            if (this.state.fullScreen) {
                this.player && this.player.setFullscreen(false)
            }
        }
        this.setState({appState: nextAppState})
    }

    componentWillUnmount() {
        BackHandler.removeEventListener('hardwareBackPress', this.BackHandler)
        Dimensions.removeEventListener('change', this.onRotated)
        AppState.removeEventListener('change', this._handleAppStateChange)
        clearInterval(this.timer)
    }


    onRotated({ window: { width, height } }) {
        // Add this condition incase if inline and fullscreen options are turned on
        if (this.props.inlineOnly) return
        const orientation = width > height ? 'LANDSCAPE' : 'PORTRAIT'
        if (this.props.rotateToFullScreen) {
            if (orientation === 'LANDSCAPE') {
                this.setState({ fullScreen: true }, () => {
                    this.animToFullscreen(height)
                    this.props.onFullScreen(this.state.fullScreen)
                })
                return
            }
            if (orientation === 'PORTRAIT') {
                this.setState({
                    fullScreen: false,
                    paused: this.props.fullScreenOnly || this.state.paused
                }, () => {
                    this.animToInline()
                    if (this.props.fullScreenOnly) this.props.onPlay(!this.state.paused)
                    this.props.onFullScreen(this.state.fullScreen)
                })
                return
            }
        } else {
            this.animToInline()
        }
        if (this.state.fullScreen) this.animToFullscreen(height)
    }

    BackHandler() {
        if (this.state.fullScreen) {
            this.toggleFS()
            return true
        }
        return false
    }

    seekToLive() {
        this.setState({currentTime : this.state.liveEdge}, () => {
            this.progress({currentTime: this.state.currentTime, liveEdge: this.state.liveEdge});
            this.player && this.player.seekToLive()
            this.forcePlay()
            this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.SEEK_TO_LIVE})
        })

    }

    toggleFS() {
        this.setState({fullScreen: !this.state.fullScreen}, () => {
            if (this.state.fullScreen) {
                const initialOrient = Orientation.getInitialOrientation()
                const height = this.state.onRotate ? Dimensions.get('window').height : Dimensions.get('window').width
                this.props.onFullScreen && this.props.onFullScreen(this.state.fullScreen)
                if (!this.state.onRotate) Orientation.lockToLandscape()
                this.animToFullscreen(height)
            } else {
                if (this.props.fullScreenOnly) {
                    this.setState({paused: true}, () => this.props.onPlay(!this.state.paused))
                }
                this.props.onFullScreen && this.props.onFullScreen(this.state.fullScreen)
                if (!this.state.onRotate) Orientation.lockToPortrait()
                this.animToInline()
                setTimeout(() => {
                    if (!this.props.lockPortraitOnFsExit) Orientation.unlockAllOrientations()
                }, 1500)
            }
            this.setState({onRotate: false})
        })
    }

    animToFullscreen(height) {
        Animated.parallel([
            Animated.timing(this.animFullscreen, {toValue: height, duration: 200, useNativeDriver: false}),
            Animated.timing(this.animInline, {toValue: height, duration: 200, useNativeDriver: false})
        ]).start()
    }

    animToInline(height) {
        const newHeight = height || this.state.inlineHeight
        Animated.parallel([
            Animated.timing(this.animFullscreen, {toValue: newHeight, duration: 100, useNativeDriver: false}),
            Animated.timing(this.animInline, {toValue: this.state.inlineHeight, duration: 100, useNativeDriver: false})
        ]).start()
    }

    setDuration(duration) {
        this.setState({duration: duration.duration})
    }

    toggleMute() {
        this.setState({
            muted: !this.state.muted,
            controlsOverlayClicked: true
        })
    }

    seek(percent) {
        const currentTime = percent * this.state.duration
        this.setState({
            seeking: true,
            currentTime,
            controlsOverlayClicked: true
        })
    }

    seekTo(seconds) {
        const percent = seconds / this.state.duration
        if (seconds > this.state.duration) {
            // throw new Error(`Current time (${seconds}) exceeded the duration ${this.state.duration}`)
            return
        }
        return this.onSeekRelease(percent)
    }

    onSeekRelease(percent) {
        const seconds = this.state.duration > 0 ? percent * this.state.duration : percent * this.state.liveEdge
        this.setState({progress: percent, completed: false, seeking: false, currentTime: (this.state.liveEdge < 1) && (this.state.duration <= seconds) ? this.state.duration - .01 :  seconds}, () => {

            this.player && this.player.seekTo((this.state.liveEdge < 1) && (this.state.duration <= seconds) ? this.state.duration - 0.01 :  seconds)
            this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.SEEK_TO, time: seconds})
            this.forcePlay()
        })
    }

    progress(time) {
        const {currentTime, duration, isInLiveEdge, liveEdge} = time
        const progress = currentTime / (duration > 0 ? duration : liveEdge )
        if (!this.state.seeking) {
            this.setState({progress, currentTime, isInLiveEdge, liveEdge})
        }
    }

    updateBufferProgress(time){
        if(!this.state.seeking){
            this.setState({bufferProgress:time.bufferProgress})
        }
    }

    onBufferingCompleted(){
        this.setState({loading: false});
        this.forcePlay();
    }

    toggleQualityOverlay() {
        this.setState({
            qualityControlMenu: !this.state.qualityControlMenu,
            controlsOverlayClicked: true
        })
    }

    togglePlaybackOverlay() {
        this.setState({
            playbackSpeedMenu: !this.state.playbackSpeedMenu,
            controlsOverlayClicked: true
        })
    }


    toggleQuality(value) {

        this.setState({
            qualityControlMenu: !this.state.qualityControlMenu,
            controlsOverlayClicked: true,
            bitRate: (value >= 0 && value !== null) ? this.quality[value] : this.state.bitRate,
            selectedQualityIndex: (value >= 0 && value !== null) ? value : this.state.selectedQualityIndex
        }, () => {
            this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.QUALITY_SELECTED, bitRate : qualityContent[value]})
        })
    }

    togglePlaybackSpeed(value) {

        this.setState({
            playbackSpeedMenu: !this.state.playbackSpeedMenu,
            controlsOverlayClicked: true,
            playbackRate: (value >= 0 && value !== null) ? playbackSpeed[value] : this.state.playbackRate,
            selectedPlaybackIndex: (value >= 0 && value !== null) ? value : this.state.selectedPlaybackIndex
        }, () => {
            this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.PLAYBACK_RATE_CHANGED, playbackRate : playbackSpeedContent[value]})
        })
    }

    togglePlay() {
        this.setState({
            paused: !this.state.paused,
            controlsOverlayClicked: true
        }, () => {
            this.player && this.player.playVideo(!this.state.paused)
            if(this.state.paused)
                this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.PAUSE})
        })
    }

    forcePlay() {
        this.setState({
            paused: false,
            controlsOverlayClicked: true
        }, () => {
            this.player && this.player.playVideo(true)
        })
    }

    onPlay() {
        this.setState({
            paused: false,
            completed: false,
        })
    }

    // openAirplay() {
    //     this.player && this.player.createAirplayIconOverlay();
    // }

    forward() {
        this.setState({
            controlsOverlayClicked: true,
            currentTime: (Math.floor(this.state.currentTime) <= Math.floor(this.state.liveEdge) || Math.floor(this.state.currentTime) <= Math.floor(this.state.duration))  ? (this.state.currentTime + FORWARD_CONTROL) :  this.state.currentTime
        }, () => {
            this.player && this.player.seekTo(this.state.currentTime + FORWARD_CONTROL)
            this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.FORWARD})
        })
    }

    rewind() {
        if(this.state.completed)
            this.forcePlay()
        this.setState({
            controlsOverlayClicked: true,
            currentTime : ((this.state.currentTime - FORWARD_CONTROL) >= 0 ) ? this.state.currentTime - FORWARD_CONTROL : this.state.currentTime,
            completed: false
        }, () => {
            if ((this.state.currentTime - FORWARD_CONTROL) >= 0 ){
                this.player && this.player.seekTo(this.state.currentTime - FORWARD_CONTROL)
                this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.REWIND})
            }
        })
    }

    onAnimEnd() {
        this.setState({showClickOverlay: !this.state.showControls})
    }
    replay() {
        this.player && this.player.seekTo(0);
        this.player && this.player.playVideo(true)
        this.setState( {currentTime: 0, paused: false, completed: false},  () => {
            this.progress({currentTime : this.state.currentTime, duration : this.state.duration})
            this.props.onEvent && this.props.onEvent({'type': PlayerEventTypes.REPLAY})
        })
    }

    overlayClick() {
        this.setState({showControls: false})
    }

    render() {


        const theme = {
            title: '#fff',
            fullscreen: '#fff',
            scrubberThumb: '#ff5000',
            scrubberBar: '#ff5000',
            seconds: '#fff',
            duration: '#ff5000',
            progress: '#ff5000',
            loading: '#ff5000',
            screenButtons: '#fff',
            qualityControl: '#fff'
        }
        const {
            fullScreen,
            paused,
            bitRate,
            playbackRate,
            progress,
            bufferProgress,
            duration,
            currentTime,
            qualityControlMenu,
            playbackSpeedMenu,
            loading,
            showControls,
            showClickOverlay,
            selectedQualityIndex,
            selectedPlaybackIndex,
            isInLiveEdge,
            muted,
            completed,
            liveEdge
        } = this.state

        const {
            style,
            hideQualityControl
        } = this.props
        const AnimView = showControls ? FadeInAnim : FadeOutAnim
        return (
            <View>
                {loading && <View style={styles.loader}><View style={{position:'absolute', left: '45%', top: '43%', zIndex: 4000}}><ActivityIndicator size="large" color={theme.loading} /></View></View>}
                <AnimView style={styles.topMenu}
                          onEnd={this.onAnimEnd}
                          onOverlayClick={() => this.setState({controlsOverlayClicked: !this.state.controlsOverlayClicked})}>
                    <SafeAreaView style={styles.topSubMenu}>
                        <ToggleIcon
                            onPress={() => this.toggleFS()}
                            iconOff="fullscreen"
                            iconOn="fullscreen-exit"
                            isOn={fullScreen}
                            theme={theme.fullscreen}
                            size={35}
                        />

                        <PlaybackSpeed
                            theme={theme.qualityControl}
                            togglePlaybackSpeed={() => this.togglePlaybackOverlay()}
                            paddingRight={10}
                            selectedOption={selectedPlaybackIndex}
                        />
                        
                        {!hideQualityControl &&
                        <QualityControl
                            theme={theme.qualityControl}
                            toggleQuality={() => this.toggleQualityOverlay()}
                            paddingRight={10}
                            selectedOption={selectedQualityIndex}
                        />
                        }
                    </SafeAreaView>
                    {playbackSpeedMenu &&
                    <PlaybackSpeedButtons onPress={(value) => this.togglePlaybackSpeed.bind(this, value)}
                                           playbackSpeedContent={playbackSpeedContent} selectedPlaybackIndex={selectedPlaybackIndex}/>
                    }
                    {!hideQualityControl && qualityControlMenu &&
                    <QualityOverlayButtons onPress={(value) => this.toggleQuality.bind(this, value)}
                                           qualityContent={qualityContent} selectedQualityIndex={selectedQualityIndex}/>
                    }
                    <ScreenButtons togglePlay={this.togglePlay.bind(this)}
                                   forcePlay={this.forcePlay.bind(this)}
                                   loading={loading} paused={paused}
                                   forward={this.forward.bind(this)}
                                   rewind={this.rewind.bind(this)}
                                   theme={theme}
                                   paused={paused}
                                   completed={completed}
                                   replay={this.replay.bind(this)}
                                   onOverlayClick={() => this.overlayClick.bind(this)}
                                   showForward={liveEdge > 0 ? (liveEdge - currentTime) > 10   : (duration - currentTime) > 10}
                                   showBackward={currentTime > 10}
                    />

                    {<View style={[styles.bottomBar, (Platform.OS === 'ios' && this.state.fullScreen) ? {bottom : 15} : {bottom: 0}]}>
                        <ControlBar
                            onSeek={pos => this.seek(pos)}
                            onSeekRelease={pos => this.onSeekRelease(pos)}
                            progress={progress}
                            bufferProgress={bufferProgress}
                            currentTime={currentTime}
                            theme={theme}
                            duration={duration.duration || duration}
                            isInLiveEdge={isInLiveEdge}
                            seekToLive={() => this.seekToLive()}
                            liveEdge={liveEdge}
                        />
                    </View>}
                </AnimView>
                {showClickOverlay &&
                <TouchableOpacity style={styles.controlsVisibility}
                                  onPress={() => this.setState({showControls: true})}/>}
                <Animated.View
                    style={[
                        styles.background,
                        fullScreen ?
                            (styles.fullScreen, {height: this.animFullscreen})
                            : {height: this.animInline},
                        fullScreen ? null : style
                    ]}
                >
                    <StatusBar hidden={fullScreen}/>
                    <BrightcovePlayerWithEvents
                        ref={(player) => this.player = player}
                        {...this.props}
                        style={[styles.player, this.props.style]}
                        playerId={this.props.playerId ? this.props.playerId : `com.brightcove/react-native/${Platform.OS}`}
                        onBeforeEnterFullscreen={this.toggleFS.bind(this)}
                        onBeforeExitFullscreen={this.toggleFS.bind(this)}
                        disableDefaultControl={true}
                        onChangeDuration={(duration) => this.setDuration(duration)}
                        onProgress={e => this.progress(e)}
                        volume={muted ? 0 : 10}
                        bitRate={bitRate}
                        playbackRate={playbackRate}
                        onBufferingStarted={() => this.setState({loading: true})}
                        onBufferingCompleted={()=>this.onBufferingCompleted()}
                        onUpdateBufferProgress={e => this.updateBufferProgress(e)}
                        onPlay={()=> this.onPlay()}
                        onEnd={() => this.setState({completed : true})}
                    />
                </Animated.View>
            </View>
        )
    }

}

BCPlayer.defaultProps = {
    placeholder: undefined,
    style: {},
    autoPlay: false,
    inlineOnly: false,
    fullScreenOnly: false,
    rotateToFullScreen: false,
    lockPortraitOnFsExit: false,
    disableControls: false
}


module.exports = BCPlayer
