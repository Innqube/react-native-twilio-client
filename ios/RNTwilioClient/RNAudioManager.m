//
//  RNLogHelper.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 6/8/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RNAudioManager.h"
#import <React/RCTLog.h>
#import <PushKit/PushKit.h>
#import <CallKit/CallKit.h>
#import "RNEventEmitterHelper.h"

@import AVFoundation;
@import PushKit;
@import CallKit;
@import UIKit;

@implementation RNAudioManager

static RNAudioManager *sharedInstance = nil;
BOOL *speakerEnabled;
AVAudioSessionMode sessionMode;

#pragma mark Singleton Methods
+ (id)sharedInstance {
    if (sharedInstance == nil) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            sharedInstance = [self alloc];
        });
        [sharedInstance listenForAudioRoutesChanges];
        [sharedInstance listenForInterruptions];
        [sharedInstance listenForMediaServicesReset];
        [sharedInstance listenForMediaServicesWereLost];
        sessionMode = AVAudioSessionModeVoiceChat;
    }
    return sharedInstance;
}

-(id) init {
    return [RNAudioManager sharedInstance];
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

RCT_EXPORT_MODULE();

- (void)listenForAudioRoutesChanges {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                              selector:@selector(handleAudioRouteChange:)
                                              name:AVAudioSessionRouteChangeNotification
                                              object:[AVAudioSession sharedInstance]];
    NSLog(@"[IIMobile - RNAudioManager][listenForAudioRoutesChanges]");
}

- (void)listenForInterruptions {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                              selector:@selector(handleInterruptions:)
                                              name:AVAudioSessionInterruptionNotification
                                              object:[AVAudioSession sharedInstance]];
    NSLog(@"[IIMobile - RNAudioManager][listenForInterruptions]");
}

- (void)listenForMediaServicesReset {
    NSLog(@"[IIMobile - RNAudioManager][listenForMediaServicesReset]");
    [[NSNotificationCenter defaultCenter]  addObserver:self
                                               selector:@selector(handleMediaServerReset:)
                                                   name:AVAudioSessionMediaServicesWereResetNotification
                                                 object:[AVAudioSession sharedInstance]];
}

- (void)listenForMediaServicesWereLost {
    NSLog(@"[IIMobile - RNAudioManager][listenForMediaServicesWereLost]");
    [[NSNotificationCenter defaultCenter]  addObserver:self
                                               selector:@selector(handleMediaServerServicesWereLost:)
                                                   name:AVAudioSessionMediaServicesWereLostNotification
                                                 object:[AVAudioSession sharedInstance]];
}

- (void)configureAudioSession {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    NSError *error;

    if (![session setCategory:AVAudioSessionCategoryPlayAndRecord
             withOptions:(AVAudioSessionCategoryOptionAllowBluetooth, AVAudioSessionCategoryOptionAllowBluetoothA2DP)
                   error:&error
          ]) {
        NSLog(@"[IIMobile - RNAudioManager][configureAudioSession] setCategory failed with error %@", [error debugDescription]);
    }

    if (![session setMode:sessionMode error:&error]) {
        NSLog(@"[IIMobile - RNAudioManager][configureAudioSession] setMode failed with error %@", [error debugDescription]);
    }

    if (![session setActive: YES error:&error]) {
        NSLog(@"[IIMobile - RNAudioManager][configureAudioSession] activate session failed with error %@", [error debugDescription]);
    }
}

- (AVAudioSessionPortDescription*)getAudioDeviceFromType:(NSString*)type {
    NSArray* inputs = [[AVAudioSession sharedInstance] availableInputs];
    for (AVAudioSessionPortDescription* port in inputs) {
        if ([type isEqualToString: port.portType]) {
            NSLog(@"[IIMobile - RNAudioManager][getAudioDeviceFromType] %@", port.portType);
            return port;
        }
    }
    return nil;
}

- (void)handleAudioRouteChange: (NSNotification *) notification {
    NSInteger routeChangeReason = [notification.userInfo[AVAudioSessionRouteChangeReasonKey] integerValue] || 0;
    AVAudioSession* session = [AVAudioSession sharedInstance];

    AVAudioSessionPortDescription *input = [[session.currentRoute.inputs count] ? session.currentRoute.inputs:nil objectAtIndex:0];
    AVAudioSessionPortDescription *output = [[session.currentRoute.outputs count] ? session.currentRoute.outputs:nil objectAtIndex:0];
    NSString *reason = [self getAudioChangeReason: routeChangeReason];

    AVAudioSessionRouteDescription *previousRoute = notification.userInfo[AVAudioSessionRouteChangePreviousRouteKey];
    AVAudioSessionPortDescription *previousInput = [[previousRoute.inputs count] ? previousRoute.inputs:nil objectAtIndex:0];
    AVAudioSessionPortDescription *previousOutput = [[previousRoute.outputs count] ? previousRoute.outputs:nil objectAtIndex:0];

    NSLog(@"[IIMobile - RNAudioManager][handleRouteChange] with reason %@ from INPUT %@ to %@", reason, previousInput.portType, input.portType);
    NSLog(@"[IIMobile - RNAudioManager][handleRouteChange] with reason %@ from OUTPUT %@ to %@", reason, previousOutput.portType, output.portType);

    if (input != nil) {
        if (!([input.portType isEqualToString:AVAudioSessionPortBuiltInMic] && speakerEnabled)) {
            [RNEventEmitterHelper emitEventWithName:@"audioRouteChanged"
                                         andPayload:@{
                                             @"reason": reason != nil ? reason : @"UNKNOWN",
                                             @"current": input.portType
                                         }];
       }
    } else {
        NSLog(@"[IIMobile - RNAudioManager][handleRouteChange] cannot change audio route: input cannot be null");
    }
}

- (void)handleInterruptions: (NSNotification *) notification {
    NSInteger interruptionType = [notification.userInfo[AVAudioSessionInterruptionTypeKey] integerValue] || 0;
    NSLog(@"[IIMobile - RNAudioManager][handleInterruptions] with type %ld ", interruptionType);
}

- (void)handleMediaServerReset: (NSNotification *) notification {
    NSLog(@"[IIMobile - RNAudioManager][handleMediaServerReset]");
    [self configureAudioSession];
}

- (void)handleMediaServerServicesWereLost: (NSNotification *) notification {
    NSLog(@"[IIMobile - RNAudioManager][handleMediaServerServicesWereLost]");
}

- (NSString*)getAudioChangeReason:(NSInteger) reason {
    NSLog(@"[IIMobile - RNAudioManager][getAudioChangeReason] %tu", reason);
    switch (reason) {
       case AVAudioSessionRouteChangeReasonUnknown:
           return @"UNKNOWN";
       case AVAudioSessionRouteChangeReasonNewDeviceAvailable:
           return @"NEW_DEVICE_AVAILABLE";
       case AVAudioSessionRouteChangeReasonOldDeviceUnavailable:
           return @"OLD_DEVICE_UNAVAILABLE";
       case AVAudioSessionRouteChangeReasonCategoryChange:
           return @"CATEGORY_CHANGE";
       case AVAudioSessionRouteChangeReasonOverride:
           return @"OVERRIDE";
       case AVAudioSessionRouteChangeReasonWakeFromSleep:
           return @"WAKE_FROM_SLEEP";
       case AVAudioSessionRouteChangeReasonNoSuitableRouteForCategory:
           return @"NO_SUITABLE_ROUTE";
       case AVAudioSessionRouteChangeReasonRouteConfigurationChange:
            return @"CONFIGURATION_CHANGE";
       default:
            return @"UNKNOWN";
       }
}

#pragma mark - AVAudioSession

- (void)toggleAudioRoute:(BOOL *)toSpeaker {
    NSError *error = nil;
    NSLog(@"[IIMobile - RNAudioManager] toggleAudioRoute");
    speakerEnabled = toSpeaker;

    if (toSpeaker) {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNAudioManager] Unable to reroute audio: %@", [error debugDescription]);
        }
    } else {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideNone
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNAudioManager] Unable to reroute audio: %@", [error debugDescription]);
        }
    }
}


RCT_REMAP_METHOD(getAvailableAudioInputs, devicesResolver: (RCTPromiseResolveBlock)resolve rej:(RCTPromiseRejectBlock)reject) {
    AVAudioSession* session = [AVAudioSession sharedInstance];
    NSMutableDictionary *types = [[NSMutableDictionary alloc] init];
    AVAudioSessionPortDescription *input = [[session.currentRoute.inputs count] ? session.currentRoute.inputs:nil objectAtIndex:0];
    AVAudioSessionRouteDescription *currentRoute = [session currentRoute];
    BOOL wiredHeadsetPresent = NO;
    NSLog(@"[IIMobile - RNAudioManager][getAvailableAudioInputs] currentRoute inputs: %ld", [currentRoute.inputs count]);


    for (AVAudioSessionPortDescription* port in session.availableInputs) {
        NSMutableDictionary *type = [[NSMutableDictionary alloc] init];
        type[@"enabled"] = [NSNumber numberWithBool: [input.portType isEqualToString:port.portType]];
        type[@"name"] = port.portName;
        types[port.portType] = type;

        if ([port.portType isEqualToString:AVAudioSessionPortHeadsetMic]) {
            wiredHeadsetPresent = YES;
        }

        NSLog(@"[IIMobile - RNAudioManager][getAvailableAudioInputs] input %@", port.portType);
    }

    if (wiredHeadsetPresent == YES) {
        [types removeObjectForKey:AVAudioSessionPortBuiltInMic];
    }


    NSArray* outputs = [currentRoute outputs];
    NSNumber* enabled = @FALSE;
    for (AVAudioSessionPortDescription *output in outputs) {
        if ([output.portType isEqualToString:AVAudioSessionPortBuiltInSpeaker]) {
            enabled = @TRUE;
        }
    }

    NSMutableDictionary *speakerType = [[NSMutableDictionary alloc] init];
    speakerType[@"enabled"] = enabled;
    speakerType[@"name"] = AVAudioSessionPortBuiltInSpeaker;
    types[AVAudioSessionPortBuiltInSpeaker] = speakerType;

    resolve(types);
}

RCT_EXPORT_METHOD(switchAudioInput: (NSString *) portType switchResolver: (RCTPromiseResolveBlock)resolve rej:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNAudioManager][switchAudioInput:portType] %@", portType);
    if (portType == nil) {
        reject(@"error", @"portName is required", nil);
    }
    BOOL enableSpeaker = [portType isEqualToString: AVAudioSessionPortBuiltInSpeaker];
    [self toggleAudioRoute:&enableSpeaker];

    if (enableSpeaker) {
        resolve(portType);
    } else {
        AVAudioSession* session = [AVAudioSession sharedInstance];
        AVAudioSessionPortDescription* newPort = [self getAudioDeviceFromType:portType];
        NSError* error;

        if (speakerEnabled) {
            [self toggleAudioRoute:FALSE];
        }

        [session setPreferredInput:newPort error: &error];
        if (error != nil) {
            NSLog(@"[IIMobile - RNAudioManager][setPreferredInput] failed with error %@", [error debugDescription]);
            reject(@"error", @"setPreferredInput failed with error", error);
        }

        AVAudioSessionPortDescription *input = [[session.currentRoute.inputs count] ? session.currentRoute.inputs:nil objectAtIndex:0];
        resolve(input.portType);
    }
}

RCT_EXPORT_METHOD(configure: (NSString*) mode) {
    if ([mode isEqualToString:@"video"]) {
        sessionMode = AVAudioSessionModeVideoChat;
    } else {
        sessionMode = AVAudioSessionModeVoiceChat;
    }

    [self configureAudioSession];
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
