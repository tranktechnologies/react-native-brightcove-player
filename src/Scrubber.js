import React from 'react' // eslint-disable-line
import PropTypes from 'prop-types'
import {
    View,
    Platform,
    StyleSheet
} from 'react-native'
import Slider from '@react-native-community/slider'
const ThumbTracker = require('../Resources/controller.png')

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center'
    },
    slider: {
        marginHorizontal: -15,
    },
    trackStyle: {
        borderRadius: 1
    },
    sliderFullScreen: {
        marginHorizontal: -15,
        marginBottom:0
    }
})

const Scrubber = (props) => {
    const trackColor = 'rgba(255,255,255,0.5)'
    const { progress, theme, onSeek, onSeekRelease } = props
    return (
        <View style={styles.container}>
            <Slider
                onValueChange={val => onSeek(val)}
                onSlidingComplete={val => onSeekRelease(val)}
                value={progress === Number.POSITIVE_INFINITY ? 0 : progress}
                minimumTrackTintColor={theme.scrubberBar}
                maximumTrackTintColor={trackColor}
                {...Platform.select({
                    ios:{
                        thumbImage: ThumbTracker,
                        trackClickable: true,
                        trackStyle: styles.trackStyle
                    },
                    android:{
                        style: props.fullScreen ? styles.sliderFullScreen : styles.slider,
                        thumbTintColor: "#e8e8e8"
                    }
                })}
            />
        </View>
    )
}

Scrubber.propTypes = {
    onSeek: PropTypes.func.isRequired,
    onSeekRelease: PropTypes.func.isRequired,
    progress: PropTypes.number.isRequired,
    theme: PropTypes.object.isRequired
}

export { Scrubber }
