//
//  RNConverter.h
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 9/18/19.
//  Copyright © 2019 No Good Software Inc. All rights reserved.
//

#import <TwilioChatClient/TwilioChatClient.h>
#import <TwilioChatClient/TCHUser.h>
#import <TwilioAccessManager/TwilioAccessManager.h>
#import <React/RCTConvert.h>

@interface RNConverter (TwilioChatClient)

+ (TCHClientSynchronizationStatus)TCHClientSynchronizationStatus:(id)json;
+ (TCHChannelSynchronizationStatus)TCHChannelSynchronizationStatus:(id)json;
+ (TCHChannelType)TCHChannelType:(id)json;
+ (TCHChannelStatus)TCHChannelStatus:(id)json;
+ (TCHUserUpdate)TCHUserUpdate:(id)json;
+ (TCHClientSynchronizationStrategy)TCHClientSynchronizationStrategy:(id)json;
+ (TCHLogLevel)TCHLogLevel:(id)json;
+ (TCHClientConnectionState)TCHClientConnectionState:(id)json;

+ (NSDictionary *)TwilioChatClient:(TwilioChatClient *)client;

+ (NSDictionary *)TCHChannel:(TCHChannel *)channel;
+ (NSDictionary *)TCHChannelDescriptor:(TCHChannelDescriptor *)channel;
+ (NSDictionary *)TCHUser:(TCHUser *)user;
+ (NSDictionary *)TCHMember:(TCHMember *)member;
+ (NSDictionary *)TCHMessage:(TCHMessage *)message;

+ (NSDictionary *)TCHMemberPaginator:(TCHMemberPaginator *)paginator;
+ (NSDictionary *)TCHChannelPaginator:(TCHChannelPaginator *)paginator;
+ (NSDictionary *)TCHChannelDescriptorPaginator:(TCHChannelDescriptorPaginator *)paginator;

+ (NSArray *)TCHChannels:(NSArray<TCHChannel *>*)channels;
+ (NSArray *)TCHChannelDescriptors:(NSArray<TCHChannelDescriptor *>*)channels;
+ (NSArray *)TCHMembers:(NSArray<TCHMember *>*)members;
+ (NSArray *)TCHMessages:(NSArray<TCHMessage *> *)messages;

+ (NSData *)dataWithHexString:(NSString*)hex;

@end