//
//  RNTwilioChatClient.m
//  RN TwilioChat
//
//  Created Enrique Viard on 5/21/19.
//  Copyright © 2016 No Good Software Inc. All rights reserved.
//


#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>
#import "RNEventEmitterHelper.h"
#import <React/RCTBridgeModule.h>
#import "RNTwilioChatClient.h"

@import AVFoundation;
@import TwilioChatClient;

@interface RNTwilioChatClient() <TwilioChatClientDelegate>
#pragma mark - Twilio Chat Members
@property (strong, nonatomic) NSString *identity;
@property (strong, nonatomic) NSMutableOrderedSet *messages;
@property (strong, nonatomic) TCHChannel *channel;
@end

@implementation RNTwilioChatClient

@synthesize client;

static RNTwilioChatClient *sharedInstance = nil;

#pragma mark Singleton Methods
+ (id)sharedInstance {
    if (sharedInstance == nil) {
        static dispatch_once_t onceToken;
        dispatch_once(&onceToken, ^{
            sharedInstance = [self alloc];
        });
    }
    return sharedInstance;
}

-(id) init {
    return [RNTwilioChatClient sharedInstance];
}

RCT_EXPORT_MODULE()

RCT_REMAP_METHOD(createClient, token:(NSString*)token properties:(NSDictionary *)properties resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSLog(@"[IIMobile - RNTwilioChatClient] createClient with token: %@", token);
    [TwilioChatClient chatClientWithToken:token properties:nil delegate:self completion:^(TCHResult * _Nonnull result, TwilioChatClient * _Nullable chatClient) {
        if (chatClient) {
            self.client = chatClient;
            dispatch_async(dispatch_get_main_queue(), ^{
                NSLog(@"[IIMobile - RNTwilioChatClient] ChatClient successfully created");
                resolve(chatClient);
            });
        } else {
            reject(@"error", @"Create ChatClient failed", nil);
        }
    }];
}

RCT_REMAP_METHOD(sendMessage, message:(NSString*)message send_message_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] sendMessage called with message: %@", message);
    
    if (message == nil) {
        reject(@"Message cannot be null", nil, nil);
    } else {
        TCHMessageOptions *messageOptions = [[TCHMessageOptions new] withBody:message];
        [self.channel.messages sendMessageWithOptions:messageOptions completion:^(TCHResult * _Nonnull result, TCHMessage * _Nullable message) {
            if (result.isSuccessful) {
                NSLog(@"[IIMobile - RNTwilioChatClient] sendMessage: message '%@' sent", message);
                resolve(@"Message sent");
            } else {
                NSLog(@"[IIMobile - RNTwilioChatClient] sendMessage: message '%@' not sent", message);
                reject(@"error", @"Message not sent", nil);
            }
        }];
    }
}

RCT_REMAP_METHOD(joinChannel, channelSid:(NSString *)channelSid join_channel_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] joinChannel called with channelSid: %@", channelSid);
    [client.channelsList channelWithSidOrUniqueName:channelSid completion:^(TCHResult *result, TCHChannel *channel) {
        if (channel) {
            self.channel = channel;
            [self.channel joinWithCompletion:^(TCHResult *result) {
                NSLog(@"[IIMobile - RNTwilioChatClient] Joines %@ channel", channelSid);
                resolve(@{
                          @"sid": channel.sid,
                          @"uniqueName": channel.uniqueName,
                          @"friendlyName": channel.friendlyName
                          });
            }];
        } else {
            reject(@"error", @"Failed to join channel", nil);
        }
    }];
}

RCT_REMAP_METHOD(createChannel, uniqueName:(NSString *)uniqueName friendlyName:(NSString *) friendlyName type:(NSString *) type create_channel_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] createChannel called with type: %@ with uniqueName: %@", type, uniqueName);
    
    if (uniqueName != nil && friendlyName != nil && type != nil) {
        NSNumber *channelType = nil;
        if ([type isEqualToString:@"private"]) {
            channelType = @(TCHChannelTypePrivate);
        } else if ([type isEqualToString:@"public"]) {
            channelType = @(TCHChannelTypePublic);
        }
        
        NSDictionary *channelOptions = @{TCHChannelOptionFriendlyName: friendlyName,
                                         TCHChannelOptionType: channelType};
        
        [client.channelsList createChannelWithOptions:channelOptions
                                           completion:^(TCHResult *result, TCHChannel *channel) {
                                               if ([result isSuccessful]) {
                                                   self.channel = channel;
                                                   [self.channel joinWithCompletion:^(TCHResult *result) {
                                                       if ([result isSuccessful]) {
                                                           [self.channel setUniqueName:uniqueName completion:^(TCHResult *result) {
                                                               NSLog(@"[IIMobile - RNTwilioChatClient] createChannel: %@ channel created with unique name: %@", type, uniqueName);
                                                               resolve(@{
                                                                         @"sid": channel.sid,
                                                                         @"uniqueName": channel.uniqueName,
                                                                         @"friendlyName": channel.friendlyName
                                                                         });
                                                           }];
                                                       } else{
                                                           reject(@"error", @"createChannel: setUniqueName failed", result.error);
                                                       }
                                                   }];
                                               } else {
                                                   reject(@"error", @"createChannel: createChannelWithOptions failed", result.error);
                                               }
                                           }];
    } else {
        NSLog(@"[IIMobile - RNTwilioChatClient] createChannel: failed to create channel. Some parameters are null");
        reject(@"error", @"Failed to create channel. Some parameters are null", nil);
    }
}

RCT_REMAP_METHOD(getPublicChannels, lpublic_channels_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] getPublicChannels called");
    [[self.client channelsList] publicChannelDescriptorsWithCompletion:^(TCHResult *result,
                                                                         TCHChannelDescriptorPaginator *paginator) {
        if ([result isSuccessful]) {
            NSMutableArray *channels = [[NSMutableArray alloc] init];
            for (TCHChannelDescriptor *channel in paginator.items) {
                NSLog(@"[IIMobile - RNTwilioChatClient] getPublicChannels: %@", channel.friendlyName);
                [channels addObject:@{
                                      @"sid": channel.sid,
                                      @"uniqueName": channel.uniqueName,
                                      @"friendlyName": channel.friendlyName
                                      }];
            }
            resolve(channels);
        } else {
            reject(@"error", @"getPublicChannels failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getUserChannels, luser_channels_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] getUserChannels called");
    [[self.client channelsList] userChannelDescriptorsWithCompletion:^(TCHResult *result,
                                                                         TCHChannelDescriptorPaginator *paginator) {
        if ([result isSuccessful]) {
            NSMutableArray *channels = [[NSMutableArray alloc] init];
            for (TCHChannelDescriptor *channel in paginator.items) {
                NSLog(@"[IIMobile - RNTwilioChatClient] getUserChannels:Channel %@", channel.friendlyName);
                [channels addObject:@{
                                      @"sid": channel.sid,
                                      @"uniqueName": channel.uniqueName,
                                      @"friendlyName": channel.friendlyName
                                      }];
            }
            resolve(channels);
        } else {
            reject(@"error", @"getUserChannels failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getLastMessages, count: (nonnull NSNumber *)count last_messages_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages getLastMessagesWithCount:count.longValue
                                         completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                             if ([result isSuccessful]) {
                                                 NSMutableArray *jsonArray = [[NSMutableArray alloc] init];
                                                 for (TCHMessage *message in messages) {
                                                     if (message.body != nil && message.body != @"(null)") {
                                                         [jsonArray addObject: @{
                                                                                 @"sid": message.sid,
                                                                                 @"index": message.index,
                                                                                 @"author": message.author,
                                                                                 @"timeStamp": message.timestamp,
                                                                                 @"body": message.body
                                                                                }];
                                                     }
                                                 }
                                                 resolve(jsonArray);
                                             } else {
                                                 reject(@"error", @"getLastMessages failed", result.error);
                                             }
                                         }];
}

RCT_REMAP_METHOD(getMessagesBefore, index: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
[self.channel.messages getMessagesBefore:(index.intValue - 1)
                               withCount:count.longValue
                              completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                  if ([result isSuccessful]) {
                                      NSMutableArray *jsonArray = [[NSMutableArray alloc] init];
                                      for (TCHMessage *message in messages) {
                                          if (message.body != nil && message.body != @"(null)") {
                                              [jsonArray addObject: @{
                                                                      @"sid": message.sid,
                                                                      @"index": message.index,
                                                                      @"author": message.author,
                                                                      @"timeStamp": message.timestamp,
                                                                      @"body": message.body
                                                                      }];
                                          }
                                      }
                                      resolve(jsonArray);
                                  } else {
                                      reject(@"error", @"getMessagesBefore failed", result.error);
                                  }
                              }];
}

- (void) createGeneralChannel {
    NSDictionary *channelOptions = @{TCHChannelOptionFriendlyName: @"General Chat Channel",
                                     TCHChannelOptionType: @(TCHChannelTypePublic)};
    
    [client.channelsList createChannelWithOptions:channelOptions
                                       completion:^(TCHResult *result, TCHChannel *channel) {
                                           self.channel = channel;
                                           [self.channel joinWithCompletion:^(TCHResult *result) {
                                               [self.channel setUniqueName:@"general" completion:^(TCHResult *result) {
                                                   NSLog(@"[IIMobile - RNTwilioChatClient] General channel created");
                                               }];
                                           }];
                                       }];
}

- (NSString*) convertSyncStatusToString:(TCHClientSynchronizationStatus) status {
    NSString *result = nil;
    
    switch(status) {
        case TCHClientSynchronizationStatusStarted:
            result = @"TCHClientSynchronizationStatusStarted";
            break;
        case TCHClientSynchronizationStatusChannelsListCompleted:
            result = @"TCHClientSynchronizationStatusChannelsListCompleted";
            break;
        case TCHClientSynchronizationStatusCompleted:
            result = @"TCHClientSynchronizationStatusCompleted";
            break;
        case TCHClientSynchronizationStatusFailed:
            result = @"TCHClientSynchronizationStatusFailed";
            break;
    }
    return result;
}

#pragma mark RNTwilioChatClient Delegates

- (void)chatClient:(TwilioChatClient *)client synchronizationStatusUpdated:(TCHClientSynchronizationStatus)status {
    NSLog(@"[IIMobile - RNTwilioChatClient] synchronizationStatusUpdated with status: %@", [self convertSyncStatusToString:status]);
    
    [RNEventEmitterHelper emitEventWithName:@"synchronizationStatusUpdated" andPayload:@{@"status": [self convertSyncStatusToString:status]}];
}

- (void)chatClient:(TwilioChatClient *)client channel:(TCHChannel *)channel messageAdded:(TCHMessage *)message {
    NSLog(@"[IIMobile - RNTwilioChatClient] messageAdded");
    NSDictionary *messageJson = @{
                                  @"sid": message.sid,
                                  @"author": message.author,
                                  @"timpeStamp": message.timestamp,
                                  @"body": message.body
                                  };
    [RNEventEmitterHelper emitEventWithName:@"messageAdded" andPayload:messageJson];
}

+(BOOL)requiresMainQueueSetup {
    return NO;
}

@end
