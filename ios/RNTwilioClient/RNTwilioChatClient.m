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
            NSLog(@"[IIMobile - RNTwilioChatClient] createClient failed with error %@", result.error);
            reject(@"error", @"Create ChatClient failed", nil);
        }
    }];
}

RCT_REMAP_METHOD(updateClient, updatedToken:(NSString*)token resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){

    [self.client updateToken:token
                 completion:^(TCHResult * _Nonnull result) {
                     if (result.isSuccessful) {
                         NSLog(@"[IIMobile - RNTwilioChatClient] ChatClient successfully updated");
                     } else {
                         NSLog(@"[IIMobile - RNTwilioChatClient] ChatClient update failed with error %@", result.error);
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
                NSLog(@"[IIMobile - RNTwilioChatClient] sendMessage: message '%@' not sent with error", message, result.error);
                reject(@"error", @"Message not sent", nil);
            }
        }];
    }
}

RCT_REMAP_METHOD(joinChannel, uniqueName:(NSString *)uniqueName friendlyName:(NSString *)friendlyName type:(NSString *)type join_channel_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] joinChannel called with uniqueName: %@", uniqueName);

    [self.client.channelsList channelWithSidOrUniqueName:uniqueName completion:^(TCHResult *result, TCHChannel *channel) {
        if (channel) {
            [self joinChannel:channel resolver:resolve rejecter:reject];
        } else {
            [self createChannel:uniqueName withFriendlyName:friendlyName andType:type resolver:resolve rejecter:reject];
        }
    }];
}
/*
- (BOOL)isMe:(TCHMember *)member {


    return ([[member identity] isEqualToString:[[[[ChatManager sharedManager] client] user] identity]]);
}*/

- (void) joinChannel:(TCHChannel *)channel resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject {
    self.channel = channel;

    [self.channel joinWithCompletion:^(TCHResult *result) {
        if (result.isSuccessful) {
            NSLog(@"[IIMobile - RNTwilioChatClient] joinChannel: Joines %@ channel", channel.uniqueName);
            resolve(@{
                    @"sid": channel.sid,
                    @"uniqueName": channel.uniqueName,
                    @"friendlyName": channel.friendlyName
                    });
        } else if (result.error.code == 50404) {
            NSLog(@"[IIMobile - RNTwilioChatClient] joinChannel: Member already exists");
            resolve(@{
                      @"sid": channel.sid,
                      @"uniqueName": channel.uniqueName,
                      @"friendlyName": channel.friendlyName
                      });
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] joinChannel: Failed to join %@ channel with error %@", channel.uniqueName, result.resultText);
            reject(@"error", @"Failed to join channel.", nil);
        }
    }];
}

- (void) createChannel:(NSString *)uniqueName withFriendlyName:(NSString *)friendlyName andType:(NSString *)type resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject{
    if (uniqueName == nil || friendlyName == nil || type == nil) {
        NSLog(@"[IIMobile - RNTwilioChatClient] createChannel: failed to create channel. Some parameters are null");
        reject(@"error", @"Failed to create channel. Some parameters are null", nil);
    } else {
        NSNumber *channelType = nil;
        if ([type isEqualToString:@"private"]) {
            channelType = @(TCHChannelTypePrivate);
        } else if ([type isEqualToString:@"public"]) {
            channelType = @(TCHChannelTypePublic);
        }

        NSDictionary *channelOptions = @{TCHChannelOptionUniqueName: uniqueName,
                                         TCHChannelOptionFriendlyName: friendlyName,
                                         TCHChannelOptionType: channelType};

        [client.channelsList createChannelWithOptions:channelOptions
                                           completion:^(TCHResult *result, TCHChannel *channel) {
                                               if ([result isSuccessful]) {
                                                   [self joinChannel:channel
                                                            resolver:resolve
                                                            rejecter:reject];
                                               } else {
                                                   NSLog(@"[IIMobile - RNTwilioChatClient] createChannel failed with error %@", result.error);
                                                   reject(@"error", @"Failed to create channel", result.error);
                                               }
                                           }];
    }
}


RCT_REMAP_METHOD(createChannel, uniqueName:(NSString *)uniqueName friendlyName:(NSString *) friendlyName type:(NSString *) type create_channel_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] createChannel called with type: %@ with uniqueName: %@", type, uniqueName);

    [self createChannel:uniqueName
       withFriendlyName:friendlyName
                andType:type
               resolver:resolve
               rejecter:reject];
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
            NSLog(@"[IIMobile - RNTwilioChatClient] getPublicChannels failed with error %@", result.error);
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
            NSLog(@"[IIMobile - RNTwilioChatClient] getUserChannels failed with error %@", result.error);
            reject(@"error", @"getUserChannels failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getChannelMembers, members_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] getChannelMembers called");

    [self.channel.members membersWithCompletion:^(TCHResult *result, TCHMemberPaginator *paginator) {
        if (result.isSuccessful) {
            NSMutableArray *members = [[NSMutableArray alloc] init];
            for (TCHMember *member in paginator.items) {
                [members addObject:@{
                                     @"sid": member.sid,
                                     @"identity": member.identity,
                                     @"lastConsumedMessageIndex": member.lastConsumedMessageIndex ? member.lastConsumedMessageIndex  : @"null"
                                     }];

            }
            NSLog(@"[IIMobile - RNTwilioChatClient] getChannelMembers with members %@", members);
            resolve(members);
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] getChannelMembers failed with error %@", result.error);
            reject(@"error", @"getChannelMembers failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getLastMessages, count: (nonnull NSNumber *)count last_messages_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages getLastMessagesWithCount:count.longValue
                                         completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                             if ([result isSuccessful]) {
                                                 resolve([self buildMessageJsonArray:messages]);
                                             } else {
                                                 NSLog(@"[IIMobile - RNTwilioChatClient] getLastMessages failed with error %@", result.error);
                                                 reject(@"error", @"getLastMessages failed", result.error);
                                             }
                                         }];
}

RCT_REMAP_METHOD(getMessagesBefore, beforeIndex: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages getMessagesBefore:(index.intValue - 1)
                                   withCount:count.longValue
                                  completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                      if ([result isSuccessful]) {
                                          resolve([self buildMessageJsonArray:messages]);
                                      } else {
                                          NSLog(@"[IIMobile - RNTwilioChatClient] getMessagesBefore failed with error %@", result.error);
                                          reject(@"error", @"getMessagesBefore failed", result.error);
                                      }
                                  }];
}

RCT_REMAP_METHOD(getMessagesAfter, afterIndex: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages getMessagesAfter:(index.intValue + 1)
                                   withCount:count.longValue
                                  completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                      if ([result isSuccessful]) {
                                          resolve([self buildMessageJsonArray:messages]);
                                      } else {
                                          NSLog(@"[IIMobile - RNTwilioChatClient] getMessagesAfter failed with error %@", result.error);
                                          reject(@"error", @"getMessagesAfter failed", result.error);
                                      }
                                  }];
}

RCT_EXPORT_METHOD(typing) {
    [self.channel typing];
}

RCT_REMAP_METHOD(getUnreadMessagesCount, resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    if (self.channel.messages.lastConsumedMessageIndex) {
        [self.channel getUnconsumedMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
            if ([result isSuccessful]) {
                NSString *strCount = [NSString stringWithFormat:@"%lu", (unsigned long)count];
                NSLog(@"[IIMobile - RNTwilioChatClient] getUnreadMessages: %@", strCount);
                resolve(strCount);
            } else {
                NSLog(@"[IIMobile - RNTwilioChatClient] getUnreadMessages failed with error %@", result.error);
                reject(@"error", @"getUnreadMessages failed", nil);
            }
        }];
    } else {
          [self.channel getMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
              if ([result isSuccessful]) {
                  NSString *strCount = [NSString stringWithFormat:@"%lu", (unsigned long)count];
                  NSLog(@"[IIMobile - RNTwilioChatClient] getUnreadMessages: %@", strCount);
                  resolve(strCount);
              } else {
                  NSLog(@"[IIMobile - RNTwilioChatClient] getUnreadMessages failed with error %@", result.error);
                  reject(@"error", @"getUnreadMessages failed", nil);
              }
          }];
    }
}

RCT_REMAP_METHOD(getMessagesCount, countResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel getMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
        if ([result isSuccessful]) {
            NSString *strCount = [NSString stringWithFormat:@"%lu", (unsigned long)count];
            NSLog(@"[IIMobile - RNTwilioChatClient] getMessagesCount: %@", strCount);
            resolve(strCount);
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] getMessagesCount failed with error %@", result.error);
            reject(@"error", @"getUnreadMessages failed", nil);
        }
    }];
}

RCT_REMAP_METHOD(getLastConsumedMessageIndex, consumedMessageIndexResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    if (self.channel.messages.lastConsumedMessageIndex) {
        NSLog(@"[IIMobile - RNTwilioChatClient] getLastConsumedMessageIndex: %@", self.channel.messages.lastConsumedMessageIndex);
        resolve(self.channel.messages.lastConsumedMessageIndex);
    } else {
        NSLog(@"[IIMobile - RNTwilioChatClient] getLastConsumedMessageIndex: 0");
        resolve(@"0");
    }
}

RCT_REMAP_METHOD(setNoMessagesConsumed, noMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages setNoMessagesConsumedWithCompletion:^(TCHResult *result, NSUInteger count) {
        if ([result isSuccessful]) {
            NSLog(@"[IIMobile - RNTwilioChatClient] setNoMessagesConsumed: Ok");
            resolve(@"ok");
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] setNoMessagesConsumed failed with error %@", result.error);
            reject(@"error", @"failed to set all messages as not read", nil);
        }
    }];
}

RCT_REMAP_METHOD(setAllMessagesConsumed, allMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages setNoMessagesConsumedWithCompletion:^(TCHResult *result, NSUInteger count) {
        if ([result isSuccessful]) {
            NSLog(@"[IIMobile - RNTwilioChatClient] setAllMessagesConsumed: Ok");
            resolve(@"ok");
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] setAllMessagesConsumed failed with error %@", result.error);
            reject(@"error", @"failed to set all messages as read", nil);
        }
    }];
}

RCT_REMAP_METHOD(setLastConsumedMessage, withIndex:(nonnull NSNumber *)index setMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages setLastConsumedMessageIndex:index
                                            completion:^(TCHResult *result, NSUInteger count) {
        if ([result isSuccessful]) {
            NSLog(@"[IIMobile - RNTwilioChatClient] setLastConsumedMessage: %@", index);
            resolve(@"ok");
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] setLastConsumedMessage failed with error %@", result.error);
            reject(@"error", @"failed to set last consumed message index", nil);
        }
    }];
}

RCT_REMAP_METHOD(advanceLastConsumedMessage, withIndex:(nonnull NSNumber *)index advanceMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    [self.channel.messages advanceLastConsumedMessageIndex:index
                                            completion:^(TCHResult *result, NSUInteger count) {
                                                if ([result isSuccessful]) {
                                                    NSLog(@"[IIMobile - RNTwilioChatClient] advanceLastConsumedMessage: %@", index);
                                                    resolve(@"ok");
                                                } else {
                                                    NSLog(@"[IIMobile - RNTwilioChatClient] advanceLastConsumedMessage failed with error %@", result.error);
                                                    reject(@"error", @"failed to advance last consumed message index", nil);
                                                }
                                            }];
}

- (NSDictionary*) buildMessageJson:(TCHMessage *)message withChannel:(TCHChannel *) channel {
    return @{
             @"sid": message.sid,
             @"index": message.index,
             @"author": message.author,
             @"timeStamp": message.timestamp,
             @"body": message.body ?  message.body : @"",
             @"channelUniqueName": channel.uniqueName
             };
}

- (NSMutableArray*) buildMessageJsonArray:(NSArray<TCHMessage *> *)messages {
    NSMutableArray *jsonArray = [[NSMutableArray alloc] init];
    for (TCHMessage *message in messages) {
        NSDictionary *jsonMessage = [self buildMessageJson:message withChannel: self.channel];
            [jsonArray addObject: jsonMessage];
    }
    return jsonArray;
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
    NSDictionary *messageJson = [self buildMessageJson:message withChannel:channel];
    [RNEventEmitterHelper emitEventWithName:@"messageAdded" andPayload:messageJson];
}

- (void)chatClient:(nonnull TwilioChatClient *)client typingStartedOnChannel:(nonnull TCHChannel *)channel member:(nonnull TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] typingStartedOnChannel");
    NSDictionary *payload = @{@"uniqueName": channel.uniqueName,
                              @"identity": member.identity
                              };
    [RNEventEmitterHelper emitEventWithName:@"typingStartedOnChannel" andPayload:payload];
}

- (void)chatClient:(nonnull TwilioChatClient *)client typingEndedOnChannel:(nonnull TCHChannel *)channel member:(nonnull TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] typingEndedOnChannel");
    NSDictionary *payload = @{@"uniqueName": channel.uniqueName,
                              @"identity": member.identity
                              };
    [RNEventEmitterHelper emitEventWithName:@"typingEndedOnChannel" andPayload:payload];
}

- (void)chatClientTokenWillExpire:(TwilioChatClient *)chatClient {
    [RNEventEmitterHelper emitEventWithName:@"tokenAboutToExpire" andPayload:nil];
}

- (void)chatClientTokenExpired:(nonnull TwilioChatClient *)client {
    [RNEventEmitterHelper emitEventWithName:@"tokenExpired" andPayload:nil];
}

+(BOOL)requiresMainQueueSetup {
    return NO;
}

@end
