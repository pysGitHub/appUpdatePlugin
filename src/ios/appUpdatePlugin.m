/********* appUpdatePlugin.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>

@interface appUpdatePlugin : CDVPlugin {
  // Member variables go here.
}

- (void)autoUpdateVersion:(CDVInvokedUrlCommand*)command;
@end

@implementation appUpdatePlugin

- (void)autoUpdateVersion:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* echo = [command.arguments objectAtIndex:0];
    UIViewController * vc = [[UIViewController alloc] init];
    [vc autoUpdateAlert:echo];
    
    if (echo != nil && [echo length] > 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
