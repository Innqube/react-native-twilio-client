//
//  RNTwilioChatClient.h
//  RN TwilioChat
//
//  Created Enrique Viard on 9/17/19.
//  Copyright © 2019 No Good Software Inc. All rights reserved.
//

#import <React/RCTBridgeModule.h>
#import <TwilioChatClient/TCHChannels.h>
#import <TwilioChatClient/TCHChannel.h>

@interface RNTwilioChatChannels : NSObject <RCTBridgeModule>

+ (void)loadChannelFromSidOrUniqueName:(NSString *)sid :(void (^)(TCHResult *result, TCHChannel *channel))completion;

@end
