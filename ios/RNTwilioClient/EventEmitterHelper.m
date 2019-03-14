//
//  EventEmitterHelper.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 11/16/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "EventEmitterHelper.h"

@implementation EventEmitterHelper

RCT_EXPORT_MODULE();

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
           @"displayIncomingCall",
           @"voipRemoteNotificationsRegistered",
           @"voipRemoteNotificationReceived"
           ];
}

- (id) init {
    self = [super init];
    if (!self) return nil;
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(emitEventInternal:)
                                                 name:@"event-emitted"
                                               object:nil];
    return self;
}

- (void) dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)emitEventInternal:(NSNotification *)notification {
  // We will receive the dictionary here - we now need to extract the name
  // and payload and throw the event
  NSArray *eventDetails = [notification.userInfo valueForKey:@"detail"];
  NSString *eventName = [eventDetails objectAtIndex:0];
  NSDictionary *eventData = [eventDetails objectAtIndex:1];
  
    if (eventName != nil) {
        [self sendEventWithName:eventName
                           body:eventData];
    }
}

+ (void)emitEventWithName:(NSString *)name andPayload:(NSDictionary *)payload
{
  // userInfo requires a dictionary so we wrap out name and payload into an array and stick
  // that into the dictionary with a key of 'detail'
    
    NSDictionary *eventDetail;
    
    if (payload != nil) {
        eventDetail = @{@"detail":@[name,payload]};
    }
    
    dispatch_async(dispatch_get_main_queue(),^{
        [[NSNotificationCenter defaultCenter] postNotificationName:@"event-emitted"
                                                            object:self
                                                          userInfo:eventDetail];
    });
}

+(BOOL)requiresMainQueueSetup {
    return NO;
}

@end
