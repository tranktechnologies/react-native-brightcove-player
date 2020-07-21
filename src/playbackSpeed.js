import React from 'react'
import PropTypes from 'prop-types'
import { View, StyleSheet, TouchableOpacity, Text } from 'react-native'

const backgroundColor = 'transparent'

const styles = StyleSheet.create({
    btnContainer: {
        alignItems: 'center',
        backgroundColor,
        justifyContent: 'center',
        zIndex: 101
    },
})

const PlaybackSpeed = (props) => {
    const {
        paddingLeft,
        paddingRight,
    } = props

    const padding = {
        height: 23,
        paddingLeft: paddingLeft,
        paddingRight: paddingRight,
        width: 56,
        alignItems:'center'
    }
    const contolArray = ['0.5x', '1x', '1.5x', '1.75x', '2x']
    return (
        <View style={styles.btnContainer}>

            <TouchableOpacity
            style={padding}
                onPress={() => props.togglePlaybackSpeed()}
            >
                <Text style={{color:'white', fontSize: 18, }}>{contolArray[props.selectedOption]}</Text>
                 
            </TouchableOpacity>
        </View>
    )
}

PlaybackSpeed.propTypes = {
    paddingRight: PropTypes.number,
    paddingLeft: PropTypes.number
}

PlaybackSpeed.defaultProps = {
    paddingRight: 0,
    paddingLeft: 0
}

export { PlaybackSpeed }
