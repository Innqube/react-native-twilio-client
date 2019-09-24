//
//  RNLogHelper.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 9/18/19.
//  Copyright © 2019 No Good Software Inc. All rights reserved.
//

#import "RCTConvert+TwilioChatClient.h"
#import <React/RCTUtils.h>

@implementation RCTConvert (TwilioChatClient)

RCT_ENUM_CONVERTER(TCHClientSynchronizationStatus,(@{
                                                      @"Started" : @(TCHClientSynchronizationStatusStarted),
                                                      @"ChannelsListCompleted" : @(TCHClientSynchronizationStatusChannelsListCompleted),
                                                      @"Completed" : @(TCHClientSynchronizationStatusCompleted),
                                                      @"Failed" : @(TCHClientSynchronizationStatusFailed),
                                                      }), TCHClientSynchronizationStatusStarted, integerValue)


RCT_ENUM_CONVERTER(TCHChannelSynchronizationStatus,(@{
                                                      @"None" : @(TCHChannelSynchronizationStatusNone),
                                                      @"Identifier" : @(TCHChannelSynchronizationStatusIdentifier),
                                                      @"Metadata" : @(TCHChannelSynchronizationStatusMetadata),
                                                      @"All" : @(TCHChannelSynchronizationStatusAll),
                                                      @"Failed" : @(TCHChannelSynchronizationStatusFailed),
                                                      }), TCHChannelSynchronizationStatusNone, integerValue)

RCT_ENUM_CONVERTER(TCHChannelStatus,(@{
                                       @"Invited" : @(TCHChannelStatusInvited),
                                       @"Joined" : @(TCHChannelStatusJoined),
                                       @"NotParticipating" : @(TCHChannelStatusNotParticipating),
                                      }), TCHChannelStatusInvited, integerValue)

RCT_ENUM_CONVERTER(TCHChannelType,(@{
                                     @"Public" : @(TCHChannelTypePublic),
                                     @"Private" : @(TCHChannelTypePrivate),
                                     }), TCHChannelTypePublic, integerValue)

RCT_ENUM_CONVERTER(TCHUserUpdate,(@{
                                        @"FriendlyName" : @(TCHUserUpdateFriendlyName),
                                        @"Attributes" : @(TCHUserUpdateAttributes),
                                        @"ReachabilityOnline": @(TCHUserUpdateReachabilityOnline),
                                        @"ReachabilityNotifiable": @(TCHUserUpdateReachabilityNotifiable),
                                        }), TCHUserUpdateFriendlyName, integerValue)

RCT_ENUM_CONVERTER(TCHMessageUpdate,(@{
                                    @"Body" : @(TCHMessageUpdateBody),
                                    @"Attributes" : @(TCHMessageUpdateAttributes),
                                    }), TCHMessageUpdateBody, integerValue)

RCT_ENUM_CONVERTER(TCHMemberUpdate,(@{
                                       @"LastConsumedMessageIndex" : @(TCHMemberUpdateLastConsumedMessageIndex),
                                       @"Attributes" : @(TCHMemberUpdateAttributes),
                                       }), TCHMemberUpdateLastConsumedMessageIndex, integerValue)

RCT_ENUM_CONVERTER(TCHChannelUpdate,(@{
                                      @"Status" : @(TCHChannelUpdateStatus),
                                      @"LastConsumedMessageIndex" : @(TCHChannelUpdateLastConsumedMessageIndex),
                                      @"UniqueName" : @(TCHChannelUpdateUniqueName),
                                      @"FriendlyName" : @(TCHChannelUpdateFriendlyName),
                                      @"Attributes" : @(TCHChannelUpdateAttributes),
                                      @"LastMessage" : @(TCHChannelUpdateLastMessage),
                                      @"UserNotificationLevel" : @(TCHChannelUpdateUserNotificationLevel),
                                      }), TCHChannelUpdateStatus, integerValue)

RCT_ENUM_CONVERTER(TCHLogLevel,(@{
                                  @"Fatal" : @(TCHLogLevelFatal),
                                  @"Critical" : @(TCHLogLevelCritical),
                                  @"Warning" : @(TCHLogLevelWarning),
                                  @"Info" : @(TCHLogLevelInfo),
                                  @"Debug" : @(TCHLogLevelDebug),
                                }), TCHLogLevelFatal, integerValue)

RCT_ENUM_CONVERTER(TCHClientConnectionState,(@{
                                  @"Unknown" : @(TCHClientConnectionStateUnknown),
                                  @"Disconnected" : @(TCHClientConnectionStateDisconnected),
                                  @"Connected" : @(TCHClientConnectionStateConnected),
                                  @"Connecting" : @(TCHClientConnectionStateConnecting),
                                  @"Denied" : @(TCHClientConnectionStateDenied),
                                  @"Error" : @(TCHClientConnectionStateError)
                                  }), TCHClientConnectionStateUnknown, integerValue)

+ (NSString *)TCHClientConnectionStateToString:(TCHClientConnectionState)state {
    if (!state) {
        return RCTNullIfNil(nil);
    }

    switch(state) {
        case TCHClientConnectionStateUnknown:
            return @"UNKNOWN";
        case TCHClientConnectionStateDisconnected:
            return @"DISCONNECTED";
        case TCHClientConnectionStateConnected:
            return @"COMPLETES";
        case TCHClientConnectionStateConnecting:
            return @"CONNECTING";
        case TCHClientConnectionStateDenied:
            return @"DENIED";
        case TCHClientConnectionStateError:
            return @"FAILED";
    }
}

+ (NSString *)TCHClientSynchronizationStatusToString:(TCHClientSynchronizationStatus)status {
    if (!status) {
        return RCTNullIfNil(nil);
    }
    
    switch(status) {
        case TCHClientSynchronizationStatusStarted:
            return @"STARTED";
        case TCHClientSynchronizationStatusChannelsListCompleted:
            return @"CHANNELS_COMPLETED";
        case TCHClientSynchronizationStatusCompleted:
            return @"COMPLETED";
        case TCHClientSynchronizationStatusFailed:
            return @"FAILED";
    }
}

+ (NSString *)TCHChannelSynchronizationStatusToString:(TCHChannelSynchronizationStatus)status {
    if (!status) {
        return RCTNullIfNil(nil);
    }

    switch(status) {
        case TCHChannelSynchronizationStatusNone:
            return @"NONE";
        case TCHChannelSynchronizationStatusIdentifier:
            return @"IDENTIFIER";
        case TCHChannelSynchronizationStatusMetadata:
            return @"METADATA";
        case TCHChannelSynchronizationStatusAll:
            return @"ALL";
        case TCHChannelSynchronizationStatusFailed:
            return @"FAILED";
    }
}

+ (NSString *)TCHChannelUpdateToString:(TCHChannelUpdate)update {
    if (!update) {
        return RCTNullIfNil(nil);
    }

    switch(update) {
        case TCHChannelUpdateStatus:
            return @"STATUS";
        case TCHChannelUpdateLastConsumedMessageIndex:
            return @"LAST_CONSUMED_MESSAGE_INDEX";
        case TCHChannelUpdateUniqueName:
            return @"UNIQUE_NAME";
        case TCHChannelUpdateFriendlyName:
            return @"FRIENDLY_NAME";
        case TCHChannelUpdateAttributes:
            return @"ATTRIBUTES";
        case TCHChannelUpdateLastMessage:
            return @"LAST_MESSAGE";
        case TCHChannelUpdateUserNotificationLevel:
            return @"NOTIFICATION_LEVEL";
    }
}

+ (NSString *)TCHMessageUpdateToString:(TCHMessageUpdate)update {
    if (!update) {
        return RCTNullIfNil(nil);
    }

    switch(update) {
        case TCHMessageUpdateBody:
            return @"BODY";
        case TCHMessageUpdateAttributes:
            return @"ATTRIBUTES";
    }
}

+ (NSString *)TCHUserUpdateToString:(TCHUserUpdate)update {
    if (!update) {
        return RCTNullIfNil(nil);
    }

    switch(update) {
        case TCHUserUpdateFriendlyName:
            return @"FRIENDLY_NAME";
        case TCHUserUpdateAttributes:
            return @"REACHABILITY_ONLINE";
        case TCHUserUpdateReachabilityOnline:
            return @"ATTRIBUTES";
        case TCHUserUpdateReachabilityNotifiable:
            return @"REACHABILITY_NOTIFIABLE";
    }
}

+ (NSString *)TCHMemberUpdateToString:(TCHMemberUpdate)update {
    if (!update) {
        return RCTNullIfNil(nil);
    }

    switch(update) {
        case TCHMemberUpdateLastConsumedMessageIndex:
            return @"LAST_CONSUMED_MESSAGE_INDEX";
        case TCHMemberUpdateAttributes:
            return @"ATTRIBUTES";
    }
}

+ (NSDictionary *)TwilioChatClient:(TwilioChatClient *)client {
  if (!client) {
    return RCTNullIfNil(nil);
  }
  return @{
           @"user": [self TCHUser:client.user],
           @"synchronizationStatus": @(client.synchronizationStatus),
           @"version": client.version,
           @"isReachabilityEnabled": @(client.isReachabilityEnabled)
           };
}


+ (NSDictionary *)TCHUser:(TCHUser *)user {
  if (!user) {
    return RCTNullIfNil(nil);
  }
  return @{
           @"identity": user.identity,
           @"friendlyName": RCTNullIfNil(user.friendlyName)
           };
}

+ (NSDictionary *)TCHMessage:(TCHMessage *)message {
  if (!message) {
    return RCTNullIfNil(nil);
  }
  return @{
           @"sid": message.sid,
           @"index": message.index,
           @"author": RCTNullIfNil(message.author),
           @"body": RCTNullIfNil(message.body),
           @"timestamp": RCTNullIfNil(message.timestamp),
           @"timestampAsDate": message.timestampAsDate == nil ? @"null" : @(message.timestampAsDate.timeIntervalSince1970 * 1000),
           @"dateUpdated": RCTNullIfNil(message.dateUpdated),
           @"dateUpdatedDate": message.dateUpdatedAsDate == nil ? @"null" : @(message.dateUpdatedAsDate.timeIntervalSince1970 * 1000),
           @"lastUpdatedBy": RCTNullIfNil(message.lastUpdatedBy),
           @"attributes": RCTNullIfNil(message.attributes)
           };
}

+ (NSDictionary *)TCHMember:(TCHMember *)member {
  if (!member) {
    return RCTNullIfNil(nil);
  }
  return @{
           @"identity": member.identity,
           @"lastConsumedMessageIndex": RCTNullIfNil(member.lastConsumedMessageIndex),
           @"lastConsumptionTimestamp": RCTNullIfNil(member.lastConsumptionTimestamp)
           };
}

+ (NSDictionary *)TCHChannel:(TCHChannel *)channel {
  if (!channel) {
    return RCTNullIfNil(nil);
  }
  return @{
           @"sid": channel.sid,
           @"friendlyName": RCTNullIfNil(channel.friendlyName),
           @"uniqueName": channel.uniqueName,
           @"status": @(channel.status),
           @"type": @(channel.type),
           @"attributes": RCTNullIfNil(channel.attributes),
           @"synchronizationStatus": @(channel.synchronizationStatus),
           @"dateCreated": RCTNullIfNil(channel.dateCreated),
           @"dateUpdated": RCTNullIfNil(channel.dateUpdated),
           @"createdBy": RCTNullIfNil(channel.createdBy)
           };
}

+ (NSDictionary *)TCHChannelDescriptor:(TCHChannelDescriptor *)channel {
    if (!channel) {
        return RCTNullIfNil(nil);
    }
    return @{
             @"sid": channel.sid,
             @"friendlyName": RCTNullIfNil(channel.friendlyName),
             @"uniqueName": channel.uniqueName,
             @"attributes": RCTNullIfNil(channel.attributes),
             @"messageCount": @(channel.messagesCount),
             @"membersCount": @(channel.membersCount),
             @"createdBy": RCTNullIfNil(channel.createdBy),
             @"dateCreated": channel.dateCreated == nil ? @"null" : @(channel.dateCreated.timeIntervalSince1970 * 1000),
             @"dateUpdated": channel.dateUpdated == nil ? @"null" : @(channel.dateUpdated.timeIntervalSince1970 * 1000),
             };
}

+ (NSArray *)TCHMembers:(NSArray<TCHMember *>*)members {
  if (!members) {
    return RCTNullIfNil(nil);
  }
  NSMutableArray *response = [NSMutableArray array];
  for (TCHMember *member in members) {
    [response addObject:[self TCHMember:member]];
  }
  return response;
}

+ (NSArray *)TCHChannels:(NSArray<TCHChannel *>*)channels {
    if (!channels) {
        return RCTNullIfNil(nil);
    }
    NSMutableArray *response = [NSMutableArray array];
    for (TCHChannel *channel in channels) {
        [response addObject:[self TCHChannel:channel]];
    }
    return response;
}

+ (NSArray *)TCHChannelDescriptors:(NSArray<TCHChannelDescriptor *>*)channels {
    if (!channels) {
        return RCTNullIfNil(nil);
    }
    NSMutableArray *response = [NSMutableArray array];
    for (TCHChannelDescriptor *channel in channels) {
        [response addObject:[self TCHChannelDescriptor:channel]];
    }
    return response;
}

+ (NSArray *)TCHMessages:(NSArray<TCHMessage *> *)messages {
  if (!messages) {
    return RCTNullIfNil(nil);
  }
  NSMutableArray *response = [NSMutableArray array];
  for (TCHMessage *message in messages) {
    [response addObject:[self TCHMessage:message]];
  }
  return response;
}

+ (NSDictionary *)TCHMemberPaginator:(TCHMemberPaginator *)paginator {
    if (!paginator) {
        return RCTNullIfNil(nil);
    }
    return @{
             @"hasNextPage": @(paginator.hasNextPage),
             @"items": [self TCHMembers:paginator.items]
             };
}

+ (NSDictionary *)TCHChannelDescriptorPaginator:(TCHChannelDescriptorPaginator *)paginator {
    if (!paginator) {
        return RCTNullIfNil(nil);
    }
    return @{
             @"hasNextPage": @(paginator.hasNextPage),
             @"items": [self TCHChannelDescriptors:paginator.items]
             };
}

+ (NSData *)dataWithHexString:(NSString *)hex {
  // Source:  https://opensource.apple.com/source/Security/Security-55471.14.18/libsecurity_transform/NSData+HexString.m
  char buf[3];
  buf[2] = '\0';
  NSAssert(0 == [hex length] % 2, @"Hex strings should have an even number of digits (%@)", hex);
  unsigned char *bytes = malloc([hex length]/2);
  unsigned char *bp = bytes;
  for (CFIndex i = 0; i < [hex length]; i += 2) {
      buf[0] = [hex characterAtIndex:i];
      buf[1] = [hex characterAtIndex:i+1];
      char *b2 = NULL;
      *bp++ = strtol(buf, &b2, 16);
      NSAssert(b2 == buf + 2, @"String should be all hex digits: %@ (bad digit around %d)", hex, i);
  }

  return [NSData dataWithBytesNoCopy:bytes length:[hex length]/2 freeWhenDone:YES];
}


@end
