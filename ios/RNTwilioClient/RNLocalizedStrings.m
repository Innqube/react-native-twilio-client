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
@property(nonatomic, strong) NSDictionary *translations;
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
    return [RNLocalizedStrings sharedInstance];
}

- (NSString *)translate:(NSString*) key {
    NSString *translation = [self.translations valueForKey: key];
    NSLog(@"[IIMobile - RNLocalizedStrings] get translation %@ for key %@", translation, key);
    return translation;
}

RCT_EXPORT_METHOD(configure: (NSString *) languageCode andTranslations: (NSDictionary *) translations) {
    NSLog(@"[IIMobile - RNLocalizedStrings] configure %@", languageCode);
    self.languageCode = languageCode;
    self.translations = translations;
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
