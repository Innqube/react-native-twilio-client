# react-native-twilio-client
This is a React Native wrapper for Twilio Programmable Voice SDK & CallKit & PushKit.
TwilioVideo compatible.

# Twilio Voice Framework
- iOS 2.0.4 (specified by the app's own podfile)
- Android not available

# Callkit
- Setup Apple CallKit and display incoming call. See [CallKit](https://developer.apple.com/reference/callkit)
- Event listeners:
  - "deviceReady" 
  - "connectionDidConnect"
  - "connectionDidDisconnect"
  - "callRejected"
  - "performAnswerVoiceCall"
  - "performAnswerVideoCall"
  - "performEndVideoCall"
  - "requestTransactionError"

# Pushkit
- Register & receive voip push notifications
- Push notification payloads containing "mode"="video" attribute will be processed as Video Calls
- Event listeners:
    - "voipRemoteNotificationsRegistered"
    - "voipRemoteNotificationReceived"

# Installation

Before starting, read this Twilio guide [Twilio Programmable Voice SDK](https://www.twilio.com/docs/api/voice-sdk).

```
npm install react-native-twilio-client --save
react-native link react-native-twilio-client
```

### iOS Installation - when projects made without react-native init
Edit your `Podfile` to include TwilioVoice and RNTwilioClient frameworks

```
target <YOUR_TARGET> do
    ...
    pod 'TwilioVoice', '~> 2.0.0'
    pod 'RNTwilioVoice', path: '../node_modules/react-native-twilio-client'
    ...
end
```

run `pod install` from inside your project `ios` directory

### VoIP Service Certificate

Twilio Programmable Voice for iOS utilizes Apple's VoIP Services and VoIP "Push Notifications". You will need a VoIP Service Certificate from Apple to receive calls.

## Usage

```javascript
import TwilioClient from 'react-native-twilio-client';

// Initialize the Programmable Voice SDK passing an access token obtained from the server.
// Listen to deviceReady and deviceNotReady events to see whether the initialization succeeded.
// Listen to voipRemoteNotificationsRegistered for TwilioVideo registration
async function initTwilioClient() {
    try {
        const accessToken = await getAccessTokenFromServer();
        const success = await TwilioClient.initWithToken(accessToken);
    } catch (err) {
        console.err(err);
    }
}

```

## Events

```javascript
// add listeners
TwilioVoice.addEventListener('deviceReady', () => {});

TwilioVoice.addEventListener('deviceNotReady', (data) => {
    // {
    //     err: string
    // }
});

TwilioVoice.addEventListener('connectionDidConnect', (data) => {
    // {
    //     call_sid: string,
    //     call_state: string,
    //     from: string,
    //     to: string
    // }
});

TwilioVoice.addEventListener('connectionDidDisconnect', (data) => {
    //     {
    //         call_sid: string,
    //         call_state,
    //         call_from?: string, 
    //         call_to?: string,
    //         from?: string,
    //         to?: string,
    //         error?: string
    //     }
});

TwilioVoice.addEventListener('callRejected', (value) => {});

TwilioVoice.addEventListener('voipRemoteNotificationsRegistered', (deviceToken) => {
    // Use this device token for TwilioVideo identity register
});

TwilioVoice.addEventListener('voipRemoteNotificationReceived', (notification) => {
    // Show incoming video call
    // TwilioClient.displayIncomingCall(
    //          notification.uuid, 
    //          notification.handle, 
    //          notification.handleType, 
    //          notification.hasVideo, 
    //          notification.localizedCallerName
    // );
});

TwilioVoice.addEventListener('requestTransactionError', (error) => {});

TwilioVoice.addEventListener('performAnswerVoiceCall', (callUuid) => {});

TwilioVoice.addEventListener('performAnswerVideoCall', (callUuid) => {});

TwilioVoice.addEventListener('performEndVideoCall', (callUuid) => {});

TwilioVoice.connect({To: '+549111222333'});

TwilioVoice.disconnect();

TwilioVoice.setMuted(mutedValue)

TwilioVoice.sendDigits(digits)

TwilioVoice.getActiveCall()
    .then(incomingCall => {
        // handle incoming call
    });

// Retrieve TwilioVoice notification payload
TwilioVoice.getDictionaryPayload()
    .then(
        (dictionaryPayload) => {
            // {
            //      twi_to: string,
            //      twi_from: string,
            //      twi_message_type: string,
            //      twi_message_id: string,
            //      twi_call_sid: string,
            //      twi_account_sid: string,
            //      twi_bridge_token: string,
            //      twi_bridge_token: string,
            // }
        }
    );

// Display incoming call (IOS Callkit)
TwilioVoice.displayIncomingCall(uuid, handle, handleType = 'number', hasVideo = false, localizedCallername);

```

## Twilio Voice SDK reference

[iOS changelog](https://www.twilio.com/docs/api/voice-sdk/ios/changelog)

## License

MIT
