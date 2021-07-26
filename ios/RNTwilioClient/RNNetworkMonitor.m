//
//  RNReachability.m
//  RNTwilioClient
//
//  Created by Enrique Viard on 7/21/21.
//  Copyright © 2021 No Good Software Inc. All rights reserved.
//

#import "RNNetworkMonitor.h"
@import Foundation;
@import Network;

@interface RNNetworkMonitor()

@property (nonatomic, strong) nw_path_monitor_t monitor;
@property (nonatomic, strong) dispatch_queue_t monitorQueue;
@property (nonatomic, strong) nw_path_t path;
@property (nonatomic, strong) NSString *pathDescription;

@end

@implementation RNNetworkMonitor

- (void)startNetworkMonitoring {
    dispatch_queue_attr_t attrs = dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL, QOS_CLASS_UTILITY, DISPATCH_QUEUE_PRIORITY_DEFAULT);
    self.monitorQueue = dispatch_queue_create("com.ngs.network.monitor", attrs);
    
    self.monitor = nw_path_monitor_create();
    nw_path_monitor_set_queue(self.monitor, self.monitorQueue);
    nw_path_monitor_set_update_handler(self.monitor, ^(nw_path_t _Nonnull path) {
        self.path = path;
        nw_path_status_t status = nw_path_get_status(path);
        BOOL isWiFi = nw_path_uses_interface_type(path, nw_interface_type_wifi);
        BOOL isCellular = nw_path_uses_interface_type(path, nw_interface_type_cellular);
        self.pathDescription = isWiFi ? @"WIFI" : @"CELLULAR";
        
        NSDictionary *userInfo = @{
                                    @"path" : self.pathDescription,
                                    @"status" : @(status),
                                 };
        
        dispatch_async(dispatch_get_main_queue(), ^{
            NSLog(@"[IIMobile - RNNetworkMonitor] onNetworkStatusChange %@", userInfo);
            [NSNotificationCenter.defaultCenter postNotificationName:@"onNetworkStatusChange" object:nil userInfo:userInfo];
        });
    });
    
    nw_path_monitor_start(self.monitor);
}

- (void)stopNetworkMonitoring {
    nw_path_monitor_cancel(self.monitor);
}

- (NSString*)getCurrentPath {
    return self.pathDescription;
}

+ (NSString *)moduleName {
    return @"RNNetworkMonitor";
}

@end

