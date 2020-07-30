import React from 'react' // eslint-disable-line
import PropTypes from 'prop-types'
import { View, StyleSheet } from 'react-native'
import Slider from "react-native-slider"

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        position: 'absolute',
        start: 0,
        end: 0
    },
    slider: {
        marginHorizontal: -5,
        height:20,
    },
    thumbStyle: {
        width: 12,
        height: 12
    },
    trackStyle: {
        borderRadius: 1,
        height: 3
    }
})

const BufferScrubber = (props) => {
    const trackColor = 'rgba(255,255,255,0.4)'
    const bufferedTrackColor = 'rgba(255,255,255,1)'
    const { bufferProgress } = props
    return (
        <View style={styles.container} pointerEvents="none">
            <Slider
                style= {styles.slider}
                value={bufferProgress === Number.POSITIVE_INFINITY ? 0 : bufferProgress}
                thumbTintColor="#00000000"
                thumbStyle={styles.thumbStyle}
                trackStyle={styles.trackStyle}
                minimumTrackTintColor={bufferedTrackColor}
                maximumTrackTintColor={trackColor}
                disabled
            />
        </View>
    )
}

BufferScrubber.propTypes = {
    bufferProgress: PropTypes.number.isRequired
}

export { BufferScrubber }