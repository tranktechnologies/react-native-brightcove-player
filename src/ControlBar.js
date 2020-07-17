import React from 'react'
import PropTypes from 'prop-types'
import { StyleSheet, View } from 'react-native'
import {Time } from './Time'
import {Scrubber} from './Scrubber'
import {GoLive} from "./GoLive";

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignSelf: 'stretch',
        justifyContent: 'flex-end'
    },
    timerSpace: {
        display: 'flex',
        flexDirection: 'row',
        flex: 1,
        justifyContent: 'space-between',
    },
    controlBarContainer: {
        marginHorizontal: 8
    }
})

const ControlBar = (props) => {
    const {
        onSeek,
        onSeekRelease,
        progress,
        currentTime,
        theme,
        isInLiveEdge,
        liveEdge,
        duration
    } = props

    return (
        <View style={styles.controlBarContainer}>
            <View style={styles.timerSpace}>
                <Time time={currentTime} theme={theme.seconds} />
                {duration > 0 && (liveEdge <= 0 || duration==liveEdge) && <Time time={duration} theme={theme.seconds} />}
                {duration <= 0 && liveEdge > 0 && <GoLive theme={theme.seconds} disabled={isInLiveEdge} seekToLive = {() => props.seekToLive()}/>}
            </View>
            <Scrubber
                onSeek={pos => onSeek(pos)}
                onSeekRelease={pos => onSeekRelease(pos)}
                progress={progress}
                theme={{scrubberThumb: theme.scrubberThumb, scrubberBar: theme.scrubberBar}}
                fullScreen = {props.fullScreen}
            />
        </View>
    )
}

ControlBar.propTypes = {
    onSeek: PropTypes.func.isRequired,
    onSeekRelease: PropTypes.func.isRequired,
    progress: PropTypes.number.isRequired,
    currentTime: PropTypes.number.isRequired,
    duration: PropTypes.number.isRequired,
    theme: PropTypes.object.isRequired,
    seekToLive: PropTypes.func.isRequired
}

export { ControlBar }
