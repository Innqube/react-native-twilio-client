//
//  RNTwilioMos.m
//  RNTwilioClient
//
//  Created by Enrique Viard on 7/23/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import "RNTwilioMos.h"

@interface RNTwilioMos()

@property (nonatomic, strong) NSTimer *timer;
@property (nonatomic, strong) TVOCall *call;
@property (nonatomic) __block double max;
@property (nonatomic) __block double min;
@property (nonatomic) __block double acc;
@property (nonatomic) __block int counter;

@end

@implementation RNTwilioMos

- (id)initWithTVOCall:(TVOCall*) call {
    self = [super init];
    self.call = call;
    self.acc = 0;
    self.min = 0;
    self.max = 0;
    self.counter = 0;
    
    self.timer = [NSTimer scheduledTimerWithTimeInterval:5
                                                 target:self
                                               selector:@selector(updateMos)
                                               userInfo:nil
                                                repeats:YES];
    NSLog(@"[IIMobile - RNTwilioMos] TIMER STARTED");
    
    return self;
}

- (void)updateMos {
    NSLog(@"[IIMobile - RNTwilioMos] updateMos");
    
    [self.call getStatsWithBlock:^(NSArray<TVOStatsReport *> * _Nonnull statsReports) {
        for (TVOStatsReport *statReport in statsReports) {
            for (TVORemoteAudioTrackStats *remoteAudioTrack in statReport.remoteAudioTrackStats) {
                NSLog(@"[IIMobile - RNTwilioMos] AUDIO_LEVEL: %tu", remoteAudioTrack.audioLevel);
                NSLog(@"[IIMobile - RNTwilioMos] JITTER: %tu", remoteAudioTrack.jitter);
                NSLog(@"[IIMobile - RNTwilioMos] MOS: %f", remoteAudioTrack.mos);

                double mos = remoteAudioTrack.mos;
                self.acc  = self.acc + remoteAudioTrack.mos;
                self.counter++;
                
                if (self.min == 0 || self.min > mos) {
                    self.min = mos;
                }
                if (self.max == 0 || self.max < mos) {
                    self.max = mos;
                }

                NSLog(@"[IIMobile - RNTwilioMos] ACC: %f", self.acc);
                NSLog(@"[IIMobile - RNTwilioMos] COUNTER: %i", self.counter);
            }
        }
    }];
}

- (void)stop {
    [self.timer invalidate];
}

- (double)getAverageMos {
    NSLog(@"[IIMobile - RNTwilioMos] ACCUMULATOR %f", self.acc);
    if (self.counter == 0) {
        NSLog(@"[IIMobile - RNTwilioMos] CANNOT GET AVERAGE MOS, COUNTER IS 0");
        return 0;
    }

    NSLog(@"[IIMobile - RNTwilioMos] %f AVERAGE MOS BASED ON %i RESULTS", self.acc / self.counter, self.counter);
    return self.acc / self.counter;
}

- (double)getMinMos {
    NSLog(@"[IIMobile - RNTwilioMos] %f MIN MOS BASED ON %i RESULTS", self.min, self.counter);
    return self.min;
}

- (double)getMaxMos {
    NSLog(@"[IIMobile - RNTwilioMos] %f MAX MOS BASED ON %i RESULTS", self.max, self.counter);
    return self.max;
}

+ (NSString *)moduleName {
    return @"RNTwilioMos";
}

@end

