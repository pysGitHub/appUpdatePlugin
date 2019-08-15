//
//  UIViewController+Update.h
//  导航条
//
//  Created by 潘远生 on 2018/7/15.
//  Copyright © 2018年 潘远生. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface UIViewController (Update)
@property (nonatomic,copy)NSString * autoUploadUrl;
@property (nonatomic,copy)NSString * autoUploadMessage;
@property (nonatomic,copy)NSString * autoUploadVersion;
@property (nonatomic,copy)NSString * autoUploadTitle;
@property (nonatomic,copy)NSString * build;
@property (nonatomic,copy)NSString * cancelTitle;
@property (nonatomic,copy)NSString * buttonTitile;
-(void)autoUpdateAlert:(NSString *)string;
@end
