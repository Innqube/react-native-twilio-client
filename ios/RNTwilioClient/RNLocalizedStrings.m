//
//  RNLogHelper.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 6/8/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import "RNLocalizedStrings.h"

@interface RNLocalizedStrings ()
@property(nonatomic, strong) NSString *languageCode;
@end

@implementation RNLocalizedStrings

RCT_EXPORT_MODULE();

static RNLocalizedStrings *sharedInstance = nil;

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

- (id)init {
    [RNLocalizedStrings sharedInstance];
    return self;
}

- (NSString *)translate:(NSString*) key {
    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    NSDictionary *translations = [preferences objectForKey:@"translations"];
    NSString *translation = [translations valueForKey: key];

    if (translation == nil) {
        if ([key isEqualToString:@"minutes"]) {
            translation = @"minutes";
        }
        if ([key isEqualToString:@"hour"]) {
            translation = @"hour";
        }
        if ([key isEqualToString:@"hours"]) {
            translation = @"hours (or more)";
        }
    }

    NSLog(@"[IIMobile - RNLocalizedStrings] get translation %@ for key %@", translation, key);
    return translation;
}

RCT_EXPORT_METHOD(configure: (NSString *) languageCode andTranslations: (NSDictionary *) translations) {
    NSLog(@"[IIMobile - RNLocalizedStrings] configure: language %@", languageCode);

    NSUserDefaults *preferences = [NSUserDefaults standardUserDefaults];
    [preferences setObject:translations forKey:@"translations"];
    [preferences setValue:languageCode forKey:@"languageCode"];
    [preferences synchronize];
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
