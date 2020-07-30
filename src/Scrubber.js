import React from 'react' // eslint-disable-line
import PropTypes from 'prop-types'
import { View, StyleSheet } from 'react-native'
import Slider from "react-native-slider"

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
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

const Scrubber = (props) => {
    const trackColor = 'rgba(255,255,255,0.4)'
    const { progress, theme, onSeek, onSeekRelease } = props
    return (
        <View style={styles.container}>
            <Slider
                style= {styles.slider}
                onValueChange={val => onSeek(val)}
                onSlidingComplete={val => onSeekRelease(val)}
                value={progress === Number.POSITIVE_INFINITY ? 0 : progress}
                thumbTintColor={theme.scrubberThumb}
                thumbStyle={styles.thumbStyle}
                trackStyle={styles.trackStyle}
                minimumTrackTintColor={theme.scrubberBar}
                maximumTrackTintColor={trackColor}
                trackClickable
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
