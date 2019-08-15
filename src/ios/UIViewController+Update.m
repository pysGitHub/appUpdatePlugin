//
//
//  Created by 潘远生 on 2018/7/15.
//  Copyright © 2018年 潘远生. All rights reserved.
//

#import "UIViewController+Update.h"
#import <objc/runtime.h>

static NSString *autoUploadUrl_Key = @"autoUploadUrl";
static NSString *autoUploadMessage_Key = @"autoUploadMessage";
static NSString *autoUploadVersion_Key = @"autoUploadVersion";
static NSString *autoUploadTitle_Key = @"autoUploadTitle";
static NSString *build_Key = @"build";
static NSString *cancelTitle_Key = @"cancelTitle";
static NSString *buttonTitile_Key = @"buttonTitile";

@implementation UIViewController (Update)

-(void)autoUpdateAlert:(NSString *)string{
    
    NSArray *appLanguages = [[NSUserDefaults standardUserDefaults] objectForKey:@"AppleLanguages"];
    NSString *languageName = [appLanguages objectAtIndex:0];
    if ([languageName containsString:@"en"]) {
        self.cancelTitle = @"Cancle";
        self.buttonTitile = @"OK";
    }else if([languageName containsString:@"zh-Hans"]){
        self.cancelTitle = @"取消";
        self.buttonTitile = @"确定";
    }else{
        self.cancelTitle = @"取消";
        self.buttonTitile = @"確定";
    }
    self.build = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    NSString * str = [string stringByAppendingString:@"/~pts/dispatcher/app/get_update.php?my_platform=iOS&my_version="];
    NSString * ptsUrl = [str stringByAppendingString:self.build];
    // 将空格转义
    NSString *charactersToEscape = @" ";
    NSCharacterSet *allowedCharacters = [[NSCharacterSet characterSetWithCharactersInString:charactersToEscape] invertedSet];
    NSString * updateUrlStr = [ptsUrl stringByAddingPercentEncodingWithAllowedCharacters:allowedCharacters];
    
    //GCD异步实现
    dispatch_queue_t q1 = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    dispatch_async(q1, ^{
        NSURL *url = [NSURL URLWithString:updateUrlStr];
        //NSURLRequestReloadIgnoringLocalCacheData 清除缓存
        NSURLRequest *request = [NSURLRequest requestWithURL:url cachePolicy:NSURLRequestReloadIgnoringLocalCacheData timeoutInterval:30.0];
        
        //使用NSURLSession获取网络返回的Json并处理
        NSURLSession *session = [NSURLSession sharedSession];
        NSURLSessionDataTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
            if (data) {
                NSMutableDictionary * autoDicData = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingMutableContainers error:nil];
                // NSLog(@"%@",autoDicData);
                if (autoDicData) {
                    self.autoUploadUrl = autoDicData[@"ios"][@"package"];
                    self.autoUploadMessage = autoDicData[@"ios"][@"description"];
                    self.autoUploadVersion = autoDicData[@"ios"][@"version"];
                    self.autoUploadTitle = autoDicData[@"ios"][@"summary"];
                    //NSLog(@"version = %@  self.build = %@",version,self.build);
                    
                    if ((![self.autoUploadVersion isEqualToString:self.build])) {
                        //更新UI操作需要在主线程
                        dispatch_async(dispatch_get_main_queue(), ^{
                            [self mainQueue:self.autoUploadUrl title:self.autoUploadTitle message:self.autoUploadMessage url:self.autoUploadUrl forceUpdate:autoDicData[@"ios"][@"update"]];
                        });
                    }
                }
            }
        }];
        [task resume];
    });
}




- (void)mainQueue:(NSString *)urlStr title:(NSString *)updateTitle message:(NSString *)message url:(NSString *)updateUrl forceUpdate:(NSString *)forceUpdate{
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:self.autoUploadTitle message:self.autoUploadMessage preferredStyle:UIAlertControllerStyleAlert];
    
    if (![forceUpdate isEqualToString:@"true"]) {
        [alertController addAction:[UIAlertAction actionWithTitle:self.cancelTitle style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
        }]];
    }
    
    
    
    [alertController addAction:[UIAlertAction actionWithTitle:self.buttonTitile style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        if (@available(iOS 10.0, *)) {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:self.autoUploadUrl] options:@{} completionHandler:nil];
        }else{
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:self.autoUploadUrl]];
        }
        [self performSelector:@selector(killApp) withObject:nil afterDelay:0.02];
        
    }]];
    
    
    UIViewController * vc1 = [self topViewController];
    [vc1 presentViewController:alertController animated:YES completion:nil];
}

- (void)killApp{
    exit(0);
}


- (UIViewController *)topViewController{
    return [self topViewControllerWithRootViewController:[UIApplication sharedApplication].keyWindow.rootViewController];
}

- (UIViewController *)topViewControllerWithRootViewController:(UIViewController *)rootViewController{
    if ([rootViewController isKindOfClass:[UITabBarController class]]) {
        UITabBarController * tabbarController = (UITabBarController *)rootViewController;
        return [self topViewControllerWithRootViewController:tabbarController.selectedViewController];
    }else if ([rootViewController isKindOfClass:[UINavigationController class]]){
        UINavigationController * nav = (UINavigationController *)rootViewController;
        return [self topViewControllerWithRootViewController:nav.visibleViewController];
    }else if (rootViewController.presentedViewController){
        UIViewController * presentedViewController = rootViewController.presentedViewController;
        return [self topViewControllerWithRootViewController:presentedViewController];
    }else{
        return rootViewController;
    }
}


#pragma mark ----属性赋值

- (void)setAutoUploadUrl:(NSString *)autoUploadUrl{
    objc_setAssociatedObject(self, &autoUploadUrl_Key, autoUploadUrl, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)autoUploadUrl{
    return objc_getAssociatedObject(self, &autoUploadUrl_Key);
}


- (void)setAutoUploadMessage:(NSString *)autoUploadMessage{
    objc_setAssociatedObject(self, &autoUploadMessage_Key, autoUploadMessage, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)autoUploadMessage{
    return objc_getAssociatedObject(self, &autoUploadMessage_Key);
}


- (void)setAutoUploadTitle:(NSString *)autoUploadTitle{
    objc_setAssociatedObject(self, &autoUploadTitle_Key, autoUploadTitle, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)autoUploadTitle{
    return objc_getAssociatedObject(self, &autoUploadTitle_Key);
}


- (void)setAutoUploadVersion:(NSString *)autoUploadVersion{
    objc_setAssociatedObject(self, &autoUploadVersion_Key, autoUploadVersion, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)autoUploadVersion{
    return objc_getAssociatedObject(self, &autoUploadVersion_Key);
}


- (void)setBuild:(NSString *)build{
    objc_setAssociatedObject(self, &build_Key, build, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)build{
    return objc_getAssociatedObject(self, &build_Key);
}


- (void)setCancelTitle:(NSString *)cancelTitle{
    objc_setAssociatedObject(self, &cancelTitle_Key, cancelTitle, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)cancelTitle{
    return objc_getAssociatedObject(self, &cancelTitle_Key);
}


- (void)setButtonTitile:(NSString *)buttonTitile{
    objc_setAssociatedObject(self, &buttonTitile_Key, buttonTitile, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
- (NSString *)buttonTitile{
    return objc_getAssociatedObject(self, &buttonTitile_Key);
}


@end
