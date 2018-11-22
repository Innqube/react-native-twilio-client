//
//  RNTwilioClient.m
//

#import "RNTwilioClient.h"
#import <React/RCTLog.h>
#import <PushKit/PushKit.h>
#import <CallKit/CallKit.h>
#import "EventEmitterHelper.h"

@import AVFoundation;
@import PushKit;
@import CallKit;
@import TwilioVoice;


@interface RNTwilioClient () <PKPushRegistryDelegate, TVONotificationDelegate, TVOCallDelegate, CXProviderDelegate>
@property(nonatomic, strong) NSString *deviceTokenString;
@property(nonatomic, strong) NSDictionary *dictionaryPayload;
@property(nonatomic, strong) NSString *pending;
@property(nonatomic, strong) TVOCallInvite *callInvite;
@property(nonatomic, strong) TVOCall *call;
@property(nonatomic, strong) NSUUID *callUuid;
@property(nonatomic, strong) NSString *callMode;
@property(nonatomic, strong) NSMutableDictionary *callParams;
@property(nonatomic, strong) PKPushRegistry *voipRegistry;
@property(nonatomic, strong) void (^callKitCompletionCallback)(BOOL);
@property(nonatomic, strong) CXProvider *callKitProvider;
@property(nonatomic, strong) CXCallController *callKitCallController;
@end

@implementation RNTwilioClient {
    NSMutableDictionary *_settings;
    NSMutableDictionary *_callParams;
    NSString *_tokenUrl;
    NSString *_token;
}

static double inactiveDelay = 3.0;
static RNTwilioClient *sharedInstance = nil;

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


-(id) init {
    if (sharedInstance == nil) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            sharedInstance = self;
        });

        [self configureCallKit];
    }
    return sharedInstance;
}

- (id)initPrivate {
    return self;
}

- (void)dealloc {
    if (self.callKitProvider) {
        [self.callKitProvider invalidate];
    }

    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

NSString *const StatePending = @"PENDING";
NSString *const StateConnecting = @"CONNECTING";
NSString *const StateConnected = @"CONNECTED";
NSString *const StateDisconnected = @"DISCONNECTED";
NSString *const StateRejected = @"REJECTED";

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents {
    return @[
            @"connectionDidConnect",
            @"connectionDidDisconnect",
            @"callRejected",
            @"deviceReady",
            @"deviceNotReady",
            @"performAnswerVoiceCall",
            @"performAnswerVideoCall",
            @"performEndVideoCall",
            @"requestTransactionError",
            @"voipRemoteNotificationsRegistered"
    ];
}

RCT_EXPORT_METHOD(sendMessage:
                  (NSString *) message) {
    if (message != nil) {
        NSLog(@"[RNTwilioClient][Message from ReactNative] %@", message);
    } else {
        NSLog(@"[RNTwilioClient][Message from ReactNative] NULL");
    }
}

RCT_EXPORT_METHOD(deviceReadyForCalls) {
    NSLog(@"[RNTwilioClient] deviceReadyForCalls. Pending call: %@ and UUID: %@", self.pending, self.callUuid.UUIDString);
    if (self.callUuid && self.pending != nil && [self.pending isEqualToString:@"true"]) {
        if ([self.callMode isEqualToString:@"video"]) {
            [self sendPerformAnswerVideoCallEvent:self.callUuid];
        } else if ([self.callMode isEqualToString:@"voice"]) {
            [self sendPerformAnswerVoiceCallEvent:self.callUuid];
        }
    }

    self.pending = @"false";
}

RCT_EXPORT_METHOD(initWithAccessToken:
(NSString *) token) {
    _token = token;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleAppTerminateNotification) name:UIApplicationWillTerminateNotification object:nil];
    [self initPushRegistry];
}

RCT_EXPORT_METHOD(connect:
(NSDictionary *) params) {
    NSLog(@"[RNTwilioClient][connect] Calling phone number %@", [params valueForKey:@"To"]);

    UIDevice *device = [UIDevice currentDevice];
    device.proximityMonitoringEnabled = YES;

    if (self.call && self.call.state == TVOCallStateConnected) {
        [self.call disconnect];
    } else {
        NSUUID *uuid = [NSUUID UUID];
        NSString *handle = [params valueForKey:@"To"];
        _callParams = [[NSMutableDictionary alloc] initWithDictionary:params];
        [self performStartCallActionWithUUID:uuid handle:handle];
    }
}

RCT_EXPORT_METHOD(disconnect:
(NSString *) uuidStr) {
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidStr];
    NSLog(@"[RNTwilioClient][disconnect] Disconnecting call with UUID: %@", uuidStr);
    [self performEndCallActionWithUUID:(uuid != nil ? uuid : self.call.uuid)];
}

RCT_EXPORT_METHOD(setMuted:
(BOOL *) muted) {
    NSLog(@"[RNTwilioClient] setMuted");
    self.call.muted = (BOOL) muted;
}

RCT_EXPORT_METHOD(setSpeakerPhone:
(BOOL *) speaker) {
    NSLog(@"[RNTwilioClient] setSpeakerPhone");
    [self toggleAudioRoute:speaker];
}

RCT_EXPORT_METHOD(sendDigits:
(NSString *) digits) {
    if (self.call && self.call.state == TVOCallStateConnected) {
        NSLog(@"[RNTwilioClient][sendDigits] %@", digits);
        [self.call sendDigits:digits];
    }
}

RCT_EXPORT_METHOD(unregister) {
        NSLog(@"[RNTwilioClient][unregister]");
        NSString *accessToken = [self fetchAccessToken];

        [TwilioVoice unregisterWithAccessToken:accessToken
        deviceToken:self.deviceTokenString
        completion:^(NSError *_Nullable error) {
            if (error) {
                NSLog(@"[RNTwilioClient] An error occurred while unregistering: %@", [error localizedDescription]);
            } else {
                NSLog(@"[RNTwilioClient] Successfully unregistered for VoIP push notifications.");
            }
        }];

        self.deviceTokenString = nil;
}

RCT_REMAP_METHOD(getActiveCall,
        resolver:
(RCTPromiseResolveBlock) resolve
        rejecter:
(RCTPromiseRejectBlock) reject) {
    NSLog(@"[RNTwilioClient][getActiveCall]");
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
        if (self.callInvite.state == TVOCallInviteStatePending) {
            params[@"call_state"] = StatePending;
        } else if (self.callInvite.state == TVOCallInviteStateCanceled) {
            params[@"call_state"] = StateDisconnected;
        } else if (self.callInvite.state == TVOCallInviteStateRejected) {
            params[@"call_state"] = StateRejected;
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
    NSDictionary *options = @{@"appName" : @"Interpreter Intelligence", @"imageName" : @"ii-logo", @"ringtoneSound": @"incoming.mp3"};
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

- (NSString *)fetchAccessToken {
    if (_tokenUrl) {
        NSString *accessToken = [NSString stringWithContentsOfURL:[NSURL URLWithString:_tokenUrl]
                                                         encoding:NSUTF8StringEncoding
                                                            error:nil];
        return accessToken;
    } else {
        return _token;
    }
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
    NSLog(@"[RNTwilioClient][getProviderConfiguration]");
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
   NSLog(@"[RNTwilioClient][didUpdatePushCredentials][DeviceToken: %@]", self.deviceTokenString);
   [self didUpdatePushCredentials:credentials
                          forType:type];
}

- (void)didUpdatePushCredentials:(PKPushCredentials *)credentials forType:(NSString *)type {
    self.deviceTokenString = [credentials.token description];

    if ([type isEqualToString:PKPushTypeVoIP]) {
        // Twilio Video registration event
        [EventEmitterHelper emitEventWithName:@"voipRemoteNotificationsRegistered" andPayload:@{@"token":self.deviceTokenString}];

        NSLog(@"[RNTwilioClient] didUpdatePushCredentials. DeviceToken: %@", self.deviceTokenString);

        // Twilio Voice registration
        NSString *accessToken = [self fetchAccessToken];

        NSLog(@"[RNTwilioClient] didUpdatePushCredentials. AccessToken: %@", accessToken);

        [TwilioVoice registerWithAccessToken:accessToken
                                 deviceToken:self.deviceTokenString
                                  completion:^(NSError *error) {
                                      if (error) {
                                          NSLog(@"[RNTwilioClient] An error occurred while registering: %@", [error localizedDescription]);
                                          NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
                                          params[@"err"] = [error localizedDescription];

                                          [EventEmitterHelper emitEventWithName:@"deviceNotReady" andPayload:params];
                                      } else {
                                          NSLog(@"[RNTwilioClient] Successfully registered for VoIP push notifications.");
                                          [EventEmitterHelper emitEventWithName:@"deviceReady" andPayload:nil];
                                      }
                                  }];

    }
}

- (void)pushRegistry:(PKPushRegistry *)registry didInvalidatePushTokenForType:(PKPushType)type {
    NSLog(@"[RNTwilioClient][didInvalidatePushTokenForType]");
    if ([type isEqualToString:PKPushTypeVoIP]) {
        NSString *accessToken = [self fetchAccessToken];

        [TwilioVoice unregisterWithAccessToken:accessToken
                                   deviceToken:self.deviceTokenString
                                    completion:^(NSError *_Nullable error) {
                                        if (error) {
                                            NSLog(@"[RNTwilioClient] An error occurred while unregistering: %@", [error localizedDescription]);
                                        } else {
                                            NSLog(@"[RNTwilioClient] Successfully unregistered for VoIP push notifications.");
                                        }
                                    }];

        self.deviceTokenString = nil;
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type {
    NSLog(@"[RNTwilioClient][didReceiveIncomingPushWithPayload]");
    [self didReceiveIncomingPushWithPayload:payload
                                    forType:type
                                    withState:@"false"];
}

- (void)didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type withState:(NSString *)pending {
    NSLog(@"[RNTwilioClient][didReceiveIncomingPushWithPayload] payload %@", payload.dictionaryPayload);

    NSString *mode = payload.dictionaryPayload[@"mode"];
    self.dictionaryPayload = payload.dictionaryPayload;
    self.pending = pending;

    if ([type isEqualToString:PKPushTypeVoIP] && [mode isEqualToString:@"video"]) {
        NSLog(@"[RNTwilioClient] VOIP_VIDEO_NOTIF: didReceiveIncomingPushWithPayload: %@", payload);
        NSLog(@"[RNTwilioClient][displayIncomingCall] uuidString = %@", payload.dictionaryPayload[@"session"]);
        int _handleType = [self getHandleType:@"generic"];
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:payload.dictionaryPayload[@"session"]];
        CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
        callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:payload.dictionaryPayload[@"displayName"]];
        callUpdate.supportsDTMF = YES;
        callUpdate.supportsHolding = NO;
        callUpdate.supportsGrouping = NO;
        callUpdate.supportsUngrouping = NO;
        callUpdate.hasVideo = true;
        callUpdate.localizedCallerName = payload.dictionaryPayload[@"displayName"];

        [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *_Nullable error) {
            [EventEmitterHelper emitEventWithName:@"displayIncomingCall" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        }];
    } else {
        NSLog(@"[RNTwilioClient] VOIP_VOICE_NOTIF: didReceiveIncomingPushWithPayload: %@", payload);
        [TwilioVoice handleNotification:payload.dictionaryPayload
                               delegate: self];
    }
}

#pragma mark - TVONotificationDelegate
- (void)callInviteReceived:(TVOCallInvite *)callInvite {
    if (callInvite.state == TVOCallInviteStatePending) {
        [self handleCallInviteReceived:callInvite];
    } else if (callInvite.state == TVOCallInviteStateCanceled) {
        [self handleCallInviteCanceled:callInvite];
    }
}

- (void)handleCallInviteReceived:(TVOCallInvite *)callInvite {
    NSLog(@"[RNTwilioClient] callInviteReceived with UUID: %@", [callInvite.uuid UUIDString]);
    if (self.callInvite && self.callInvite == TVOCallInviteStatePending) {
        NSLog(@"[RNTwilioClient] Already a pending incoming call invite.");
        NSLog(@"[RNTwilioClient]   >> Ignoring call from %@", callInvite.from);
        return;
    } else if (self.call) {
        NSLog(@"[RNTwilioClient] Already an active call.");
        NSLog(@"[RNTwilioClient]   >> Ignoring call from %@", callInvite.from);
        return;
    }

    self.callInvite = callInvite;

    [self reportIncomingCallFrom:callInvite.from withUUID:callInvite.uuid];
}

- (void)handleCallInviteCanceled:(TVOCallInvite *)callInvite {
    NSLog(@"[RNTwilioClient] callInviteCanceled");

    [self performEndCallActionWithUUID:callInvite.uuid];

    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    if (self.callInvite.callSid) {
        params[@"call_sid"] = self.callInvite.callSid;
    }
    if (self.callInvite.from) {
        params[@"from"] = self.callInvite.from;
    }
    if (self.callInvite.to) {
        params[@"to"] = self.callInvite.to;
    }
    if (self.callInvite.state == TVOCallInviteStateCanceled) {
        params[@"call_state"] = StateDisconnected;
    } else if (self.callInvite.state == TVOCallInviteStateRejected) {
        params[@"call_state"] = StateRejected;
    }
    [EventEmitterHelper emitEventWithName:@"connectionDidDisconnect" andPayload:params];

    self.callInvite = nil;
}

- (void)notificationError:(NSError *)error {
    NSLog(@"[RNTwilioClient] notificationError: %@", [error localizedDescription]);
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

    if (![self.pending isEqualToString:@"true"]) {
        [EventEmitterHelper emitEventWithName:@"connectionDidConnect" andPayload:callParams];
    }

}

- (void)call:(TVOCall *)call didFailToConnectWithError:(NSError *)error {
    NSLog(@"[RNTwilioClient] Call failed to connect: %@", error);
    self.callKitCompletionCallback(NO);
    [self performEndCallActionWithUUID:call.uuid];
    [self callDisconnected:error];
}

- (void)call:(TVOCall *)call didDisconnectWithError:(NSError *)error {
    NSLog(@"[RNTwilioClient] Call disconnected with error: %@", error);
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
    [EventEmitterHelper emitEventWithName:@"connectionDidDisconnect" andPayload:params];

    self.call = nil;
    self.callKitCompletionCallback = nil;
}

#pragma mark - AVAudioSession
- (void)toggleAudioRoute:(BOOL *)toSpeaker {
    // The mode set by the Voice SDK is "VoiceChat" so the default audio route is the built-in receiver.
    // Use port override to switch the route.
    NSError *error = nil;
    NSLog(@"[RNTwilioClient] toggleAudioRoute");

    if (toSpeaker) {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker
                                                                error:&error]) {
            NSLog(@"[RNTwilioClient] Unable to reroute audio: %@", [error localizedDescription]);
        }
    } else {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideNone
                                                                error:&error]) {
            NSLog(@"[RNTwilioClient] Unable to reroute audio: %@", [error localizedDescription]);
        }
    }
}

#pragma mark - CXProviderDelegate
- (void)providerDidReset:(CXProvider *)provider {
    NSLog(@"[RNTwilioClient] providerDidReset");
    TwilioVoice.audioEnabled = YES;
}

- (void)providerDidBegin:(CXProvider *)provider {
    NSLog(@"[RNTwilioClient] providerDidBegin");
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession {
    NSLog(@"[RNTwilioClient] provider:didActivateAudioSession");
    TwilioVoice.audioEnabled = YES;
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession {
    NSLog(@"[RNTwilioClient] provider:didDeactivateAudioSession");
    TwilioVoice.audioEnabled = NO;
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action {
    NSLog(@"[RNTwilioClient] provider:timedOutPerformingAction");
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    NSLog(@"[RNTwilioClient] provider:performStartCallAction");

    [TwilioVoice configureAudioSession];
    TwilioVoice.audioEnabled = NO;

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
    NSLog(@"[RNTwilioClient] provider:performAnswerCallAction wioth UUID: %@", [action.callUUID UUIDString]);
    // Should end all calls?
    //NSAssert([self.callInvite.uuid isEqual:action.callUUID], @"We only support one Invite at a time.");

    NSString *mode = self.dictionaryPayload[@"mode"];
    TwilioVoice.audioEnabled = NO;
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
    NSLog(@"[RNTwilioClient] provider:performEndCallAction with UUID: %@", [action.callUUID UUIDString]);
    TwilioVoice.audioEnabled = NO;

    if (self.callInvite && self.callInvite.state == TVOCallInviteStatePending) {
        [EventEmitterHelper emitEventWithName:@"callRejected" andPayload:nil];
        [self.callInvite reject];
        self.callInvite = nil;
    } else if (self.call) {
        [self.call disconnect];
    }

    NSString *mode = self.dictionaryPayload[@"mode"];
    if ([mode isEqualToString:@"video"]) {
        [EventEmitterHelper emitEventWithName:@"performEndVideoCall" andPayload:nil];
    }

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
- (void)performStartCallActionWithUUID:(NSUUID *)uuid handle:(NSString *)handle {
    if (uuid == nil || handle == nil) {
        return;
    }

    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:callHandle];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];

    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
        if (error) {
            NSLog(@"[RNTwilioClient] StartCallAction transaction request failed: %@", [error localizedDescription]);
            [EventEmitterHelper emitEventWithName:@"requestTransactionError" andPayload:@{@"error": error ? error.localizedDescription : @""}];
        } else {
            NSLog(@"[RNTwilioClient] StartCallAction transaction request successful");

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
            NSLog(@"[RNTwilioClient] Incoming call successfully reported with UUID: %@", [uuid UUIDString]);

            // RCP: Workaround per https://forums.developer.apple.com/message/169511
            [TwilioVoice configureAudioSession];
        } else {
            NSLog(@"[RNTwilioClient] Failed to report incoming call successfully: %@.", [error localizedDescription]);
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
                    NSLog(@"[RNTwilioClient] EndCallAction transaction request failed: %@", [error localizedDescription]);
                } else {
                    NSLog(@"[RNTwilioClient] EndCallAction transaction request successful");
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
            NSLog(@"[RNTwilioClient] EndCallAction transaction request failed for UUID %@: %@", [uuid UUIDString], [error localizedDescription]);
        } else {
            NSLog(@"[RNTwilioClient] EndCallAction transaction request successful");
        }
    }];
}

- (void)performVoiceCallWithUUID:(NSUUID *)uuid
                          client:(NSString *)client
                      completion:(void (^)(BOOL success))completionHandler {

    self.call = [TwilioVoice call:[self fetchAccessToken]
                           params:_callParams
                             uuid:uuid
                         delegate:self];

    self.callKitCompletionCallback = completionHandler;
}

- (void)performAnswerVoiceCallWithUUID:(NSUUID *)uuid
                            completion:(void (^)(BOOL success))completionHandler {
    self.call = [self.callInvite acceptWithDelegate:self];
    self.callInvite = nil;
    self.callKitCompletionCallback = completionHandler;
    self.callMode = @"voice";

    if (![self.pending isEqualToString:@"true"]) {
        [self sendPerformAnswerVoiceCallEvent:uuid];
    }
}

- (void)performAnswerVideoCallWithUUID:(NSUUID *)uuid
                            completion:(void (^)(BOOL success))completionHandler {
    self.call = [self.callInvite acceptWithDelegate:self];
    self.callInvite = nil;
    self.callKitCompletionCallback = completionHandler;
    self.callMode = @"video";

    if (![self.pending isEqualToString:@"true"]) {
        [self sendPerformAnswerVideoCallEvent:uuid];
    }
}

- (void)sendPerformAnswerVideoCallEvent:(NSUUID *)uuid {
    NSLog(@"[RNTwilioClient] sendPerformAnswerVideoCallEvent called with UUID: %@", uuid.UUIDString);
    [EventEmitterHelper emitEventWithName:@"performAnswerVideoCall" andPayload:@{@"voipPush": self.dictionaryPayload, @"uuid": uuid.UUIDString} ];
}

- (void)sendPerformAnswerVoiceCallEvent:(NSUUID *)uuid {
    NSLog(@"[RNTwilioClient] sendPerformAnswerVoiceCallEvent called with UUID: %@", uuid.UUIDString);
    [EventEmitterHelper emitEventWithName:@"performAnswerVoiceCall" andPayload:@{@"voipPush": self.dictionaryPayload, @"uuid": uuid.UUIDString}];
    [EventEmitterHelper emitEventWithName:@"connectionDidConnect" andPayload:self.callParams];
}

- (void)handleAppTerminateNotification {
    NSLog(@"[RNTwilioClient] handleAppTerminateNotification called");
    if (self.call) {
        NSLog(@"[RNTwilioClient] handleAppTerminateNotification disconnecting an active call");
        [self.call disconnect];
    }
}

@end
