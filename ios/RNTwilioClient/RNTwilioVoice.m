//
//  RNTwilioVoice.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard.
//  Copyright © 2018 No Good Software Inc. All rights reserved.
//


#import "RNTwilioVoice.h"
#import <React/RCTLog.h>
#import <PushKit/PushKit.h>
#import <CallKit/CallKit.h>
#import "RNEventEmitterHelper.h"
#import "RNNetworkMonitor.h"
#import "RNTwilioMos.h"
#import "RNLocalizedStrings.h"

@import AVFoundation;
@import PushKit;
@import CallKit;
@import TwilioVoice;
@import UIKit;
@import Foundation;
@import SystemConfiguration;

@interface RNTwilioVoice () <PKPushRegistryDelegate, CXProviderDelegate, TVOCallDelegate>
@property(nonatomic, strong) NSString *deviceTokenString;
@property(nonatomic, strong) NSData *deviceToken;
@property(nonatomic, strong) NSDictionary *dictionaryPayload;
@property(nonatomic, strong) NSString *type;
@property(nonatomic, strong) NSString *callInvite;
@property(nonatomic, strong) TVOCall *call;
@property(nonatomic, strong) NSUUID *callUuid;
@property(nonatomic, strong) NSString *callMode;
@property(nonatomic, strong) NSMutableDictionary *callParams;
@property(nonatomic, strong) PKPushRegistry *voipRegistry;
@property(nonatomic, strong) void (^callKitCompletionCallback)(BOOL);
@property(nonatomic, strong) CXProvider *callKitProvider;
@property(nonatomic, strong) CXCallController *callKitCallController;
@property(nonatomic, strong) TVODefaultAudioDevice *audioDevice;
@property(nonatomic, strong) void(^incomingPushCompletionCallback)(void);
@property(nonatomic, strong) CXStartCallAction *action;
@property(nonatomic, strong) RNNetworkMonitor *monitor;
@property(nonatomic, strong) RNTwilioMos *twilioMos;

@end

@implementation RNTwilioVoice {
    NSMutableDictionary *_settings;
    NSMutableDictionary *_callParams;
    NSArray *_iceServers;
    NSString *_token;
}

RCT_EXPORT_MODULE()

static RNTwilioVoice *sharedInstance = nil;
NSString *const StatePending = @"PENDING";
NSString *const StateConnecting = @"CONNECTING";
NSString *const StateConnected = @"CONNECTED";
NSString *const StateDisconnected = @"DISCONNECTED";
NSString *const StateRejected = @"REJECTED";

+ (id)sharedInstance {
    if (sharedInstance == nil) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            sharedInstance = [self alloc];
        });
        [sharedInstance configureCallKit];
        [sharedInstance configureAudioDevice];
    }
    return sharedInstance;
}

- (id)init {
    // [TwilioVoice setLogLevel:TVOLogLevelAll];
    self.monitor = [[RNNetworkMonitor alloc] init];
    return [RNTwilioVoice sharedInstance];
}

- (void)dealloc {
    if (self.callKitProvider) {
        [self.callKitProvider invalidate];
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(sendMessage:(NSString *) message) {
    if (message != nil) {
        NSLog(@"[IIMobile - RNTwilioVoice][Message from ReactNative] %@", message);
    } else {
        NSLog(@"[IIMobile - RNTwilioVoice][Message from ReactNative] NULL");
    }
}

RCT_EXPORT_METHOD(connect: (NSDictionary *)params nts: (NSArray *)iceServers andToken: (NSString *) token) {
    NSLog(@"[IIMobile - RNTwilioVoice][connect] Calling phone number %@", [params valueForKey:@"To"]);
    NSLog(@"[IIMobile - RNTwilioVoice][connect] edge: %@", TwilioVoiceSDK.edge);
    _token = token;
    UIDevice *device = [UIDevice currentDevice];
    device.proximityMonitoringEnabled = YES;

    [self.monitor startNetworkMonitoring];

    if (self.call && self.call.state == TVOCallStateConnected) {
        [self.call disconnect];
    } else {
        NSString *handle = [params valueForKey:@"To"];
        _callParams = [[NSMutableDictionary alloc] initWithDictionary:params];
        _iceServers = iceServers;

        if (handle == nil) {
            __weak typeof(self) weakSelf = self;
            [self performVoiceCallWithUUID:self.callUuid client:nil completion:^(BOOL success) {
                __strong typeof(self) strongSelf = weakSelf;
                if (success) {
                    [self.action fulfill];
                } else {
                    [self.action fail];
                }
            }];
        } else {
            [self performStartCallActionWithUUID:[NSUUID UUID] handle:handle andToken:token];
        }
    }
}

RCT_EXPORT_METHOD(disconnect:(NSString *) uuidStr) {
    NSUUID *remoteUuid = [[NSUUID alloc] initWithUUIDString:uuidStr];
    NSUUID *uuid = remoteUuid != nil ? remoteUuid : self.call != nil ? self.call.uuid : self.callUuid;
    NSLog(@"[IIMobile - RNTwilioVoice][disconnect] Disconnecting call with UUID: %@", [uuid UUIDString]);
    [self.monitor stopNetworkMonitoring];
    [self performEndCallActionWithUUID:(uuid)];
}

RCT_EXPORT_METHOD(setMuted:(BOOL *) muted) {
    NSLog(@"[IIMobile - RNTwilioVoice] setMuted");
    self.call.muted = (BOOL) muted;
}

RCT_EXPORT_METHOD(setSpeakerPhone:(BOOL *) speaker) {
    NSLog(@"[IIMobile - RNTwilioVoice] setSpeakerPhone");
    [self toggleAudioRoute:speaker];
}

RCT_EXPORT_METHOD(sendDigits: (NSString *) digits) {
    if (self.call && self.call.state == TVOCallStateConnected) {
        NSLog(@"[IIMobile - RNTwilioVoice][sendDigits] %@", digits);
        [self.call sendDigits:digits];
    }
}

RCT_REMAP_METHOD(getDeviceToken, tokenResolver: (RCTPromiseResolveBlock)resolve rej:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioVoice][getDeviceToken] %@", self.deviceTokenString);

    #if TARGET_IPHONE_SIMULATOR
        resolve(@"iphone-simulator-token");
    #else
        if (self.deviceTokenString != nil) {
            resolve(self.deviceTokenString);
        } else {
            reject(@"error", @"Device token is null", nil);
        }
    #endif
}

RCT_REMAP_METHOD(getActiveCall, resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock) reject) {
    NSLog(@"[IIMobile - RNTwilioVoice][getActiveCall]");
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];

    if (self.call) {
        if (self.call.sid) {
            params[@"call_sid"] = self.call.sid;
        }
        if (self.call.to) {
            params[@"call_to"] = self.call.to;
        }
        if (self.call.from) {
            params[@"call_from"] = self.call.from;
        }
        if (self.call.state == TVOCallStateConnected) {
            params[@"call_state"] = StateConnected;
        } else if (self.call.state == TVOCallStateConnecting) {
            params[@"call_state"] = StateConnecting;
        } else if (self.call.state == TVOCallStateDisconnected) {
            params[@"call_state"] = StateDisconnected;
        }
        resolve(params);
    } else {
        reject(@"no_call", @"There was no active call", nil);
    }
}

RCT_EXPORT_METHOD(setEdge:(NSString *) edge) {
    NSLog(@"[IIMobile - RNTwilioVoice] setEdge(%@)", edge);
    TwilioVoiceSDK.edge = edge;
}

- (void)configureCallKit {
    NSString *appName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    NSDictionary *options = @{@"appName": appName, @"imageName": @"ii-logo", @"ringtoneSound": @"incoming.mp3"};
    self.callKitCallController = [[CXCallController alloc] init];
    _settings = [[NSMutableDictionary alloc] initWithDictionary:options];
    self.callKitProvider = [[CXProvider alloc] initWithConfiguration:[self getProviderConfiguration]];
    [self.callKitProvider setDelegate:self queue:nil];
}

- (void)configureAudioDevice {
    self.audioDevice = [TVODefaultAudioDevice audioDevice];
    TwilioVoiceSDK.audioDevice = self.audioDevice;
}

- (void)initPushRegistry {
    self.voipRegistry = [[PKPushRegistry alloc] initWithQueue:dispatch_get_main_queue()];
    self.voipRegistry.delegate = self;
    self.voipRegistry.desiredPushTypes = [NSSet setWithObject:PKPushTypeVoIP];
}

- (int)getHandleType:(NSString *)handleType {
    int _handleType;
    if ([handleType isEqualToString:@"generic"]) {
        _handleType = CXHandleTypeGeneric;
    } else if ([handleType isEqualToString:@"number"]) {
        _handleType = CXHandleTypePhoneNumber;
    } else if ([handleType isEqualToString:@"email"]) {
        _handleType = CXHandleTypeEmailAddress;
    } else {
        _handleType = CXHandleTypeGeneric;
    }
    return _handleType;
}

- (CXProviderConfiguration *)getProviderConfiguration {
    NSLog(@"[IIMobile - RNTwilioVoice][getProviderConfiguration]");
    CXProviderConfiguration *providerConfiguration = [[CXProviderConfiguration alloc] initWithLocalizedName:_settings[@"appName"]];
    providerConfiguration.supportsVideo = YES;
    providerConfiguration.maximumCallGroups = 1;
    providerConfiguration.maximumCallsPerCallGroup = 1;
    providerConfiguration.supportedHandleTypes = [NSSet setWithObjects:@(CXHandleTypePhoneNumber), @(CXHandleTypeEmailAddress), @(CXHandleTypeGeneric), nil];
    if (_settings[@"imageName"]) {
        providerConfiguration.iconTemplateImageData = UIImagePNGRepresentation([UIImage imageNamed:_settings[@"imageName"]]);
    }
    if (_settings[@"ringtoneSound"]) {
        providerConfiguration.ringtoneSound = _settings[@"ringtoneSound"];
    }
    return providerConfiguration;
}

#pragma mark - PKPushRegistryDelegate ##################

- (void)pushRegistry:(PKPushRegistry *)registry didUpdatePushCredentials:(PKPushCredentials *)credentials forType:(NSString *)type {
    self.deviceTokenString = [self stringFromDeviceToken:credentials.token];
    self.deviceToken = credentials.token;
    self.type = type;
    NSLog(@"[IIMobile - RNTwilioVoice][didUpdatePushCredentials][DeviceToken: %@]", self.deviceTokenString);
}

- (void)pushRegistry:(PKPushRegistry *)registry didInvalidatePushTokenForType:(PKPushType)type {
    NSLog(@"[IIMobile - RNTwilioVoice][didInvalidatePushTokenForType]");
    if ([type isEqualToString:PKPushTypeVoIP]) {
        self.deviceTokenString = nil;
    }
}

/**
* Try using the `pushRegistry:didReceiveIncomingPushWithPayload:forType:withCompletionHandler:` method if
* your application is targeting iOS 11. According to the docs, this delegate method is deprecated by Apple.
*/
- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type {
    [self handleIncomingPushWithPayload:payload forType: type withCompletionHandler:nil];
}

/**
 * This delegate method is available on iOS 11 and above. Call the completion handler once the
 * notification payload is passed to the `TwilioVoice.handleNotification()` method.
 */
- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(PKPushType)type withCompletionHandler:(void (^)(void))completion {
    [self handleIncomingPushWithPayload:payload forType: type withCompletionHandler:completion];
}

- (void)handleIncomingPushWithPayload:(PKPushPayload *)payload
                              forType:(PKPushType)type
                withCompletionHandler:(void (^)(void))completion {
    NSLog(@"[IIMobile - RNTwilioVoice][didReceiveIncomingPushWithPayload] payload %@", payload.dictionaryPayload);

    NSString *mode = payload.dictionaryPayload[@"mode"];
    NSString *action = payload.dictionaryPayload[@"action"];
    NSString *displayName = [NSString stringWithFormat:@"%@ / %@ / %@",
                             payload.dictionaryPayload[@"languageName"],
                             [self getCallDurationString: payload.dictionaryPayload[@"estimatedDuration"]],
                             payload.dictionaryPayload[@"customerName"]];

    self.dictionaryPayload = payload.dictionaryPayload;
    self.incomingPushCompletionCallback = completion;

    if ([action isEqualToString:@"cancel"]) {
        // Cancel Video Call
        NSLog(@"[IIMobile - RNTwilioVoice] handleIncomingPushWithPayload: CANCEL");

        if (self.callUuid != nil || self.callInvite != nil) {
            [self performEndCallActionWithUUID:self.callUuid];
        } else {
            NSLog(@"[IIMobile - RNTwilioVoice] performFakeCallToPreventAppKilled");
            int _handleType = [self getHandleType:@"generic"];
            NSDictionary *taskAttributes = payload.dictionaryPayload[@"taskAttributes"];

            CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
            callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:displayName];
            callUpdate.supportsDTMF = YES;
            callUpdate.supportsHolding = NO;
            callUpdate.supportsGrouping = NO;
            callUpdate.supportsUngrouping = NO;
            callUpdate.hasVideo = NO;

            NSUUID *uuid = [NSUUID UUID];
            [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *error) {
            }];

            dispatch_async(dispatch_get_main_queue(), ^{
                [self performEndCallActionWithUUID:uuid];
            });
        }

    } else if ([mode isEqualToString:@"video"]) {
        // Receive Video Call
        NSLog(@"[IIMobile - RNTwilioVoice] handleIncomingPushWithPayload: VIDEO");
        int _handleType = [self getHandleType:@"generic"];
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:[payload.dictionaryPayload[@"session"] uppercaseString]];
        CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
        callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:displayName];
        callUpdate.supportsDTMF = YES;
        callUpdate.supportsHolding = NO;
        callUpdate.supportsGrouping = NO;
        callUpdate.supportsUngrouping = NO;
        callUpdate.hasVideo = true;
        callUpdate.localizedCallerName = displayName;

        [[AVAudioSession sharedInstance] setActive: YES error: nil];

        [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *_Nullable error) {
            self.callInvite = @"true";
            [RNEventEmitterHelper emitEventWithName:@"displayIncomingCall" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        }];
    } else if ([mode isEqualToString:@"voice"]) {
        // Receive Voice Call
        NSLog(@"[IIMobile - RNTwilioVoice] handleIncomingPushWithPayload: VOICE");
        int _handleType = [self getHandleType:@"generic"];
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:[payload.dictionaryPayload[@"session"] uppercaseString]];
        CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
        callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:displayName];
        callUpdate.supportsDTMF = YES;
        callUpdate.supportsHolding = NO;
        callUpdate.supportsGrouping = NO;
        callUpdate.supportsUngrouping = NO;
        callUpdate.hasVideo = NO;
        callUpdate.localizedCallerName = displayName;

        [[AVAudioSession sharedInstance] setActive: YES error: nil];

        [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *_Nullable error) {
            self.callInvite = @"true";
            [RNEventEmitterHelper emitEventWithName:@"displayIncomingCall" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        }];

    }

    /**
    * The Voice SDK processes the call notification and returns the call invite synchronously. Report the incoming call to
    * CallKit and fulfill the completion before exiting this callback method.
    */
    if (completion != nil && [[NSProcessInfo processInfo] operatingSystemVersion].majorVersion >= 13) {
        completion();
    }
}

- (NSString *)stringFromDeviceToken:(NSData *)deviceToken {
    NSUInteger length = deviceToken.length;
    if (length == 0) {
        return nil;
    }
    const unsigned char *buffer = deviceToken.bytes;
    NSMutableString *hexString = [NSMutableString stringWithCapacity:(length * 2)];
    for (int i = 0; i < length; ++i) {
        [hexString appendFormat:@"%02x", buffer[i]];
    }
    return [hexString copy];
}

- (void)incomingPushHandled {
    if (self.incomingPushCompletionCallback) {
        self.incomingPushCompletionCallback();
        self.incomingPushCompletionCallback = nil;
    }
}

- (NSString *)getCallDurationString:(NSString *)duration {
    int minutes = [duration intValue];

    if (minutes == 120) {
        NSString *unit = [[RNLocalizedStrings sharedInstance] translate:@"hoursOrMore"];
        return [NSString stringWithFormat:@"2 %@", unit];
    }
    if (minutes == 60) {
        NSString *unit = [[RNLocalizedStrings sharedInstance] translate:@"hour"];
        return [NSString stringWithFormat:@"1 %@", unit];
    }
    NSString *unit = [[RNLocalizedStrings sharedInstance] translate:@"minutes"];
    return [NSString stringWithFormat:@"%d %@", minutes, unit];
}

#pragma mark - TVOCallDelegate

- (void)callDidConnect:(TVOCall *)call {
    NSLog(@"[IIMobile - RNTwilioVoice] callDidConnect: %@", [call.uuid UUIDString]);
    self.call = call;
    self.callKitCompletionCallback(YES);
    self.callKitCompletionCallback = nil;

    self.twilioMos = [[RNTwilioMos alloc] initWithTVOCall:call];

    NSMutableDictionary *callParams = [[NSMutableDictionary alloc] init];
    callParams[@"call_sid"] = call.sid;
    if (call.state == TVOCallStateConnecting) {
        callParams[@"call_state"] = StateConnecting;
    } else if (call.state == TVOCallStateConnected) {
        callParams[@"call_state"] = StateConnected;
    }
    if (call.from) {
        callParams[@"from"] = call.from;
    }
    if (call.to) {
        callParams[@"to"] = call.to;
    }

    self.callParams = callParams;

    [RNEventEmitterHelper emitEventWithName:@"connectionDidConnect" andPayload:callParams];
}

- (void)call:(TVOCall *)call didFailToConnectWithError:(NSError *)error {
    NSLog(@"[IIMobile - RNTwilioVoice] Call failed to connect: %@", error);
    [RNEventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
    self.callKitCompletionCallback(NO);
    [self performEndCallActionWithUUID:call.uuid];
    [self callDisconnected:error];
}

- (void)call:(TVOCall *)call didDisconnectWithError:(NSError *)error {
    NSLog(@"[IIMobile - RNTwilioVoice] Call disconnected with error: %@", error);
    [self performEndCallActionWithUUID:call.uuid];
    [self callDisconnected:error];
}

- (void)callDisconnected:(NSError *)error {
    NSLog(@"[IIMobile - RNTwilioVoice] callDisconnected with error: %@", error ? error.localizedDescription : @"");
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    [self.twilioMos stop]; // Stop MOS listener

    if (error) {
        NSString *errMsg = [error localizedDescription];
        if (error.localizedFailureReason) {
            errMsg = [error localizedFailureReason];
        }
        params[@"error"] = errMsg;
    }
    if (self.call.sid) {
        params[@"call_sid"] = self.call.sid;
    }
    if (self.call.to) {
        params[@"call_to"] = self.call.to;
    }
    if (self.call.from) {
        params[@"call_from"] = self.call.from;
    }
    if (self.call.state == TVOCallStateDisconnected) {
        params[@"call_state"] = StateDisconnected;
    }
    params[@"averageMos"] = [[NSDecimalNumber alloc] initWithDouble: [self.twilioMos getAverageMos]];
    params[@"maxMos"] = [[NSDecimalNumber alloc] initWithDouble: [self.twilioMos getMaxMos]];
    params[@"minMos"] = [[NSDecimalNumber alloc] initWithDouble: [self.twilioMos getMinMos]];

    [RNEventEmitterHelper emitEventWithName:@"connectionDidDisconnect" andPayload:params];

    [self.call disconnect];
    self.call = nil;
    self.callKitCompletionCallback = nil;
}

- (void)call:(TVOCall *)call isReconnectingWithError:(NSError *)error {
    NSLog(@"[IIMobile - RNTwilioVoice] isReconnectingWithError %@", [error localizedDescription]);
    // Update UI
    // Check the error: It could be either
    // TVOErrorSignalingConnectionDisconnectedError (53001) or
    // TVOErrorMediaConnectionError (53405).
    [RNEventEmitterHelper emitEventWithName:@"isReconnectingWithError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
}

- (void)callDidReconnect:(TVOCall *)call {
    NSLog(@"[IIMobile - RNTwilioVoice] callDidReconnect with uuid %@", call.uuid);
    self.call = call;
    [RNEventEmitterHelper emitEventWithName:@"callDidReconnect" andPayload:@{@"uuid": call.uuid}];
}

- (void)callDidStartRinging:(TVOCall *)call {
    NSLog(@"[IIMobile - RNTwilioVoice] callDidStartRinging with uuid %@", call.uuid);

    /*
     When [answerOnBridge](https://www.twilio.com/docs/voice/twiml/dial#answeronbridge) is enabled in the
     <Dial> TwiML verb, the caller will not hear the ringback while the call is ringing and awaiting to be
     accepted on the callee's side. The application can use the `AVAudioPlayer` to play custom audio files
     between the `[TVOCallDelegate callDidStartRinging:]` and the `[TVOCallDelegate callDidConnect:]` callbacks.
     */
}

- (void)call:(TVOCall *)call
didReceiveQualityWarnings:(NSSet<NSNumber *> *)currentWarnings
previousWarnings:(NSSet<NSNumber *> *)previousWarnings {
    NSMutableSet *warningIntersetction = [currentWarnings mutableCopy];
    [warningIntersetction intersectSet:previousWarnings];

    NSMutableSet *newWarnings = [currentWarnings mutableCopy];
    [newWarnings minusSet:warningIntersetction];
    if ([newWarnings count] > 0) {
        for (NSNumber *warning in newWarnings) {
            NSString *warningName = [self warningString:[warning unsignedIntValue]];
            NSLog(@"[IIMobile - RNTwilioVoice] didReceiveQualityWarnings NEW WARNING: %@", warningName);
        }
    }

    NSMutableSet *clearedWarnings = [previousWarnings mutableCopy];
    [clearedWarnings minusSet:warningIntersetction];
    if ([clearedWarnings count] > 0) {
        for (NSNumber *warning in clearedWarnings) {
            NSString *warningName = [self warningString:[warning unsignedIntValue]];
            NSLog(@"[IIMobile - RNTwilioVoice] didReceiveQualityWarnings WARNING CLEARED: %@", warningName);
        }
    }
}

- (NSString *)warningString:(TVOCallQualityWarning)qualityWarning {
    switch (qualityWarning) {
        case TVOCallQualityWarningHighRtt:
            return @"high-rtt";
            break;
        case TVOCallQualityWarningHighJitter:
            return @"high-jitter";
            break;
        case TVOCallQualityWarningHighPacketsLostFraction:
            return @"high-packets-lost-fraction";
            break;
        case TVOCallQualityWarningLowMos:
            return @"low-mos";
            break;
        case TVOCallQualityWarningConstantAudioInputLevel:
            return @"constant-audio-input-level";
            break;
        default:
            return @"Unknown warning";
            break;
    }
}

#pragma mark - AVAudioSession

- (void)toggleAudioRoute:(BOOL *)toSpeaker __attribute((deprecated("Use the RNAudioManager instead."))) {
    NSError *error = nil;
    NSLog(@"[IIMobile - RNTwilioVoice] toggleAudioRoute");

    if (toSpeaker) {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNTwilioVoice] Unable to reroute audio: %@", [error localizedDescription]);
        }
    } else {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideNone
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNTwilioVoice] Unable to reroute audio: %@", [error localizedDescription]);
        }
    }
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider {
    NSLog(@"[IIMobile - RNTwilioVoice] providerDidReset");
    self.audioDevice.enabled = YES;
}

- (void)providerDidBegin:(CXProvider *)provider {
    NSLog(@"[IIMobile - RNTwilioVoice] providerDidBegin");
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession {
    NSLog(@"[IIMobile - RNTwilioVoice] provider:didActivateAudioSession");
    self.audioDevice.enabled = YES;
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession {
    NSLog(@"[IIMobile - RNTwilioVoice] provider:didDeactivateAudioSession");
    self.audioDevice.enabled = NO;
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action {
    NSLog(@"[IIMobile - RNTwilioVoice] provider:timedOutPerformingAction");
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    NSLog(@"[IIMobile - RNTwilioVoice] provider:performStartCallAction");

    [self.callKitProvider reportOutgoingCallWithUUID:action.callUUID startedConnectingAtDate:[NSDate date]];

    __weak typeof(self) weakSelf = self;
    [self performVoiceCallWithUUID:action.callUUID client:nil completion:^(BOOL success) {
        __strong typeof(self) strongSelf = weakSelf;
        if (success) {
            [strongSelf.callKitProvider reportOutgoingCallWithUUID:action.callUUID connectedAtDate:[NSDate date]];
            [action fulfill];
        } else {
            [action fail];
        }
    }];
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {
    NSLog(@"[IIMobile - RNTwilioVoice] provider:performAnswerCallAction wioth UUID: %@", [action.callUUID UUIDString]);
    self.action = action;

    NSString *mode = self.dictionaryPayload[@"mode"];
    self.callUuid = action.callUUID;

    if ([mode isEqualToString:@"video"]) {
        [self performAnswerVideoCallWithUUID:action.callUUID completion:^(BOOL success) {
            if (success) {
                [action fulfill];
            } else {
                [action fail];
            }
        }];
    } else {
        [self performAnswerVoiceCallWithUUID:action.callUUID completion:^(BOOL success) {
            if (success) {
                [action fulfill];
            } else {
                [action fail];
            }
        }];
    }
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    NSLog(@"[IIMobile - RNTwilioVoice] provider:performEndCallAction with UUID: %@", [action.callUUID UUIDString]);

    if (self.call) {
        [self.call disconnect];
    }

    NSString *mode = self.dictionaryPayload[@"mode"];
    if (self.callUuid == nil) {
        NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
        params[@"session"] = self.dictionaryPayload[@"session"];
        params[@"companyUuid"] = self.dictionaryPayload[@"companyUuid"];
        params[@"reservationSid"] = self.dictionaryPayload[@"reservationSid"];
        params[@"uuid"] = self.dictionaryPayload[@"uuid"];

        NSString *event = [mode isEqualToString:@"video"] ? @"performEndVideoCall" : @"performEndVoiceCall";

        [RNEventEmitterHelper emitEventWithName:event andPayload:params];
    }
    self.callUuid = nil;
    self.callInvite = nil;

    [action fulfill];
}

- (void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action {
    if (self.call && self.call.state == TVOCallStateConnected) {
        [self.call setOnHold:action.isOnHold];
        [action fulfill];
    } else {
        [action fail];
    }
}

#pragma mark - CallKit Actions

- (void)performStartCallActionWithUUID:(NSUUID *)uuid handle:(NSString *)handle andToken:(NSString *)token {
    if (uuid == nil || handle == nil || token == nil) {
        return;
    }

    _token = token;

    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:callHandle];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];

    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
        if (error) {
            NSLog(@"[IIMobile - RNTwilioVoice] StartCallAction transaction request failed: %@", [error localizedDescription]);
            [RNEventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        } else {
            NSLog(@"[IIMobile - RNTwilioVoice] StartCallAction transaction request successful");

            CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
            callUpdate.remoteHandle = callHandle;
            callUpdate.supportsDTMF = YES;
            callUpdate.supportsHolding = YES;
            callUpdate.supportsGrouping = NO;
            callUpdate.supportsUngrouping = NO;
            callUpdate.hasVideo = NO;

            [self.callKitProvider reportCallWithUUID:uuid updated:callUpdate];
        }
    }];
}

- (void)reportIncomingCallFrom:(NSString *)from withUUID:(NSUUID *)uuid {
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:from];

    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = callHandle;
    callUpdate.supportsDTMF = YES;
    callUpdate.supportsHolding = YES;
    callUpdate.supportsGrouping = NO;
    callUpdate.supportsUngrouping = NO;
    callUpdate.hasVideo = NO;

    [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *error) {
        if (!error) {
            NSLog(@"[IIMobile - RNTwilioVoice] Incoming call successfully reported with UUID: %@", [uuid UUIDString]);
        } else {
            NSLog(@"[IIMobile - RNTwilioVoice] Failed to report incoming call successfully: %@.", [error localizedDescription]);
        }
    }];
}

- (void)performEndCallActionWithUUID:(NSUUID *)uuid {
    NSLog(@"[IIMobile - RNTwilioVoice] performEndCallActionWithUUID: %@", [uuid UUIDString]);

    UIDevice *device = [UIDevice currentDevice];
    device.proximityMonitoringEnabled = NO;

    if (uuid != nil) {
        CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:uuid];
        CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
        [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
            if (error) {
                NSLog(@"[IIMobile - RNTwilioVoice] EndCallAction transaction request failed for UUID %@: %@", [uuid UUIDString], [error localizedDescription]);
                for (CXCall *call in self.callKitCallController.callObserver.calls) {
                    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:call.UUID];
                    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
                    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
                        if (error) {
                            NSLog(@"[IIMobile - RNTwilioVoice] EndCallAction transaction request failed: %@", [error localizedDescription]);

                        } else {
                            NSLog(@"[IIMobile - RNTwilioVoice] EndCallAction transaction request successful");
                        }
                    }];
                }
            } else {
                NSLog(@"[IIMobile - RNTwilioVoice] EndCallAction transaction request successful");
            }
        }];
    } else if (self.callInvite != nil) {
        for (CXCall *call in self.callKitCallController.callObserver.calls) {
            CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:call.UUID];
            CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
            [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
                if (error) {
                    NSLog(@"[IIMobile - RNTwilioVoice] EndCallAction transaction request failed: %@", [error localizedDescription]);

                } else {
                    NSLog(@"[IIMobile - RNTwilioVoice] EndCallAction transaction request successful");
                }
            }];
        }
    }

}

- (void)performVoiceCallWithUUID:(NSUUID *)uuid
                          client:(NSString *)client
                      completion:(void (^)(BOOL success))completionHandler {

    if (_token == nil) {
        [RNEventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": @"Invalid access token"}];
    } else {
        NSMutableArray *iceServers = [NSMutableArray array];
        TVOConnectOptions *options;

        if (_iceServers != nil && [_iceServers count] > 0) {
            for (NSDictionary *is in _iceServers) {
                TVOIceServer *iceServer = [[TVOIceServer alloc] initWithURLString:[is objectForKey:@"url"]
                                                                          username:[is objectForKey:@"username"]
                                                                          password:[is objectForKey:@"credential"]];
                [iceServers addObject:iceServer];
            }

            TVOIceOptions *iceOptions = [TVOIceOptions optionsWithBlock:^(TVOIceOptionsBuilder *builder) {
                builder.servers = iceServers;
            }];

            options = [TVOConnectOptions optionsWithAccessToken:_token
                                                                             block:^(TVOConnectOptionsBuilder *builder) {
                                                                                  builder.params = _callParams;
                                                                                 builder.uuid = uuid;
                                                                                 builder.iceOptions = iceOptions;
                                                                             }];
        } else { // Legacy code V3.86
            options = [TVOConnectOptions optionsWithAccessToken:_token
                                                                             block:^(TVOConnectOptionsBuilder *builder) {
                                                                                 builder.params = _callParams;
                                                                                 builder.uuid = uuid;
                                                                             }];
        }

        self.call = [TwilioVoiceSDK connectWithOptions:options delegate:self];
        self.callKitCompletionCallback = completionHandler;
    }
}

- (void)performAnswerVoiceCallWithUUID:(NSUUID *)uuid
                            completion:(void (^)(BOOL success))completionHandler {
    self.callInvite = nil;
    self.callKitCompletionCallback = completionHandler;
    self.callMode = @"voice";

    NSLog(@"[IIMobile - RNTwilioVoice] sendPerformAnswerVoiceCallEvent called with UUID: %@", uuid.UUIDString);
    [RNEventEmitterHelper emitEventWithName:@"performAnswerVoiceCall"
                                 andPayload:@{
                                     @"voipPush": self.dictionaryPayload,
                                     @"uuid": uuid.UUIDString,
                                     @"timestamp":  @([[NSDate date] timeIntervalSince1970] * 1000)
                                 }];

    if ([[NSProcessInfo processInfo] operatingSystemVersion].majorVersion < 13) {
        [self incomingPushHandled];
    }
}

- (void)performAnswerVideoCallWithUUID:(NSUUID *)uuid
                            completion:(void (^)(BOOL success))completionHandler {
    self.callInvite = nil;
    self.callKitCompletionCallback = completionHandler;
    self.callMode = @"video";

    NSLog(@"[IIMobile - RNTwilioVoice] sendPerformAnswerVideoCallEvent called with UUID: %@", uuid.UUIDString);

    [RNEventEmitterHelper emitEventWithName:@"performAnswerVideoCall" andPayload:@{@"voipPush": self.dictionaryPayload, @"uuid": uuid.UUIDString}];
}

- (void)handleAppTerminateNotification {
    NSLog(@"[IIMobile - RNTwilioVoice] handleAppTerminateNotification called");
    if (self.call) {
        NSLog(@"[IIMobile - RNTwilioVoice] handleAppTerminateNotification disconnecting an active call");
        [self.call disconnect];
    }
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

@end
