//
//  RNTwilioMos.h
//  Pods
//
//  Created by Enrique Viard on 7/23/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
@import TwilioVoice;

@interface RNTwilioMos: NSObject<RCTBridgeModule>
-(id)initWithTVOCall:(TVOCall*) call;
-(void)stop;
-(double)getMaxMos;
-(double)getMinMos;
-(double)getAverageMos;
@end
