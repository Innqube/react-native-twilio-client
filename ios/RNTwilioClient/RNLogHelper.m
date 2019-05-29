//
//  RNLogHelper.m
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 5/22/19.
//  Copyright © 2019 No Good Software Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RNLogHelper.h"

@implementation RNLogHelper 

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(log:
                  (NSString *) message) {
    if (message != nil) {
        NSLog(@"[IIMobile - RNLogHelper] %@", message);
    } else {
        NSLog(@"[IIMobile - RNLogHelper] NULL");
    }
}

@end
