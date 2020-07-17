import React from 'react'
import PropTypes from 'prop-types'
import {View, StyleSheet, TouchableOpacity} from 'react-native'
import Icons from 'react-native-vector-icons/MaterialIcons'

const styles = StyleSheet.create({
    btnContainer: {
        color: 'red',
        zIndex: 1,
        position: 'absolute',
        width: '100%',
        display: 'flex',
        flexDirection: 'row',
        height: '100%',
        alignContent: 'stretch'
    },
    individualButton: {
        display: 'flex',
        flex: 1,
        flexBasis: 40
    },
    centerButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center'
    },
    rewindButton: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'flex-end'
    },
    forwardButton: {
        flexDirection: 'row',
        alignItems: 'center'
    },
    buttonContentTxt: {
        display: 'flex',
        fontWeight: 'bold',
        fontSize: 14,
        paddingTop: 3
    },
    colorGrey: {
        color:'grey',

    },
    colorWhite: {
        color: 'white'
    }
})

const ScreenButtons = (props) => {
    const onPress = () => {
        if (props.completed) {
            props.replay()
            props.forcePlay()

        } else {
            props.togglePlay()

        }

    }
    return (<TouchableOpacity style={styles.btnContainer} onPress={props.onOverlayClick()}>
        <View style={[styles.individualButton, styles.rewindButton]}>
            <TouchableOpacity onPress={() => props.showBackward && props.rewind()}>
                <Icons
                    name={'replay-10'}
                    color={props.showBackward ? props.theme.screenButtons : 'grey'}
                    size={28}
                />
            </TouchableOpacity>
        </View>
        <View style={[styles.individualButton, styles.centerButton]}>

            {!props.loading && <TouchableOpacity onPress={() => onPress()}><Icons
                name={props.completed ? 'replay' : props.paused ? 'play-arrow' : 'pause'}
                color={props.theme.screenButtons}
                size={36}
            /></TouchableOpacity>}
        </View>
        <View style={[styles.individualButton, styles.forwardButton]}>
            <TouchableOpacity onPress={() => props.showForward && props.forward()}>
                <Icons
                    name={'forward-10'}
                    color={props.showForward ? props.theme.screenButtons : 'grey'}
                    size={28}
                />
            </TouchableOpacity>
        </View>

    </TouchableOpacity>)
}

ScreenButtons.propTypes = {
    onPress: PropTypes.func,
    theme: PropTypes.object.isRequired
}

ScreenButtons.defaultProps = {
    onPress: undefined,
    theme: {
        screenButtons: '#fff'
    }
}

export {ScreenButtons}
