//
//  RNLogHelper.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 6/8/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RNLocalizedStrings.h"

@interface RNLocalizedStrings ()
@property(nonatomic, strong) NSString *languageCode;
@property(nonatomic, strong) NSDictionary *translations;
@end

@implementation RNLocalizedStrings

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

-(id) init {
    return [RNLocalizedStrings sharedInstance];
}

RCT_EXPORT_MODULE();

- (NSString *)translate:(NSString*) key {
    return [self.translations valueForKey: key];
}


RCT_EXPORT_METHOD(configure: (NSString *) languageCode andTranslations: (NSDictionary *) translations) {
    self.languageCode = languageCode;
    self.translations = translations;
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

@end
