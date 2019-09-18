//
//  RNTwilioChatClient.m
//  RN TwilioChat
//
//  Created Enrique Viard on 9/17/19.
//  Copyright © 2019 No Good Software Inc. All rights reserved.
//

#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>
#import "RNEventEmitterHelper.h"
#import <React/RCTBridgeModule.h>
#import "RNTwilioChatClient.h"
#import "RNTwilioChatChannels.h"

@implementation RNTwilioChatChannels

RCT_EXPORT_MODULE()

+ (void)loadChannelFromSidOrUniqueName:(NSString *)sid :(void (^)(TCHResult *result, TCHChannel *channel))completion {
    RNTwilioChatClient *client = [[RNTwilioChatClient sharedManager] client];
    [[client channelsList] channelWithSidOrUniqueName:sid completion:completion];
}

RCT_REMAP_METHOD(get, sidOrUniqueName:(NSString *)sidOrUniqueName get_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] get channel called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            resolve(@{
                    @"sid": channel.sid,
                    @"uniqueName": channel.uniqueName,
                    @"friendlyName": channel.friendlyName
                    });
        } else {
            reject(@"get-channel-error", @"get channel failed with error", result.error);
        }
    }];
}

RCT_REMAP_METHOD(create, sidOrUniqueName:(NSString *)sidOrUniqueName friendlyName:(NSString *)friendlyName type:(NSString *)type create_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] create channel called with sidOrUniqueName: %@", sidOrUniqueName);

    if (sidOrUniqueName == nil || friendlyName == nil || type == nil) {
        reject(@"create-channel-error", @"Failed to create channel. Some parameters are null", nil);
    } else {
        RNTwilioChatClient *_twilioChatClient = [RNTwilioChatClient sharedManager];

        NSNumber *channelType = nil;
        if ([type isEqualToString:@"private"]) {
            channelType = @(TCHChannelTypePrivate);
        } else if ([type isEqualToString:@"public"]) {
            channelType = @(TCHChannelTypePublic);
        }

        NSDictionary *channelOptions = @{TCHChannelOptionUniqueName: uniqueName,
                                         TCHChannelOptionFriendlyName: friendlyName,
                                         TCHChannelOptionType: channelType};

        [_twilioChatClient.client.channelsList createChannelWithOptions:channelOptions
                                                             completion:^(TCHResult *result, TCHChannel *channel) {
           if ([result isSuccessful]) {
               resolve(@{
                       @"sid": channel.sid,
                       @"uniqueName": channel.uniqueName,
                       @"friendlyName": channel.friendlyName
                       });
           } else {
               reject(@"create-channel-error", @"Failed to create channel", result.error);
           }
       }];
    }
}

RCT_REMAP_METHOD(join, sidOrUniqueName:(NSString *)sidOrUniqueName friendlyName:(NSString *)friendlyName type:(NSString *)type join_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] join channel called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
             [channel joinWithCompletion:^(TCHResult *result) {
                if (result.isSuccessful) {
                    resolve(@[@TRUE]);
                } else {
                    reject(@"join-channel-error", @"joinWithCompletion failed with error", result.error);
                }
             }];
        } else {
            reject(@"join-channel-error", @"loadChannelFromSid failed with error", result.error);
        }
    }];
}

RCT_REMAP_METHOD(leave, sidOrUniqueName:(NSString *)sidOrUniqueName leave_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] leave channel called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
             [channel leaveWithCompletion:^(TCHResult *result) {
                if (result.isSuccessful) {
                    resolve(@[@TRUE]);
                }
                else {
                    reject(@"leave-channel-error", @"leaveWithCompletion failed with error", result.error);
                }
             }];
        } else {
            reject(@"leave-channel-error", @"loadChannelFromSid failed with error", result.error);
        }
    }];
}

RCT_EXPORT_METHOD(typing, sidOrUniqueName:(NSString *)sidOrUniqueName) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] typing called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel typing];
        }
    }];
}

RCT_REMAP_METHOD(getUnconsumedMessagesCount, sidOrUniqueName:(NSString *)sidOrUniqueName unread_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getUnconsumedMessagesCount called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            if (self.channel.messages.lastConsumedMessageIndex) {
                    [self.channel getUnconsumedMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                        if ([result isSuccessful]) {
                            resolve(@(count));
                        } else {
                            reject(@"unread-messages-error", @"getUnconsumedMessagesCount failed", nil);
                        }
                    }];
                } else {
                      [self.channel getMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                          if ([result isSuccessful]) {
                              resolve(@(count));
                          } else {
                              reject(@"unread-messages-error", @"getUnconsumedMessagesCount failed", nil);
                          }
                      }];
                }
            } else {
                reject(@"unread-messages-error", @"getUnconsumedMessagesCount failed with error", result.error);
            }
     }];
}

RCT_REMAP_METHOD(getMessagesCount, sidOrUniqueName:(NSString *)sidOrUniqueName countResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMessagesCount called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel getMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@(count));
                } else {
                    reject(@"messages-count-error", @"getMessagesCount failed with error", result.error);
                }
             }];
        } else {
            reject(@"messages-count-error", @"getMessagesCount failed with error", result.error);
        }
     }];
}

RCT_REMAP_METHOD(getMembersCount, sidOrUniqueName:(NSString *)sidOrUniqueName members_count_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMembersCount called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel getMembersCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                if (result.isSuccessful) {
                    resolve(@(count));
                }
                else {
                    reject(@"get-members-count-error", @"getMembersCount failed with error", result.error);
                }
            }];
        } else {
            reject(@"get-members-count-error", @"getMembersCount failed with error", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getLastMessages, sidOrUniqueName:(NSString *)sidOrUniqueName count: (nonnull NSNumber *)count last_messages_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getLastMessages called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages getLastMessagesWithCount:count.longValue
                     completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                         if ([result isSuccessful]) {
                             resolve([self buildMessageJsonArray:messages]);
                         } else {
                             reject(@"get-last-messages-error", @"getLastMessages failed with error", result.error);
                         }
                     }];
        } else {
             reject(@"get-last-messages-error", @"getLastMessages failed with error", result.error);
        }
     }];
}

RCT_REMAP_METHOD(sendMessage, message:(NSString*)message send_message_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] sendMessage called with message: %@", message);

    if (message == nil) {
        reject(@"send-message-error", @"Message cannot be null", nil);
    } else {
        [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
            if (result.isSuccessful) {
                TCHMessageOptions *messageOptions = [[TCHMessageOptions new] withBody:message];
                [channel.messages sendMessageWithOptions:messageOptions completion:^(TCHResult * _Nonnull result, TCHMessage * _Nullable message) {
                    if (result.isSuccessful) {
                        resolve(@[@TRUE]);
                    } else {
                        reject(, @"Message not sent", result.error);
                    }
                }];
            } else {
                reject(@"send-message-error", @"Message not sent", result.error);
            }
    }
}

RCT_REMAP_METHOD(getMessagesBefore, sidOrUniqueName:(NSString *)sidOrUniqueName beforeIndex: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMessagesBefore called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages getMessagesBefore:(index.intValue - 1)
                                      withCount:count.longValue
                                     completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                          if ([result isSuccessful]) {
                                              resolve([self buildMessageJsonArray:messages]);
                                          } else {
                                              reject(@"get-messages-before-error", @"getMessagesBefore failed", result.error);
                                          }
                                      }];
        } else {
            reject(@"get-messages-before-error", @"getMessagesBefore failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getMessagesAfter, sidOrUniqueName:(NSString *)sidOrUniqueName afterIndex: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMessagesAfter called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages getMessagesAfter:(index.intValue + 1)
                                     withCount:count.longValue
                                    completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                      if ([result isSuccessful]) {
                                          resolve([self buildMessageJsonArray:messages]);
                                      } else {
                                          reject(@"get-messages-after-error", @"getMessagesAfter failed", result.error);
                                      }
                                  }];
        } else {
            reject(@"get-messages-after-error", @"getMessagesAfter failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(getLastConsumedMessageIndex, sidOrUniqueName:(NSString *)sidOrUniqueName consumedMessageIndexResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
   NSLog(@"[IIMobile - RNTwilioChatChannels] getLastConsumedMessageIndex called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            if (channel.messages.lastConsumedMessageIndex) {
                resolve(channel.messages.lastConsumedMessageIndex);
            } else {
                resolve(@"0");
            }
        } else {
            reject(@"get-last-consumed-index-error", @"getLastConsumedMessageIndex failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(setNoMessagesConsumed, sidOrUniqueName:(NSString *)sidOrUniqueName noMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] setNoMessagesConsumed called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages setNoMessagesConsumedWithCompletion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@[@TRUE]);
                } else {
                    reject(@"set-no-message-consumed-error", @"setNoMessagesConsumed failed", result.error);
                }
            }];
        } else {
            reject(@"set-no-message-consumed-error", @"setNoMessagesConsumed failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(setAllMessagesConsumed, sidOrUniqueName:(NSString *)sidOrUniqueName allMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] setAllMessagesConsumed called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages setAllMessagesConsumedWithCompletion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@[@TRUE]);
                } else {
                    reject(@"set-all-message-consumed-error", @"setAllMessagesConsumed failed", result.error);
                }
            }];
        } else {
            reject(@"set-all-message-consumed-error", @"setAllMessagesConsumed failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(setLastConsumedMessage, sidOrUniqueName:(NSString *)sidOrUniqueName withIndex:(nonnull NSNumber *)index setMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] setLastConsumedMessage called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages setLastConsumedMessageIndex:index
                                                    completion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@[@TRUE]);
                } else {
                    reject(@"set-last-consumed-message-error", @"setLastConsumedMessage failed", result.error);
                }
            }];
        } else {
            reject(@"set-last-consumed-message-error", @"setLastConsumedMessage failed", result.error);
        }
    }];
}

RCT_REMAP_METHOD(advanceLastConsumedMessage, sidOrUniqueName:(NSString *)sidOrUniqueName withIndex:(nonnull NSNumber *)index advanceMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] advanceLastConsumedMessage called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSid:sid :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages advanceLastConsumedMessageIndex:index
                                                    completion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@[@TRUE]);
                } else {
                    reject(@"advance-last-consumed-message-error", @"advanceLastConsumedMessage failed", result.error);
                }
            }];
        } else {
            reject(@"advance-last-consumed-message-error", @"advanceLastConsumedMessage failed", result.error);
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

+(BOOL)requiresMainQueueSetup {
    return NO;
}

@end
