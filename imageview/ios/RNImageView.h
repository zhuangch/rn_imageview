//
//  RNImageView.h
//  MICEN
//
//  Created by syj on 2020/12/22.
//  Copyright © 2020 FocusTechnology Co.,LTD. All rights reserved.
//

#import <React/RCTView.h>
#import <React/RCTResizeMode.h>

NS_ASSUME_NONNULL_BEGIN

@class RCTBridge;
@class RCTImageSource;

@interface RNImageView : RCTView

//
@property (nonatomic, assign) UIEdgeInsets capInsets;
@property (nonatomic, strong) UIImage *defaultImage;
@property (nonatomic, assign) UIImageRenderingMode renderingMode;
@property (nonatomic, copy) NSArray<RCTImageSource *> *source;
@property (nonatomic, assign) CGFloat blurRadius;
@property (nonatomic, assign) RCTResizeMode resizeMode;
@property (nonatomic, copy) NSString  *defaultSource;
@property (nonatomic, copy) UIColor  *foregroundColor;

@end

NS_ASSUME_NONNULL_END
