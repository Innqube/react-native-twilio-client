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

#pragma mark Singleton Methods
+ (id)sharedInstance {
    if (sharedInstance == nil) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            sharedInstance = [self alloc];
        });
        [sharedInstance configureAudioSession];
        [sharedInstance listenForAudioRoutesChanges];
    }
    return sharedInstance;
}

-(id) init {
    return [RNAudioManager sharedInstance];
}

RCT_EXPORT_MODULE();

- (void)listenForAudioRoutesChanges {
    AVAudioSession* session = [AVAudioSession sharedInstance];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                              selector:@selector(handleAudioRouteChange:)
                                              name:AVAudioSessionRouteChangeNotification
                                              object:session];
    NSLog(@"[IIMobile - RNAudioManager][listenForAudioRoutesChanges]");
}

- (void)configureAudioSession {
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryMultiRoute error:nil];
    [[AVAudioSession sharedInstance] setMode:AVAudioSessionModeVideoChat error:nil];
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
    NSString *reason = [self getAudioChangeReason: routeChangeReason];
    NSLog(@"[IIMobile - RNAudioManager][handleRouteChange] with reason %@ to %@", reason, input.portType);

    if (input != nil) {
        if (![input.portType isEqualToString:AVAudioSessionPortBuiltInMic]) {
            [self toggleAudioRoute:false];
        }

        if (!([input.portType isEqualToString:AVAudioSessionPortBuiltInMic] && speakerEnabled)) {
            [RNEventEmitterHelper emitEventWithName:@"audioRouteChanged"
                                         andPayload:@{
                                             @"reason": reason != nil ? reason : @"UNKNOWN",
                                             @"current": input.portType
                                         }];
       }
    } else {
        NSLog(@"[IIMobile - RNAudioManager][handleRouteChange] canoot change audio route: input cannot be null");
    }
}

- (NSString*)getAudioChangeReason:(NSInteger) reason {
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
            NSLog(@"[IIMobile - RNAudioManager] Unable to reroute audio: %@", [error localizedDescription]);
        }
    } else {
        if (![[AVAudioSession sharedInstance] overrideOutputAudioPort:AVAudioSessionPortOverrideNone
                                                                error:&error]) {
            NSLog(@"[IIMobile - RNAudioManager] Unable to reroute audio: %@", [error localizedDescription]);
        }
    }
}


RCT_REMAP_METHOD(getAvailableAudioInputs, devicesResolver: (RCTPromiseResolveBlock)resolve rej:(RCTPromiseRejectBlock)reject) {
    AVAudioSession* session = [AVAudioSession sharedInstance];
    NSArray* inputs = [session availableInputs];
    NSMutableDictionary *types = [[NSMutableDictionary alloc] init];
    AVAudioSessionPortDescription *input = [[session.currentRoute.inputs count] ? session.currentRoute.inputs:nil objectAtIndex:0];
    BOOL wiredHeadsetPresent = NO;

    for (AVAudioSessionPortDescription* port in inputs) {
        NSMutableDictionary *type = [[NSMutableDictionary alloc] init];
        type[@"enabled"] = [NSNumber numberWithBool: [input.portType isEqualToString:port.portType]];
        type[@"name"] = port.portName;
        types[port.portType] = type;

        if ([port.portType isEqualToString:AVAudioSessionPortHeadsetMic]) {
            wiredHeadsetPresent = YES;
        }

        NSLog(@"[IIMobile - RNAudioManager][getAvailableAudioInputs:input] %@", port.portType);
    }

    if (wiredHeadsetPresent == YES) {
        [types removeObjectForKey:AVAudioSessionPortBuiltInMic];
    }

    AVAudioSessionRouteDescription *currentRoute = [[AVAudioSession sharedInstance] currentRoute];
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
    BOOL speakerEnabled = [portType isEqualToString: AVAudioSessionPortBuiltInSpeaker];
    [self toggleAudioRoute:&speakerEnabled];

    if (speakerEnabled) {
        resolve(portType);
    } else {
        AVAudioSession* session = [AVAudioSession sharedInstance];
        AVAudioSessionPortDescription* newPort = [self getAudioDeviceFromType:portType];
        NSError* error;

        if (![session setPreferredInput:newPort error: &error]) {
            reject(@"error", @"setPreferredInput failed with error", error);
        }

        AVAudioSessionPortDescription *input = [[session.currentRoute.inputs count] ? session.currentRoute.inputs:nil objectAtIndex:0];
        resolve(input.portType);
    }
}

@end
