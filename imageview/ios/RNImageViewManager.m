//
//  RNImageViewManager.m
//  MICEN
//
//  Created by syj on 2020/12/24.
//  Copyright © 2020 FocusTechnology Co.,LTD. All rights reserved.
//

#import "RNImageViewManager.h"
#import "RNImageView.h"

@implementation RNImageViewManager

RCT_EXPORT_MODULE(RNImageView)


RCT_EXPORT_VIEW_PROPERTY(capInsets, UIEdgeInsets);

RCT_EXPORT_VIEW_PROPERTY(defaultImage, UIImage);

RCT_EXPORT_VIEW_PROPERTY(renderingMode, UIImageRenderingMode);

RCT_EXPORT_VIEW_PROPERTY(source, NSArray<RCTImageSource *>);

RCT_EXPORT_VIEW_PROPERTY(blurRadius, CGFloat);

RCT_EXPORT_VIEW_PROPERTY(resizeMode, RCTResizeMode);

RCT_EXPORT_VIEW_PROPERTY(defaultSource, NSString *);

RCT_EXPORT_VIEW_PROPERTY(foregroundColor, UIColor);

- (UIView *)view
{
    return [[RNImageView alloc] init];
}

@end
