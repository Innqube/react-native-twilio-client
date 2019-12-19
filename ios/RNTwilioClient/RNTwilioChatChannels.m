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
#import "RCTConvert+TwilioChatClient.h"

@implementation RNTwilioChatChannels

RCT_EXPORT_MODULE()

+ (void)loadChannelFromSidOrUniqueName:(NSString *)sid :(void (^)(TCHResult *result, TCHChannel *channel))completion {
    TwilioChatClient *client = [[RNTwilioChatClient sharedInstance] client];
    [[client channelsList] channelWithSidOrUniqueName:sid completion:completion];
}

RCT_REMAP_METHOD(get, sidOrUniqueName:(NSString *)sidOrUniqueName get_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] get channel called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            resolve(@{
                    @"sid": channel.sid,
                    @"uniqueName": channel.uniqueName,
                    @"friendlyName": channel.friendlyName
                    });
        } else {
            reject(@"get-channel-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], nil);
        }
    }];
}

RCT_REMAP_METHOD(create, sidOrUniqueName:(NSString *)sidOrUniqueName friendlyName:(NSString *)friendlyName type:(NSString *)type attributes:(NSDictionary *)attributes create_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] create channel called with sidOrUniqueName: %@", sidOrUniqueName);

    if (sidOrUniqueName == nil || friendlyName == nil || type == nil) {
        reject(@"create-channel-error", @"Failed to create channel. Some parameters are null", nil);
    } else {
        TwilioChatClient *_twilioChatClient = [[RNTwilioChatClient sharedInstance] client];

        NSNumber *channelType = nil;
        if ([type isEqualToString:@"PRIVATE"]) {
            channelType = @(TCHChannelTypePrivate);
        } else if ([type isEqualToString:@"PUBLIC"]) {
            channelType = @(TCHChannelTypePublic);
        }

        NSDictionary *channelOptions = @{TCHChannelOptionUniqueName: sidOrUniqueName,
                                         TCHChannelOptionFriendlyName: friendlyName,
                                         TCHChannelOptionType: channelType};

        [_twilioChatClient.channelsList createChannelWithOptions:channelOptions
                                                             completion:^(TCHResult *result, TCHChannel *channel) {
           if ([result isSuccessful]) {
               resolve(@{
                       @"sid": channel.sid,
                       @"uniqueName": channel.uniqueName,
                       @"friendlyName": channel.friendlyName
                       });
           } else {
               reject(@"create-channel-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
           }
       }];
    }
}

RCT_REMAP_METHOD(join, sidOrUniqueName:(NSString *)sidOrUniqueName join_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] join channel called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
             [channel joinWithCompletion:^(TCHResult *result) {
                if (result.isSuccessful) {
                    resolve(@[@TRUE]);
                } else {
                    reject(@"join-channel-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
             }];
        } else {
            reject(@"join-channel-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(leave, sidOrUniqueName:(NSString *)sidOrUniqueName leave_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] leave channel called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
             [channel leaveWithCompletion:^(TCHResult *result) {
                if (result.isSuccessful) {
                    resolve(@[@TRUE]);
                }
                else {
                    reject(@"leave-channel-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
             }];
        } else {
            reject(@"leave-channel-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(typing, sidOrUniqueName:(NSString *)sidOrUniqueName) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] typing called with sidOrUniqueName: %@", sidOrUniqueName);

    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel typing];
        }
    }];
}

RCT_REMAP_METHOD(getUnconsumedMessagesCount, sidOrUniqueName:(NSString *)sidOrUniqueName unread_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getUnconsumedMessagesCount called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            if (channel.messages.lastConsumedMessageIndex) {
                    [channel getUnconsumedMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                        if ([result isSuccessful]) {
                            resolve(@(count));
                        } else {
                            reject(@"unconsumed-messages-error", @"getUnconsumedMessagesCount failed", nil);
                        }
                    }];
                } else {
                      [channel getMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                          if ([result isSuccessful]) {
                              resolve(@(count));
                          } else {
                              reject(@"unconsumed-messages-error", @"getUnconsumedMessagesCount failed", nil);
                          }
                      }];
                }
            } else {
                reject(@"unconsumed-messages-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
            }
     }];
}

RCT_REMAP_METHOD(getMessagesCount, sidOrUniqueName:(NSString *)sidOrUniqueName countResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMessagesCount called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel getMessagesCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@(count));
                } else {
                    reject(@"messages-count-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
             }];
        } else {
            reject(@"messages-count-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
     }];
}

RCT_REMAP_METHOD(getMembersCount, sidOrUniqueName:(NSString *)sidOrUniqueName members_count_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMembersCount called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel getMembersCountWithCompletion:^(TCHResult *result, NSUInteger count) {
                if (result.isSuccessful) {
                    resolve(@(count));
                }
                else {
                    reject(@"get-members-count-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
            }];
        } else {
            reject(@"get-members-count-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(getLastMessages, sidOrUniqueName:(NSString *)sidOrUniqueName count: (nonnull NSNumber *)count last_messages_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getLastMessages called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages getLastMessagesWithCount:count.longValue
                     completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                         if ([result isSuccessful]) {
                             resolve([RCTConvert TCHMessages: messages]);
                         } else {
                             reject(@"get-last-messages-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                         }
                     }];
        } else {
             reject(@"get-last-messages-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
     }];
}

RCT_REMAP_METHOD(sendMessage, sidOrUniqueName:(NSString *)sidOrUniqueName message:(NSString*)message send_message_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] sendMessage called with message: %@", message);

    if (message == nil) {
        reject(@"send-message-error", @"Message cannot be null", nil);
    } else {
        [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
            if (result.isSuccessful) {
                TCHMessageOptions *messageOptions = [[TCHMessageOptions new] withBody:message];
                [channel.messages sendMessageWithOptions:messageOptions completion:^(TCHResult * _Nonnull result, TCHMessage * _Nullable message) {
                    if (result.isSuccessful) {
                        resolve(@[@TRUE]);
                    } else {
                        reject(@"send-message-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                    }
                }];
            } else {
                reject(@"send-message-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
            }
        }];
    }
}

RCT_REMAP_METHOD(getMessagesBefore, sidOrUniqueName:(NSString *)sidOrUniqueName beforeIndex: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMessagesBefore called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages getMessagesBefore:(index.intValue)
                                      withCount:count.longValue
                                     completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                          if ([result isSuccessful]) {
                                              resolve([RCTConvert TCHMessages: messages]);
                                          } else {
                                              reject(@"get-messages-before-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                                          }
                                      }];
        } else {
            reject(@"get-messages-before-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(getMessagesAfter, sidOrUniqueName:(NSString *)sidOrUniqueName afterIndex: (nonnull NSNumber *)index count: (nonnull NSNumber *) count messages_before_resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMessagesAfter called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages getMessagesAfter:(index.intValue)
                                     withCount:count.longValue
                                    completion:^(TCHResult *result, NSArray<TCHMessage *> *messages) {
                                      if ([result isSuccessful]) {
                                          resolve([RCTConvert TCHMessages: messages]);
                                      } else {
                                          reject(@"get-messages-after-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                                      }
                                  }];
        } else {
            reject(@"get-messages-after-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(getLastConsumedMessageIndex, sidOrUniqueName:(NSString *)sidOrUniqueName consumedMessageIndexResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getLastConsumedMessageIndex called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            if (channel.messages.lastConsumedMessageIndex) {
                resolve(channel.messages.lastConsumedMessageIndex);
            } else {
                resolve(@"0");
            }
        } else {
            reject(@"get-last-consumed-index-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(setNoMessagesConsumed, sidOrUniqueName:(NSString *)sidOrUniqueName noMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] setNoMessagesConsumed called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages setNoMessagesConsumedWithCompletion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@(count));
                } else {
                    reject(@"set-no-message-consumed-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
            }];
        } else {
            reject(@"set-no-message-consumed-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(setAllMessagesConsumed, sidOrUniqueName:(NSString *)sidOrUniqueName allMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] setAllMessagesConsumed called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages setAllMessagesConsumedWithCompletion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@(count));
                } else {
                    reject(@"set-all-message-consumed-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
            }];
        } else {
            reject(@"set-all-message-consumed-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(setLastConsumedMessage, sidOrUniqueName:(NSString *)sidOrUniqueName withIndex:(nonnull NSNumber *)index setMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] setLastConsumedMessage called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages setLastConsumedMessageIndex:index
                                                    completion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@(count));
                } else {
                    reject(@"set-last-consumed-message-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
            }];
        } else {
            reject(@"set-last-consumed-message-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(advanceLastConsumedMessage, sidOrUniqueName:(NSString *)sidOrUniqueName withIndex:(nonnull NSNumber *)index advanceMessageConsumedResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] advanceLastConsumedMessage called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.messages advanceLastConsumedMessageIndex:index
                                                    completion:^(TCHResult *result, NSUInteger count) {
                if ([result isSuccessful]) {
                    resolve(@(count));
                } else {
                    reject(@"advance-last-consumed-message-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
            }];
        } else {
            reject(@"advance-last-consumed-message-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

RCT_REMAP_METHOD(getMembers, sidOrUniqueName:(NSString *)sidOrUniqueName getMembersResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    NSLog(@"[IIMobile - RNTwilioChatChannels] getMembers called with sidOrUniqueName: %@", sidOrUniqueName);
    [RNTwilioChatChannels loadChannelFromSidOrUniqueName:sidOrUniqueName :^(TCHResult *result, TCHChannel *channel) {
        if (result.isSuccessful) {
            [channel.members membersWithCompletion:^(TCHResult* result, TCHMemberPaginator* paginator) {
                if ([result isSuccessful]) {
                    resolve([RCTConvert TCHMembers: paginator.items]);
                } else {
                    reject(@"get-members-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
                }
            }];
        } else {
            reject(@"get-members-error", [result.error.userInfo objectForKey:@"NSLocalizedDescription"], result.error);
        }
    }];
}

+(BOOL)requiresMainQueueSetup {
    return NO;
}

@end
