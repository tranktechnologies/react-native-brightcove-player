import React from 'react'
import PropTypes from 'prop-types'
import { View, StyleSheet, TouchableOpacity, Image, Text } from 'react-native'
const settings = require('../Resources/settings_icon.png')

const backgroundColor = 'transparent'

const styles = StyleSheet.create({
    btnContainer: {
        alignItems: 'center',
        backgroundColor,
        justifyContent: 'center',
        zIndex: 101
    },
    textStyle : {
        backgroundColor: '#ff5000',
        color: 'white',
        position: 'absolute',
        zIndex: 102,
        borderRadius:2,
        left: 15,
        paddingHorizontal:2,
        paddingBottom: 1
    }
})

const QualityControl = (props) => {
    const {
        paddingLeft,
        paddingRight,
        theme,
        size
    } = props

    const padding = {
        height: 23,
        paddingLeft: paddingLeft,
        paddingRight: paddingRight,
        width: 23,
        marginRight:17
    }
    const contolArray = ['Auto', 'High', 'Med', 'Data']
    return (
        <View style={styles.btnContainer}>

            <TouchableOpacity
                onPress={() => props.toggleQuality()}
            >
                <View style={styles.textStyle}><Text style={{color:'white', fontSize: 9}}>{contolArray[props.selectedOption]}</Text></View>
                <Image
                    style={padding}
                    source={settings}
                />
            </TouchableOpacity>
        </View>
    )
}

QualityControl.propTypes = {
    onPress: PropTypes.func,
    theme: PropTypes.string.isRequired,
    size: PropTypes.number,
    paddingRight: PropTypes.number,
    paddingLeft: PropTypes.number
}

QualityControl.defaultProps = {
    onPress: undefined,
    isOn: false,
    size: 25,
    paddingRight: 0,
    paddingLeft: 0
}

export { QualityControl }
