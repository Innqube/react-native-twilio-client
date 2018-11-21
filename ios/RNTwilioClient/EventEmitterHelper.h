//
//  EventEmitterHelper.h
//  Interpreter Intelligence
//
//  Created by Enrique Viard on 11/16/18.
//  Copyright © 2018 Facebook. All rights reserved.
//

#ifndef EventEmitterHelper_h
#define EventEmitterHelper_h

#import <React/RCTEventEmitter.h>

@interface EventEmitterHelper : RCTEventEmitter<RCTBridgeModule>

+ (void)emitEventWithName:(NSString *)name andPayload:(NSDictionary *)payload;

@end
#endif /* EventEmitterHelper_h */
