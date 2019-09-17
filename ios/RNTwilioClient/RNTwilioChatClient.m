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

- (void)chatClient:(TwilioChatClient *)client connectionStateChanged:(TCHClientConnectionState)state {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:connectionStateChanged with status: %@", @(state));
    [RNEventEmitterHelper emitEventWithName:@"connectionStateChanged" andPayload:@(state)];
}

- (void)chatClient:(TwilioChatClient *)client synchronizationStatusUpdated:(TCHClientSynchronizationStatus)status {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:synchronizationStatusUpdated with status: %@", [self convertSyncStatusToString:status]);
    [RNEventEmitterHelper emitEventWithName:@"synchronizationStatusUpdated" andPayload:@{@"status": [self convertSyncStatusToString:status]}];
}

- (void)chatClient:(TwilioChatClient *)client channel:(TCHChannel *)channel messageAdded:(TCHMessage *)message {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:messageAdded with body: %@", message.body);
    NSDictionary *messageJson = [self buildMessageJson:message withChannel:channel];
    [RNEventEmitterHelper emitEventWithName:@"messageAdded" andPayload:messageJson];
}

- (void)chatClient:(TwilioChatClient *)client channelAdded:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelAdded with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelAdded" andPayload:[RNTConvert TCHChannel:channel]];
}

- (void)chatClient:(TwilioChatClient *)client channelChanged:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelChanged with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelChanged" andPayload:[RNTConvert TCHChannel:channel]];
}

- (void)chatClient:(TwilioChatClient *)client channelDeleted:(TCHChannel *)channel {
    NSLog(@"[IIMobile - RNTwilioChatClient] Delegates:channelRemoved with sid: %@", channel.sid);
    [RNEventEmitterHelper emitEventWithName:@"channelRemoved" andPayload:[RNTConvert TCHChannel:channel]];
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
