//
//  RNLocalizedStrings.h
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 8/6/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#ifndef RNAudioManager_h
#define RNAudioManager_h

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface RNLocalizedStrings : NSObject<RCTBridgeModule>
+ (id)sharedInstance;
- (NSString *)translate:(NSString*) key;
@end
#endif /* RNLocalizedStrings_h */
