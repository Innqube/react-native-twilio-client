//
//  RNTwilioVoiceClient.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard.
//  Copyright © 2018 No Good Software Inc. All rights reserved.
//


#import "RNTwilioVoiceClient.h"
#import <React/RCTLog.h>
#import <PushKit/PushKit.h>
#import <CallKit/CallKit.h>
#import "RNEventEmitterHelper.h"

@import AVFoundation;
@import PushKit;
@import CallKit;
@import TwilioVoice;
@import UIKit;


@interface RNTwilioVoiceClient () <PKPushRegistryDelegate, TVONotificationDelegate, TVOCallDelegate, CXProviderDelegate>
@property(nonatomic, strong) NSString *deviceTokenString;
@property(nonatomic, strong) NSDictionary *dictionaryPayload;
@property(nonatomic, strong) NSString *type;
@property(nonatomic, strong) TVOCallInvite *callInvite;
@property(nonatomic, strong) TVOCall *call;
@property(nonatomic, strong) NSUUID *callUuid;
@property(nonatomic, strong) NSString *callMode;
@property(nonatomic, strong) NSMutableDictionary *callParams;
@property(nonatomic, strong) PKPushRegistry *voipRegistry;
@property(nonatomic, strong) void (^callKitCompletionCallback)(BOOL);
@property(nonatomic, strong) CXProvider *callKitProvider;
@property(nonatomic, strong) CXCallController *callKitCallController;
@property(nonatomic, strong) TVODefaultAudioDevice *audioDevice;
@end

@implementation RNTwilioVoiceClient {
    NSMutableDictionary *_settings;
    NSMutableDictionary *_callParams;
    NSString *_token;
}

RCT_EXPORT_MODULE()

        static RNTwilioVoiceClient *sharedInstance = nil;
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
    }
    return sharedInstance;
}

- (id)init {
    return [RNTwilioVoiceClient sharedInstance];
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
        NSLog(@"[IIMobile - RNTwilioVoiceClient][Message from ReactNative] %@", message);
    } else {
        NSLog(@"[IIMobile - RNTwilioVoiceClient][Message from ReactNative] NULL");
    }
}

RCT_REMAP_METHOD(init, withToken:(NSString*)token init_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] init with token: %@", token);

    #if TARGET_IPHONE_SIMULATOR
        self.deviceTokenString = @"device-token-simulator";
    #endif

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleAppTerminateNotification) name:UIApplicationWillTerminateNotification object:nil];

    [TwilioVoice registerWithAccessToken:token
                             deviceToken:self.deviceTokenString
                              completion:^(NSError *error) {
                                  if (error) {
                                      NSLog(@"[IIMobile - RNTwilioVoiceClient] An error occurred while registering: %@", [error localizedDescription]);
                                      reject(@"init-client-error", [error localizedDescription], nil);
                                  } else {
                                      NSLog(@"[IIMobile - RNTwilioVoiceClient] Successfully registered for VoIP push notifications.");
                                      resolve(@{@"status": @"null"});
                                  }
                              }];
}

RCT_EXPORT_METHOD(connect: (NSDictionary *)params andToken: (NSString *) token) {
    NSLog(@"[IIMobile - RNTwilioVoiceClient][connect] Calling phone number %@", [params valueForKey:@"To"]);

    UIDevice *device = [UIDevice currentDevice];
    device.proximityMonitoringEnabled = YES;

    if (self.call && self.call.state == TVOCallStateConnected) {
        [self.call disconnect];
    } else {
        NSUUID *uuid = [NSUUID UUID];
        NSString *handle = [params valueForKey:@"To"];
        _callParams = [[NSMutableDictionary alloc] initWithDictionary:params];
        [self performStartCallActionWithUUID:uuid handle:handle andToken:token];
    }
}

RCT_EXPORT_METHOD(disconnect:(NSString *) uuidStr) {
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidStr];
    NSLog(@"[IIMobile - RNTwilioVoiceClient][disconnect] Disconnecting call with UUID: %@", uuidStr);
    [self performEndCallActionWithUUID:(uuid != nil ? uuid : self.call.uuid)];
}

RCT_EXPORT_METHOD(setMuted:(BOOL *) muted) {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] setMuted");
    self.call.muted = (BOOL) muted;
}

RCT_EXPORT_METHOD(setSpeakerPhone:(BOOL *) speaker) {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] setSpeakerPhone");
    [self toggleAudioRoute:speaker];
}

RCT_EXPORT_METHOD(sendDigits: (NSString *) digits) {
    if (self.call && self.call.state == TVOCallStateConnected) {
        NSLog(@"[IIMobile - RNTwilioVoiceClient][sendDigits] %@", digits);
        [self.call sendDigits:digits];
    }
}

RCT_EXPORT_METHOD(unregister: (NSString *) token) {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] unregister with accessToken: %@", token);

    [TwilioVoice unregisterWithAccessToken:token
                               deviceToken:self.deviceTokenString
                                completion:^(NSError *_Nullable error) {
                                    if (error) {
                                        NSLog(@"[IIMobile - RNTwilioVoiceClient] An error occurred while unregistering: %@", [error localizedDescription]);
                                    } else {
                                        NSLog(@"[IIMobile - RNTwilioVoiceClient] Successfully unregistered for VoIP push notifications.");
                                    }
                                }];
}

RCT_REMAP_METHOD(getDeviceToken, tokenResolver: (RCTPromiseResolveBlock)resolve rej:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioVoiceClient][getDeviceToken] %@", self.deviceTokenString);

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
    NSLog(@"[IIMobile - RNTwilioVoiceClient][getActiveCall]");
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    if (self.callInvite) {
        if (self.callInvite.callSid) {
            params[@"call_sid"] = self.callInvite.callSid;
        }
        if (self.callInvite.from) {
            params[@"from"] = self.callInvite.from;
        }
        if (self.callInvite.to) {
            params[@"to"] = self.callInvite.to;
        }
        resolve(params);
    } else if (self.call) {
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

- (void)configureCallKit {
    NSDictionary *options = @{@"appName": @"Interpreter Intelligence", @"imageName": @"ii-logo", @"ringtoneSound": @"incoming.mp3"};
    self.callKitCallController = [[CXCallController alloc] init];
    _settings = [[NSMutableDictionary alloc] initWithDictionary:options];
    self.callKitProvider = [[CXProvider alloc] initWithConfiguration:[self getProviderConfiguration]];
    [self.callKitProvider setDelegate:self queue:nil];
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
    NSLog(@"[IIMobile - RNTwilioVoiceClient][getProviderConfiguration]");
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
    self.type = type;
    NSLog(@"[IIMobile - RNTwilioVoiceClient][didUpdatePushCredentials][DeviceToken: %@]", self.deviceTokenString);
}

- (void)pushRegistry:(PKPushRegistry *)registry didInvalidatePushTokenForType:(PKPushType)type {
    NSLog(@"[IIMobile - RNTwilioVoiceClient][didInvalidatePushTokenForType]");
    if ([type isEqualToString:PKPushTypeVoIP]) {
        /*
        [TwilioVoice unregisterWithAccessToken:token
                                   deviceToken:self.deviceTokenString
                                    completion:^(NSError *_Nullable error) {
                                        if (error) {
                                            NSLog(@"[IIMobile - RNTwilioVoiceClient] An error occurred while unregistering: %@", [error localizedDescription]);
                                        } else {
                                            NSLog(@"[IIMobile - RNTwilioVoiceClient] Successfully unregistered for VoIP push notifications.");
                                        }
                                    }];
        */
        self.deviceTokenString = nil;
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type {
    NSLog(@"[IIMobile - RNTwilioVoiceClient][didReceiveIncomingPushWithPayload] payload %@", payload.dictionaryPayload);

    NSString *mode = payload.dictionaryPayload[@"mode"];
    NSString *msgType = payload.dictionaryPayload[@"twi_message_type"];
    NSString *action = payload.dictionaryPayload[@"action"];
    self.dictionaryPayload = payload.dictionaryPayload;

    if (![type isEqualToString:PKPushTypeVoIP]) {
        return;
    }

    if ([mode isEqualToString:@"video"]) {
        // Receive Video Call, handle by II
        NSLog(@"[IIMobile - RNTwilioVoiceClient] VOIP_VIDEO_NOTIF: didReceiveIncomingPushWithPayload: %@", payload);
        NSLog(@"[IIMobile - RNTwilioVoiceClient][displayIncomingCall] uuidString = %@", payload.dictionaryPayload[@"session"]);
        int _handleType = [self getHandleType:@"generic"];
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:[payload.dictionaryPayload[@"session"] uppercaseString]];
        CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
        callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:payload.dictionaryPayload[@"displayName"]];
        callUpdate.supportsDTMF = YES;
        callUpdate.supportsHolding = NO;
        callUpdate.supportsGrouping = NO;
        callUpdate.supportsUngrouping = NO;
        callUpdate.hasVideo = true;
        callUpdate.localizedCallerName = payload.dictionaryPayload[@"displayName"];

        [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *_Nullable error) {
            [RNEventEmitterHelper emitEventWithName:@"displayIncomingCall" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        }];
    } else if ([msgType isEqualToString:@"twilio.voice.call"]) {
        // Receive Voice Call, Twilio handle this push
        NSLog(@"[IIMobile - RNTwilioVoiceClient] VOIP_VOICE_NOTIF: didReceiveIncomingPushWithPayload: %@", payload);
        [TwilioVoice handleNotification:payload.dictionaryPayload
                                       delegate:self
                                  delegateQueue:nil];
        /*
    } else if ([msgType isEqualToString:@"twilio.voice.cancel"] && self.callInvite && self.callInvite.state == TVOCallInviteStatePending) {
        // Cancel Voice Call, Twilio handle this push
        [self performEndCallActionWithUUID:self.call.uuid];
         */
    } else if ([action isEqualToString:@"cancel"]) {
        // Cancel Video or Voice Call, sent by II
        [self performEndCallActionWithUUID:self.call.uuid];
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

#pragma mark - TVONotificationDelegate

- (void)callInviteReceived:(TVOCallInvite *)callInvite {
    [self handleCallInviteReceived:callInvite];
}

- (void)cancelledCallInviteReceived:(TVOCancelledCallInvite *)cancelledCallInvite {
    [RNEventEmitterHelper emitEventWithName:@"connectionDidDisconnect" andPayload:@{@"callSid":cancelledCallInvite.callSid}];
    self.callInvite = nil;
}

- (void)handleCallInviteReceived:(TVOCallInvite *)callInvite {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] callInviteReceived with UUID: %@", [callInvite.uuid UUIDString]);
    if (self.callInvite) {
        NSLog(@"[IIMobile - RNTwilioVoiceClient] Already a pending incoming call invite.");
        NSLog(@"[IIMobile - RNTwilioVoiceClient]   >> Ignoring call from %@", callInvite.from);
        return;
    } else if (self.call) {
        NSLog(@"[IIMobile - RNTwilioVoiceClient] Already an active call.");
        NSLog(@"[IIMobile - RNTwilioVoiceClient]   >> Ignoring call from %@", callInvite.from);
        return;
    }

    self.callInvite = callInvite;

    [self reportIncomingCallFrom:callInvite.from withUUID:callInvite.uuid];
}

#pragma mark - TVOCallDelegate

- (void)callDidConnect:(TVOCall *)call {
    self.call = call;
    self.callKitCompletionCallback(YES);
    self.callKitCompletionCallback = nil;

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
    NSLog(@"[IIMobile - RNTwilioVoiceClient] Call failed to connect: %@", error);
    [RNEventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
    self.callKitCompletionCallback(NO);
    [self performEndCallActionWithUUID:call.uuid];
    [self callDisconnected:error];
}

- (void)call:(TVOCall *)call didDisconnectWithError:(NSError *)error {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] Call disconnected with error: %@", error);
    [self performEndCallActionWithUUID:call.uuid];
    [self callDisconnected:error];
}

- (void)callDisconnected:(NSError *)error {
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
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
    [RNEventEmitterHelper emitEventWithName:@"connectionDidDisconnect" andPayload:params];

    [self.call disconnect];
    self.call = nil;
    self.callKitCompletionCallback = nil;
}

- (void)call:(TVOCall *)call isReconnectingWithError:(NSError *)error {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] isReconnectingWithError %@", [error localizedDescription]);
    // Update UI
    // Check the error: It could be either
    // TVOErrorSignalingConnectionDisconnectedError (53001) or
    // TVOErrorMediaConnectionError (53405).
    [RNEventEmitterHelper emitEventWithName:@"isReconnectingWithError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
}

- (void)callDidReconnect:(TVOCall *)call {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] callDidReconnect with uuid %@", call.uuid);
    self.call = call;
    [RNEventEmitterHelper emitEventWithName:@"callDidReconnect" andPayload:@{@"uuid": call.uuid}];
}

#pragma mark - AVAudioSession

- (void)toggleAudioRoute:(BOOL *)toSpeaker {
    NSError *error = nil;
    NSLog(@"[IIMobile - RNTwilioVoiceClient] toggleAudioRoute");

    if (toSpeaker) {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNTwilioVoiceClient] Unable to reroute audio: %@", [error localizedDescription]);
        }
    } else {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideNone
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNTwilioVoiceClient] Unable to reroute audio: %@", [error localizedDescription]);
        }
    }
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] providerDidReset");
    self.audioDevice.enabled = YES;
}

- (void)providerDidBegin:(CXProvider *)provider {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] providerDidBegin");
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] provider:didActivateAudioSession");
    self.audioDevice.enabled = YES;
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] provider:didDeactivateAudioSession");
    self.audioDevice.enabled = NO;
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] provider:timedOutPerformingAction");
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] provider:performStartCallAction");
    self.audioDevice.enabled = NO;

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
    NSLog(@"[IIMobile - RNTwilioVoiceClient] provider:performAnswerCallAction wioth UUID: %@", [action.callUUID UUIDString]);

    NSString *mode = self.dictionaryPayload[@"mode"];
    self.audioDevice.enabled = NO;
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
    NSLog(@"[IIMobile - RNTwilioVoiceClient] provider:performEndCallAction with UUID: %@", [action.callUUID UUIDString]);
    self.audioDevice.enabled = NO;

    // Pending Voice Call
    if (self.callInvite) {
        [self.callInvite reject];
        self.callInvite = nil;
    } else if (self.call) {
        [self.call disconnect];
    }

    // Pending Video Call
    NSString *mode = self.dictionaryPayload[@"mode"];
    if (self.callUuid == nil && [mode isEqualToString:@"video"]) {
        NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
        params[@"session"] = self.dictionaryPayload[@"session"];
        params[@"companyUuid"] = self.dictionaryPayload[@"companyUuid"];
        params[@"reservationSid"] = self.dictionaryPayload[@"reservationSid"];
        [RNEventEmitterHelper emitEventWithName:@"performEndVideoCall" andPayload:params];
    }
    self.callUuid = nil;

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
            NSLog(@"[IIMobile - RNTwilioVoiceClient] StartCallAction transaction request failed: %@", [error localizedDescription]);
            [RNEventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        } else {
            NSLog(@"[IIMobile - RNTwilioVoiceClient] StartCallAction transaction request successful");

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
            NSLog(@"[IIMobile - RNTwilioVoiceClient] Incoming call successfully reported with UUID: %@", [uuid UUIDString]);
        } else {
            NSLog(@"[IIMobile - RNTwilioVoiceClient] Failed to report incoming call successfully: %@.", [error localizedDescription]);
        }
    }];
}

- (void)performEndCallActionWithUUID:(NSUUID *)uuid {
    if (uuid == nil) {
        for (CXCall *call in self.callKitCallController.callObserver.calls) {
            CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:call.UUID];
            CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
            [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
                if (error) {
                    NSLog(@"[IIMobile - RNTwilioVoiceClient] EndCallAction transaction request failed: %@", [error localizedDescription]);
                } else {
                    NSLog(@"[IIMobile - RNTwilioVoiceClient] EndCallAction transaction request successful");
                }
            }];
        }
        return;
    }

    UIDevice *device = [UIDevice currentDevice];
    device.proximityMonitoringEnabled = NO;

    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:uuid];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];

    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
        if (error) {
            NSLog(@"[IIMobile - RNTwilioVoiceClient] EndCallAction transaction request failed for UUID %@: %@", [uuid UUIDString], [error localizedDescription]);
            if (uuid == nil) {
                for (CXCall *call in self.callKitCallController.callObserver.calls) {
                    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:call.UUID];
                    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
                    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
                        if (error) {
                            NSLog(@"[IIMobile - RNTwilioVoiceClient] EndCallAction transaction request failed: %@", [error localizedDescription]);

                        } else {
                            NSLog(@"[IIMobile - RNTwilioVoiceClient] EndCallAction transaction request successful");
                        }
                    }];
                }
                return;
            }
        } else {
            NSLog(@"[IIMobile - RNTwilioVoiceClient] EndCallAction transaction request successful");
        }
    }];

}

- (void)performVoiceCallWithUUID:(NSUUID *)uuid
                          client:(NSString *)client
                      completion:(void (^)(BOOL success))completionHandler {

    if (_token == nil) {
        [RNEventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": @"Invalid access token"}];
    } else {
        TVOConnectOptions *options = [TVOConnectOptions optionsWithAccessToken:_token
                                                                         block:^(TVOConnectOptionsBuilder *builder) {
                                                                             builder.params = _callParams;
                                                                             builder.uuid = uuid;
                                                                         }];

        self.call = [TwilioVoice connectWithOptions:options delegate:self];

        self.callKitCompletionCallback = completionHandler;
    }
}

- (void)performAnswerVoiceCallWithUUID:(NSUUID *)uuid
                            completion:(void (^)(BOOL success))completionHandler {
    self.call = [self.callInvite acceptWithDelegate:self];
    self.callInvite = nil;
    self.callKitCompletionCallback = completionHandler;
    self.callMode = @"voice";

    NSLog(@"[IIMobile - RNTwilioVoiceClient] sendPerformAnswerVoiceCallEvent called with UUID: %@", uuid.UUIDString);
    [RNEventEmitterHelper emitEventWithName:@"performAnswerVoiceCall" andPayload:@{@"voipPush": self.dictionaryPayload, @"uuid": uuid.UUIDString}];
    [RNEventEmitterHelper emitEventWithName:@"connectionDidConnect" andPayload:self.callParams];
}

- (void)performAnswerVideoCallWithUUID:(NSUUID *)uuid
                            completion:(void (^)(BOOL success))completionHandler {
    self.call = [self.callInvite acceptWithDelegate:self];
    self.callInvite = nil;
    self.callKitCompletionCallback = completionHandler;
    self.callMode = @"video";
    self.audioDevice.enabled = YES;

    NSLog(@"[IIMobile - RNTwilioVoiceClient] sendPerformAnswerVideoCallEvent called with UUID: %@", uuid.UUIDString);

    [RNEventEmitterHelper emitEventWithName:@"performAnswerVideoCall" andPayload:@{@"voipPush": self.dictionaryPayload, @"uuid": uuid.UUIDString}];
}

- (void)handleAppTerminateNotification {
    NSLog(@"[IIMobile - RNTwilioVoiceClient] handleAppTerminateNotification called");
    if (self.call) {
        NSLog(@"[IIMobile - RNTwilioVoiceClient] handleAppTerminateNotification disconnecting an active call");
        [self.call disconnect];
    }
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

@end
