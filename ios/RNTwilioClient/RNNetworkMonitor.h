//
//  RNNetworkMonitor.h
//  RNTwilioClient
//
//  Created by Enrique Viard on 7/21/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface RNNetworkMonitor: NSObject<RCTBridgeModule>
- (void)startNetworkMonitoring;
- (void)stopNetworkMonitoring;
@end
