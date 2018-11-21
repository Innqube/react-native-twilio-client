//
//  RNTwilioClient.h
//  TwilioClient
//

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <PushKit/PushKit.h>

@interface RNTwilioClient : RCTEventEmitter
-(void)didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(NSString *)type withState:(NSString *)pending;
-(void)didUpdatePushCredentials:(PKPushCredentials *)credentials forType:(NSString *)type;
+(id)sharedInstance;
@end
