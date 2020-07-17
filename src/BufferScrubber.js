import React from 'react' // eslint-disable-line
import PropTypes from 'prop-types'
import {
    View,
    Platform,
    StyleSheet
} from 'react-native'
import Slider from '@react-native-community/slider'
const BufferThumbTracker = require('../Resources/hidden_controller.png')

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        position:'absolute',
        start: 0,
        end: 0
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
    },
})

const BufferScrubber = (props) => {
    const trackColor = 'rgba(255,255,255,0.5)'
    const bufferedTrackColor = 'rgba(255,255,255,1)'
    const { bufferProgress, fullScreen } = props
    return (
        <View style={styles.container} pointerEvents="none">
            <Slider 
                value={bufferProgress === Number.POSITIVE_INFINITY ? 0 : bufferProgress}
                minimumTrackTintColor={bufferedTrackColor}
                maximumTrackTintColor={trackColor}
                //disabled
                {...Platform.select({
                    ios:{
                        thumbImage: BufferThumbTracker,
                        trackStyle: styles.trackStyle 
                    }, 
                    android:{
                        style: fullScreen ? styles.sliderFullScreen : styles.slider,
                        thumbTintColor: "#FFFFFF00"
                    }
                })}
            />
        </View>
    )
}

BufferScrubber.propTypes = {
    bufferProgress: PropTypes.number.isRequired,
    theme: PropTypes.object.isRequired
}

export { BufferScrubber }