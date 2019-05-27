//
//  RNTwilioClient.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard.
//  Copyright © 2018 No Good Software Inc. All rights reserved.
//


#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <PushKit/PushKit.h>

@interface RNTwilioClient: NSObject<RCTBridgeModule>
-(void)didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type withState:(NSString *)pending;
-(void)didUpdatePushCredentials:(PKPushCredentials *)credentials forType:(NSString *)type;
-(void)initPushRegistry;
+(id)sharedInstance;
@end
