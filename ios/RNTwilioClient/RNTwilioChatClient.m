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
#import "RNConverter+TwilioChatClient.h"

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

RCT_REMAP_METHOD(createClient, token:(NSString*)token properties:(NSDictionary *)properties create_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
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
            reject(@"create-client-error", @"Create ChatClient failed", nil);
        }
    }];
}

RCT_REMAP_METHOD(updateClient, updatedToken:(NSString*)token update_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSLog(@"[IIMobile - RNTwilioChatClient] updatedToken with token: %@", token);
    [self.client updateToken:token
                  completion:^(TCHResult * _Nonnull result) {
        if (result.isSuccessful) {
           resolve(@[@TRUE])
        } else {
           reject("create-client-error", "updatedToken failed with error", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getPublicChannels, lpublic_channels_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatClient] getPublicChannels called");
    [[self.client channelsList] publicChannelDescriptorsWithCompletion:^(TCHResult *result,
                                                                         TCHChannelDescriptorPaginator *paginator) {
        if ([result isSuccessful]) {
            NSMutableArray *channels = [[NSMutableArray alloc] init];
            for (TCHChannelDescriptor *channel in paginator.items) {
                [channels addObject:@{
                                      @"sid": channel.sid,
                                      @"uniqueName": channel.uniqueName,
                                      @"friendlyName": channel.friendlyName
                                      }];
            }
            resolve(channels);
        } else {
            reject(@"get-public-channels-error", @"getPublicChannels failed", result.error);
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
                [channels addObject:@{
                                      @"sid": channel.sid,
                                      @"uniqueName": channel.uniqueName,
                                      @"friendlyName": channel.friendlyName
                                      }];
            }
            resolve(channels);
        } else {
            reject(@"get-user-channels-error", @"getUserChannels failed", result.error);
        }
    }];
}

#pragma mark RNTwilioChatClient Delegates

- (void)chatClient:(TwilioChatClient *)client connectionStateUpdated:(TCHClientConnectionState)state {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:connectionStateUpdated with status: %@", @(state));
    [RNEventEmitterHelper emitEventWithName:@"connectionStateUpdated"
                                 andPayload:@(state)];
}

- (void)chatClientTokenWillExpire:(TwilioChatClient *)chatClient {
    [RNEventEmitterHelper emitEventWithName:@"tokenAboutToExpire"
                                 andPayload:nil];
}

- (void)chatClientTokenExpired:(nonnull TwilioChatClient *)client {
    [RNEventEmitterHelper emitEventWithName:@"tokenExpired"
                                 andPayload:nil];
}

- (void)chatClient:(TwilioChatClient *)client synchronizationStatusUpdated:(TCHClientSynchronizationStatus)status {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:synchronizationStatusUpdated with status: %@", @(state));
    [RNEventEmitterHelper emitEventWithName:@"synchronizationStatusUpdated"
                                 andPayload:@(status)];
}

- (void)chatClient:(TwilioChatClient *)client channelAdded:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelAdded with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelAdded"
                                 andPayload:[RNConverter TCHChannel:channel]];
}

- (void)chatClient:(TwilioChatClient *)client channel:(TCHChannel *)channel updated:(TCHChannelUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelUpdated with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelUpdated"
                                 andPayload:@{
                                              @"channel": [RNConverter TCHChannel:channel],
                                              @"reason": @(updated)]
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client channelDeleted:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelDeleted with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelDeleted"
                                 andPayload:[RNConverter TCHChannel:channel]];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel synchronizationStatusUpdated:(TCHChannelSynchronizationStatus)status {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelSynchronizationStatusUpdated with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelSynchronizationStatusUpdated"
                                 andPayload:@{
                                              @"channel": [RNConverter TCHChannel:channel],
                                              @"status": @(status)
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel memberJoined:(nonnull TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:memberJoined with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"memberAdded"
                                 andPayload:@{
                                              @"member": [RNConverter TCHMember:member],
                                              @"channelSid": channel.sid
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel member:(nonnull TCHMember *)member updated:(TCHMemberUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:memberUpdated with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"memberUpdated"
                                 andPayload:@{
                                              @"member": [RNConverter TCHMember:member],
                                              @"channelSid": channel.sid,
                                              @"reason": @(updated)
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel memberLeft:(nonnull TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:memberLeft with sid: %@", channel.sid);
        [RNEventEmitterHelper emitEventWithName:@"memberDeleted"
                                     andPayload:@{
                                                  @"member": [RNConverter TCHMember:member],
                                                  @"channelSid": channel.sid
                                                  }];
}

- (void)chatClient:(TwilioChatClient *)client channel:(TCHChannel *)channel messageAdded:(TCHMessage *)message {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageAdded with body: %@", message.body);
    [RNEventEmitterHelper emitEventWithName:@"messageAdded"
                                 andPayload:@{
                                            @"channelSid": channel.sid,
                                            @"member": [RNConverter TCHMessage:message]
                                            }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel message:(nonnull TCHMessage *)message updated:(TCHMessageUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageUpdated with body: %@", message.body);
    [RNEventEmitterHelper emitEventWithName:@"messageUpdated"
                                 andPayload:@{
                                            @"channelSid": channel.sid,
                                            @"member": [RNConverter TCHMessage:message],
                                            @"reason": @(updated)
                                            }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel messageDeleted:(nonnull TCHMessage *)message {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageDeleted with body: %@", message.body);
        [RNEventEmitterHelper emitEventWithName:@"messageDeleted"
                                     andPayload:@{
                                                @"channelSid": channel.sid,
                                                @"member": [RNConverter TCHMessage:message]
                                                }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client errorReceived:(nonnull TCHError *)error {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:errorReceived with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"error"
                                 andPayload:@{
                                              @"code": error.code,
                                              @"message": error.userInfo,
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client typingStartedOnChannel:(TCHChannel *)channel member:(TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] typingStartedOnChannel");
    [RNEventEmitterHelper emitEventWithName:@"typingStartedOnChannel"
                                 andPayload:@{
                                              @"channelSid": channel.sid,
                                              @"member": [RNConverter TCHMember:member]
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client typingEndedOnChannel:(TCHChannel *)channel member:(TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] typingEndedOnChannel");
    [RNEventEmitterHelper emitEventWithName:@"typingEndedOnChannel"
                                 andPayload:@{
                                              @"channelSid": channel.sid,
                                              @"member": [RNConverter TCHMember:member]
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationNewMessageReceivedForChannelSid:(nonnull NSString *)channelSid messageIndex:(NSUInteger)messageIndex {
    NSLog(@"[IIMobile - RNTwilioChatClient] notificationNewMessageReceivedForChannelSid");
    [RNEventEmitterHelper emitEventWithName:@"newMessageNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid,
                                              @"messageIndex": messageIndex
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationAddedToChannelWithSid:(nonnull NSString *)channelSid {
    NSLog(@"[IIMobile - RNTwilioChatClient] notificationAddedToChannelWithSid");
    [RNEventEmitterHelper emitEventWithName:@"addedToChannelNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationInvitedToChannelWithSid:(nonnull NSString *)channelSid {
    NSLog(@"[IIMobile - RNTwilioChatClient] notificationInvitedToChannelWithSid");
    [RNEventEmitterHelper emitEventWithName:@"invitedToChannelNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationRemovedFromChannelWithSid:(nonnull NSString *)channelSid {
    NSLog(@"[IIMobile - RNTwilioChatClient] notificationRemovedFromChannelWithSid");
    [RNEventEmitterHelper emitEventWithName:@"removedFromChannelNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client user:(TCHUser *)user updated:(TCHUserUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] userUpdated");
    [RNEventEmitterHelper emitEventWithName:@"userUpdated"
                                 andPayload:@{@"reason": @(updated),
                                              @"user": [RNConverter TCHUser:user]
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client userSubscribed:(nonnull TCHUser *)user {
    NSLog(@"[IIMobile - RNTwilioChatClient] userSubscribed");
    [RNEventEmitterHelper emitEventWithName:@"userSubscribed"
                                 andPayload:@{[RNConverter TCHUser:user]];
}

- (void)chatClient:(nonnull TwilioChatClient *)client userUnsubscribed:(nonnull TCHUser *)user {
    NSLog(@"[IIMobile - RNTwilioChatClient] userUnsubscribed");
            [RNEventEmitterHelper emitEventWithName:@"userUnsubscribed"
                                         andPayload:@{[RNConverter TCHUser:user]];
}

+(BOOL)requiresMainQueueSetup {
    return NO;
}

#pragma mark Enums

- (NSDictionary *)constantsToExport
{
  return @{ @"Constants": @{
                @"TCHClientSynchronizationStatus": @{
                    @"Started" : @(TCHClientSynchronizationStatusStarted),
                    @"ChannelsListCompleted" : @(TCHClientSynchronizationStatusChannelsListCompleted),
                    @"Completed" : @(TCHClientSynchronizationStatusCompleted),
                    @"Failed" : @(TCHClientSynchronizationStatusFailed)
                    },
                @"TCHChannelSynchronizationStatus": @{
                    @"None" : @(TCHChannelSynchronizationStatusNone),
                    @"Identifier" : @(TCHChannelSynchronizationStatusIdentifier),
                    @"Metadata" : @(TCHChannelSynchronizationStatusMetadata),
                    @"All" : @(TCHChannelSynchronizationStatusAll),
                    @"Failed" : @(TCHChannelSynchronizationStatusFailed)
                    },
                @"TCHChannelStatus": @{
                    @"Invited": @(TCHChannelStatusInvited),
                    @"Joined": @(TCHChannelStatusJoined),
                    @"NotParticipating": @(TCHChannelStatusNotParticipating)
                    },
                @"TCHChannelType": @{
                    @"Public": @(TCHChannelTypePublic),
                    @"Private": @(TCHChannelTypePrivate)
                    },
                @"TCHClientSynchronizationStrategy": @{
                    @"All": @(TCHClientSynchronizationStrategyAll),
                    @"ChannelsList": @(TCHClientSynchronizationStrategyChannelsList)
                    },
                @"TCHUserInfoUpdate": @{
                    @"FriendlyName": @(TCHUserInfoUpdateFriendlyName),
                    @"Attributes": @(TCHUserInfoUpdateAttributes),
                    @"ReachabilityOnline": @(TCHUserInfoUpdateReachabilityOnline),
                    @"ReachabilityNotifiable": @(TCHUserInfoUpdateReachabilityNotifiable)
                    },
                @"TCHLogLevel": @{
                    @"Fatal" : @(TCHLogLevelFatal),
                    @"Critical" : @(TCHLogLevelCritical),
                    @"Warning" : @(TCHLogLevelWarning),
                    @"Info" : @(TCHLogLevelInfo),
                    @"Debug" : @(TCHLogLevelDebug)
                    },
                @"TCHChannelOption": @{
                        @"FriendlyName" : TCHChannelOptionFriendlyName,
                        @"UniqueName" : TCHChannelOptionUniqueName,
                        @"Type" : TCHChannelOptionType,
                        @"Attributes" : TCHChannelOptionAttributes
                        },
                @"TCHClientConnectionState": @{
                        @"Unknown" : @(TCHClientConnectionStateUnknown),
                        @"Disconnected" : @(TCHClientConnectionStateDisconnected),
                        @"Connected" : @(TCHClientConnectionStateConnected),
                        @"Connecting" : @(TCHClientConnectionStateConnecting),
                        @"Denied" : @(TCHClientConnectionStateDenied),
                        @"Error" : @(TCHClientConnectionStateError)
                        }
                }
            };
};

@end
