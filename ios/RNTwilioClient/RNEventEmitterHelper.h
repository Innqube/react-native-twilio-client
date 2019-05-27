//
//  RNEventEmitterHelper.h
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 5/22/19.
//  Copyright © 2019 No Good Software Inc. All rights reserved.
//

#ifndef RNEventEmitterHelper_h
#define RNEventEmitterHelper_h

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RNEventEmitterHelper : RCTEventEmitter<RCTBridgeModule>

+ (void)emitEventWithName:(NSString *)name andPayload:(NSDictionary *)payload;

@end
#endif /* RNEventEmitterHelper_h */
