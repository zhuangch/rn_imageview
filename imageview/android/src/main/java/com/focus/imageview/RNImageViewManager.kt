package com.micen.react.bridge.viewmanager.imageview

import android.graphics.Color
import android.graphics.PorterDuff
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.react.bridge.JSApplicationIllegalArgumentException
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.PixelUtil
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewProps
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.annotations.ReactPropGroup
import com.facebook.react.views.image.*
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper
import com.facebook.yoga.YogaConstants

/**********************************************************
 * @文件作者：xiongjiangwei
 * @创建时间：2020/12/22 16:29
 * @文件描述：自定义的ImageView，使用Glide异步加载图片
 * @修改历史：2020/12/22 创建初始版本
 **********************************************************/
class RNImageViewManager : SimpleViewManager<RNCardView?> {
    override fun getName(): String {
        return "RNImageView"
    }

    private var mDraweeControllerBuilder: AbstractDraweeControllerBuilder<*, *, *, ImageInfo?>?
    private var mGlobalImageLoadListener: GlobalImageLoadListener? = null
    private val mCallerContext: Any?
    private val mCallerContextFactory: ReactCallerContextFactory?

    constructor(
            draweeControllerBuilder: AbstractDraweeControllerBuilder<*, *, *, ImageInfo?>?,
            callerContextFactory: ReactCallerContextFactory?) : this(draweeControllerBuilder, null, callerContextFactory) {
    }

    constructor(
            draweeControllerBuilder: AbstractDraweeControllerBuilder<*, *, *, ImageInfo?>?,
            globalImageLoadListener: GlobalImageLoadListener?,
            callerContextFactory: ReactCallerContextFactory?) {
        mDraweeControllerBuilder = draweeControllerBuilder
        mGlobalImageLoadListener = globalImageLoadListener
        mCallerContextFactory = callerContextFactory
        mCallerContext = null
    }

    constructor() {
        // Lazily initialize as FrescoModule have not been initialized yet
        mDraweeControllerBuilder = null
        mCallerContext = null
        mCallerContextFactory = null
    }

    fun getDraweeControllerBuilder(): AbstractDraweeControllerBuilder<*, *, *, ImageInfo?>? {
        if (mDraweeControllerBuilder == null) {
            mDraweeControllerBuilder = Fresco.newDraweeControllerBuilder()
        }
        return mDraweeControllerBuilder
    }

    @Deprecated("use {@link ReactCallerContextFactory} instead ")
    fun getCallerContext(): Any? {
        return mCallerContext
    }

    override fun createViewInstance(context: ThemedReactContext): RNCardView {
        val callerContext = if (mCallerContextFactory != null) mCallerContextFactory.getOrCreateCallerContext(context, null) else getCallerContext()
        return RNCardView(context)
    }

    // In JS this is Image.props.source
    @ReactProp(name = "source")
    fun setSource(view: RNCardView, sources: ReadableArray?) {
        view.setSource(sources)
    }

    @ReactProp(name = "blurRadius")
    fun setBlurRadius(view: RNCardView, blurRadius: Float) {
        view.setBlurRadius(blurRadius)
    }

    @ReactProp(name = "foregroundColor", customType = "Color")
    fun setForegroundColor(view: RNCardView, foregroundColor: Int?) {
        view.setForegroundColor(foregroundColor)
    }

    @ReactProp(name = "internal_analyticTag")
    fun setInternal_AnalyticsTag(view: RNCardView, analyticTag: String?) {
        if (mCallerContextFactory != null) {
            view.updateCallerContext(
                    mCallerContextFactory.getOrCreateCallerContext(
                            view.context as ThemedReactContext, analyticTag))
        }
    }

    // In JS this is Image.props.defaultSource
    @ReactProp(name = "defaultSource")
    fun setDefaultSource(view: RNCardView, sources: String?) {
        //val uri = sources?.getString("uri")?:""
        view.setDefaultSource(sources)
    }
    // In JS this is Image.props.defaultSource
    @ReactProp(name = "backgroundImg")
    fun setBackground(view: RNCardView, sources: String?) {
        //val uri = sources?.getString("uri")?:""
        view.setBackgroundImg(sources)
    }

    // In JS this is Image.props.loadingIndicatorSource.uri
    @ReactProp(name = "loadingIndicatorSrc")
    fun setLoadingIndicatorSource(view: RNCardView, source: String?) {
        view.setLoadingIndicatorSource(source)
    }

    @ReactProp(name = "borderColor", customType = "Color")
    fun setBorderColor(view: RNCardView, borderColor: Int?) {
        if (borderColor == null) {
            view.setBorderColor(Color.TRANSPARENT)
        } else {
            view.setBorderColor(borderColor)
        }
    }

    @ReactProp(name = "overlayColor", customType = "Color")
    fun setOverlayColor(view: RNCardView, overlayColor: Int?) {
        if (overlayColor == null) {
            view.setOverlayColor(Color.TRANSPARENT)
        } else {
            view.setOverlayColor(overlayColor)
        }
    }

    @ReactProp(name = "borderWidth")
    fun setBorderWidth(view: RNCardView, borderWidth: Float) {
        view.setBorderWidth(borderWidth)
    }

    @ReactPropGroup(names = [ViewProps.BORDER_RADIUS, ViewProps.BORDER_TOP_LEFT_RADIUS, ViewProps.BORDER_TOP_RIGHT_RADIUS, ViewProps.BORDER_BOTTOM_RIGHT_RADIUS, ViewProps.BORDER_BOTTOM_LEFT_RADIUS], defaultFloat = 0f)
    fun setBorderRadius(view: RNCardView, index: Int, borderRadius: Float) {
        var borderRadius = borderRadius
        if (!YogaConstants.isUndefined(borderRadius)) {
            borderRadius = PixelUtil.toPixelFromDIP(borderRadius)
        }
        if (index == 0) {
            view.setBorderRadius(borderRadius)
        } else {
            view.setBorderRadius(borderRadius, index - 1)
        }
    }

    @ReactProp(name = ViewProps.RESIZE_MODE)
    fun setResizeMode(view: RNCardView, resizeMode: String?) {
        view.setScaleType(ImageResizeMode.toScaleType(resizeMode))
        view.setTileMode(ImageResizeMode.toTileMode(resizeMode))
    }

    @ReactProp(name = ViewProps.RESIZE_METHOD)
    fun setResizeMethod(view: RNCardView, resizeMethod: String?) {
        if (resizeMethod == null || "auto" == resizeMethod) {
            view.setResizeMethod(ImageResizeMethod.AUTO)
        } else if ("resize" == resizeMethod) {
            view.setResizeMethod(ImageResizeMethod.RESIZE)
        } else if ("scale" == resizeMethod) {
            view.setResizeMethod(ImageResizeMethod.SCALE)
        } else {
            throw JSApplicationIllegalArgumentException(
                    "Invalid resize method: '$resizeMethod'")
        }
    }

    @ReactProp(name = "tintColor", customType = "Color")
    fun setTintColor(view: RNCardView, tintColor: Int?) {
        if (tintColor == null) {
            view.clearColorFilter()
        } else {
            view.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        }
    }

    @ReactProp(name = "progressiveRenderingEnabled")
    fun setProgressiveRenderingEnabled(view: RNCardView, enabled: Boolean) {
        view.setProgressiveRenderingEnabled(enabled)
    }

    @ReactProp(name = "fadeDuration")
    fun setFadeDuration(view: RNCardView, durationMs: Int) {
        view.setFadeDuration(durationMs)
    }

//    @ReactProp(name = "shouldNotifyLoadEvents")
//    fun setLoadHandlersRegistered(view: RNCardView, shouldNotifyLoadEvents: Boolean) {
//        view.setShouldNotifyLoadEvents(shouldNotifyLoadEvents)
//    }

    @ReactProp(name = "headers")
    fun setHeaders(view: RNCardView, headers: ReadableMap?) {
        view.setHeaders(headers)
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Map<String, String>>? {
        return MapBuilder.of<String, Map<String, String>>(
                ImageLoadEvent.eventNameForType(ImageLoadEvent.ON_LOAD_START),
                MapBuilder.of("registrationName", "onLoadStart"),
                ImageLoadEvent.eventNameForType(ImageLoadEvent.ON_LOAD),
                MapBuilder.of("registrationName", "onLoad"),
                ImageLoadEvent.eventNameForType(ImageLoadEvent.ON_ERROR),
                MapBuilder.of("registrationName", "onError"),
                ImageLoadEvent.eventNameForType(ImageLoadEvent.ON_LOAD_END),
                MapBuilder.of("registrationName", "onLoadEnd"))
    }

    override fun onAfterUpdateTransaction(view: RNCardView) {
        super.onAfterUpdateTransaction(view)
        view.maybeUpdateView()
    }
}