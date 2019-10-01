//
//  RNTwilioChatClient.h
//  RN TwilioChat
//
//  Created Enrique Viard on 5/21/19.
//  Copyright © 2016 No Good Software Inc. All rights reserved.
//

#import <React/RCTBridgeModule.h>

@import TwilioChatClient;

@interface RNTwilioChatClient : NSObject <RCTBridgeModule> {
    TwilioChatClient *client;
}

@property (nonatomic, retain) TwilioChatClient *client;
@property (nonatomic, retain) NSData *deviceToken;

+ (id)sharedInstance;
- (void)handleNotification:(NSDictionary *)userInfo;

@end
