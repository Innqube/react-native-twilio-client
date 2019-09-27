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
#import "RCTConvert+TwilioChatClient.h"

@import AVFoundation;
@import TwilioChatClient;

@interface RNTwilioChatClient() <TwilioChatClientDelegate>
#pragma mark - Twilio Chat Members
@property (nonatomic) TCHClientSynchronizationStatus *synchronizationStatus;
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
    [TwilioChatClient chatClientWithToken:token properties:properties delegate:self completion:^(TCHResult * _Nonnull result, TwilioChatClient * _Nullable chatClient) {
        if (chatClient) {
            self.client = chatClient;
            dispatch_async(dispatch_get_main_queue(), ^{
                NSLog(@"[IIMobile - RNTwilioChatClient] ChatClient successfully created");
                resolve(@{@"status": @"null"});
            });
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] createClient failed with error %@", result.error);
            reject(@"create-client-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], nil);
        }
    }];
}

RCT_REMAP_METHOD(updateClient, updatedToken:(NSString*)token update_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSLog(@"[IIMobile - RNTwilioChatClient] updateClient with token: %@", token);
    [self.client updateToken:token
                  completion:^(TCHResult * _Nonnull result) {
        if (result.isSuccessful) {
            NSLog(@"[IIMobile - RNTwilioChatClient] ChatClient successfully updated");
            resolve(@[@TRUE]);
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] updateClient failed with error: %@", result.error);
           reject(@"update-client-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
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
            NSLog(@"[IIMobile - RNTwilioChatClient] getPublicChannels failed with error: %@", result.error);
            reject(@"get-public-channels-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
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
            reject(@"get-user-channels-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(register, register_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSLog(@"[IIMobile - RNTwilioChatClient] register with token: %@", self.deviceToken);

    [self.client registerWithNotificationToken:self.deviceToken completion:^(TCHResult * _Nonnull result) {
        if (result.isSuccessful) {
            NSLog(@"[IIMobile - RNTwilioChatClient] register with token successfully");
            resolve(@{@"status": @"registered"});
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] register with token failed with error %@", result.error);
             reject(@"register-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], nil);
        }
    }];
}

RCT_REMAP_METHOD(unRegister, deregister_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    NSLog(@"[IIMobile - RNTwilioChatClient] deregister with token: %@", self.deviceToken);

    [self.client deregisterWithNotificationToken:self.deviceToken completion:^(TCHResult * _Nonnull result) {
        if (result.isSuccessful) {
            NSLog(@"[IIMobile - RNTwilioChatClient] deregister with token successfully");
            resolve(@{@"status": @"registered"});
        } else {
            NSLog(@"[IIMobile - RNTwilioChatClient] deregister with token failed with error %@", result.error);
             reject(@"deregister-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], nil);
        }
    }];
}

#pragma mark RNTwilioChatClient Delegates

- (void)chatClient:(TwilioChatClient *)client connectionStateUpdated:(TCHClientConnectionState)state {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:connectionStateUpdated with status: %@", [RCTConvert TCHClientConnectionStateToString: state]);
    [RNEventEmitterHelper emitEventWithName:@"connectionStateUpdated"
                                 andPayload:@{@"state": [RCTConvert TCHClientConnectionStateToString: state]}];
}

- (void)chatClientTokenWillExpire:(TwilioChatClient *)chatClient {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:chatClientTokenWillExpire");
    [RNEventEmitterHelper emitEventWithName:@"tokenAboutToExpire"
                                 andPayload:@{}];
}

- (void)chatClientTokenExpired:(nonnull TwilioChatClient *)client {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:chatClientTokenExpired");
    [RNEventEmitterHelper emitEventWithName:@"tokenExpired"
                                 andPayload:@{}];
}

- (void)chatClient:(TwilioChatClient *)client synchronizationStatusUpdated:(TCHClientSynchronizationStatus)status {
    self.synchronizationStatus = status;
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:synchronizationStatusUpdated with status: %@", [RCTConvert TCHClientSynchronizationStatusToString:status]);
    [RNEventEmitterHelper emitEventWithName:@"synchronizationStatusUpdated"
                                 andPayload:@{@"status": [RCTConvert TCHClientSynchronizationStatusToString:status]}];
}

- (void)chatClient:(TwilioChatClient *)client channelAdded:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelAdded with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelAdded"
                                 andPayload:[RCTConvert TCHChannel:channel]];
}

- (void)chatClient:(TwilioChatClient *)client channel:(TCHChannel *)channel updated:(TCHChannelUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelUpdated with reason: %@", [RCTConvert TCHChannelUpdateToString:updated]);
    [RNEventEmitterHelper emitEventWithName:@"channelUpdated"
                                 andPayload:@{
                                              @"channel": [RCTConvert TCHChannel:channel],
                                              @"reason": [RCTConvert TCHChannelUpdateToString:updated]
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client channelDeleted:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelDeleted with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelDeleted"
                                 andPayload:[RCTConvert TCHChannel:channel]];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel synchronizationStatusUpdated:(TCHChannelSynchronizationStatus)status {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelSynchronizationStatusUpdated with status: %@", [RCTConvert TCHChannelSynchronizationStatusToString:status]);
    [RNEventEmitterHelper emitEventWithName:@"channelSynchronizationStatusUpdated"
                                 andPayload:@{
                                              @"channel": [RCTConvert TCHChannel:channel],
                                              @"status": [RCTConvert TCHChannelSynchronizationStatusToString:status]
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel memberJoined:(nonnull TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:memberJoined with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"memberAdded"
                                 andPayload:@{
                                              @"member": [RCTConvert TCHMember:member],
                                              @"channelSid": channel.sid
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel member:(nonnull TCHMember *)member updated:(TCHMemberUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:memberUpdated with reason: %@", [RCTConvert TCHMemberUpdateToString:updated]);
    [RNEventEmitterHelper emitEventWithName:@"memberUpdated"
                                 andPayload:@{
                                              @"member": [RCTConvert TCHMember:member],
                                              @"channelSid": channel.sid,
                                              @"reason": [RCTConvert TCHMemberUpdateToString:updated]
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel memberLeft:(nonnull TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:memberLeft with sid: %@", channel.sid);
        [RNEventEmitterHelper emitEventWithName:@"memberDeleted"
                                     andPayload:@{
                                                  @"channelSid": channel.sid,
                                                  @"member": [RCTConvert TCHMember:member]
                                                  }];
}

- (void)chatClient:(TwilioChatClient *)client channel:(TCHChannel *)channel messageAdded:(TCHMessage *)message {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageAdded with body: %@", message.body);
    [RNEventEmitterHelper emitEventWithName:@"messageAdded"
                                 andPayload:@{
                                            @"channelSid": channel.sid,
                                            @"message": [RCTConvert TCHMessage:message]
                                            }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel message:(nonnull TCHMessage *)message updated:(TCHMessageUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageUpdated with reason: %@ ", [RCTConvert TCHMessageUpdateToString:updated]);
    [RNEventEmitterHelper emitEventWithName:@"messageUpdated"
                                 andPayload:@{
                                            @"channelSid": channel.sid,
                                            @"message": [RCTConvert TCHMessage:message],
                                            @"reason": [RCTConvert TCHMessageUpdateToString:updated]
                                            }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client channel:(nonnull TCHChannel *)channel messageDeleted:(nonnull TCHMessage *)message {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageDeleted with body: %@", message.body);
        [RNEventEmitterHelper emitEventWithName:@"messageDeleted"
                                     andPayload:@{
                                                @"channelSid": channel.sid,
                                                @"member": [RCTConvert TCHMessage:message]
                                                }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client errorReceived:(nonnull TCHError *)error {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:errorReceived with code: %@", error.userInfo);
    [RNEventEmitterHelper emitEventWithName:@"error"
                                 andPayload:@{
                                              @"message": error.userInfo,
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client typingStartedOnChannel:(TCHChannel *)channel member:(TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:typingStartedOnChannel");
    [RNEventEmitterHelper emitEventWithName:@"typingStarted"
                                 andPayload:@{
                                              @"channelSid": channel.sid,
                                              @"member": [RCTConvert TCHMember:member]
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client typingEndedOnChannel:(TCHChannel *)channel member:(TCHMember *)member {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:typingEndedOnChannel");
    [RNEventEmitterHelper emitEventWithName:@"typingEnded"
                                 andPayload:@{
                                              @"channelSid": channel.sid,
                                              @"member": [RCTConvert TCHMember:member]
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationNewMessageReceivedForChannelSid:(nonnull NSString *)channelSid messageIndex:(NSUInteger)messageIndex {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:notificationNewMessageReceivedForChannelSid");
    [RNEventEmitterHelper emitEventWithName:@"newMessageNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid,
                                              @"messageIndex": @(messageIndex)
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationAddedToChannelWithSid:(nonnull NSString *)channelSid {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:notificationAddedToChannelWithSid");
    [RNEventEmitterHelper emitEventWithName:@"addedToChannelNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationInvitedToChannelWithSid:(nonnull NSString *)channelSid {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:notificationInvitedToChannelWithSid");
    [RNEventEmitterHelper emitEventWithName:@"invitedToChannelNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid
                                              }];
}

- (void)chatClient:(nonnull TwilioChatClient *)client notificationRemovedFromChannelWithSid:(nonnull NSString *)channelSid {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:notificationRemovedFromChannelWithSid");
    [RNEventEmitterHelper emitEventWithName:@"removedFromChannelNotification"
                                 andPayload:@{
                                              @"channelSid": channelSid
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client user:(TCHUser *)user updated:(TCHUserUpdate)updated {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:userUpdated");
    [RNEventEmitterHelper emitEventWithName:@"userUpdated"
                                 andPayload:@{@"reason": [RCTConvert TCHUserUpdateToString:updated],
                                              @"user": [RCTConvert TCHUser:user]
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client userSubscribed:(TCHUser *)user {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:userSubscribed");
    [RNEventEmitterHelper emitEventWithName:@"userSubscribed"
                                 andPayload:@{
                                              @"identity": user.identity,
                                              @"friendlyName": RCTNullIfNil(user.friendlyName)
                                              }];
}

- (void)chatClient:(TwilioChatClient *)client userUnsubscribed:(TCHUser *)user {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:userUnsubscribed");
            [RNEventEmitterHelper emitEventWithName:@"userUnsubscribed"
                                         andPayload:@{
                                                      @"identity": user.identity,
                                                      @"friendlyName": RCTNullIfNil(user.friendlyName)
                                                      }];
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
                @"TCHUserUpdate": @{
                    @"FriendlyName": @(TCHUserUpdateFriendlyName),
                    @"Attributes": @(TCHUserUpdateAttributes),
                    @"ReachabilityOnline": @(TCHUserUpdateReachabilityOnline),
                    @"ReachabilityNotifiable": @(TCHUserUpdateReachabilityNotifiable)
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
