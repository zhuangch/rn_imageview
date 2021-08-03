package com.micen.react.bridge.viewmanager.imageview

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.facebook.common.references.CloseableReference
import com.facebook.common.util.UriUtil
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.drawable.AutoRotateDrawable
import com.facebook.drawee.drawable.RoundedColorDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor
import com.facebook.imagepipeline.request.BasePostprocessor
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.build.ReactBuildConfig
import com.facebook.react.uimanager.FloatUtil
import com.facebook.react.uimanager.PixelUtil
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.views.image.ImageLoadEvent
import com.facebook.react.views.image.ImageResizeMethod
import com.facebook.react.views.image.ImageResizeMode
import com.facebook.react.views.imagehelper.ImageSource
import com.facebook.react.views.imagehelper.MultiSourceHelper
import com.facebook.react.views.imagehelper.ResourceDrawableIdHelper
import com.facebook.yoga.YogaConstants
import com.micen.widget.common.util.CommonUtil
import com.micen.widget.common.util.GlideRoundTransform
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.padding
import java.io.File
import java.util.*

/**********************************************************
 * @文件作者：xiongjiangwei
 * @创建时间：2020/12/22 16:29
 * @文件描述：自定义的ImageView，使用Glide异步加载图片
 * @修改历史：2020/12/22 创建初始版本
 **********************************************************/
class RNImageView(context: Context) : AppCompatImageView(context) {
    private var mResizeMethod = ImageResizeMethod.AUTO
    fun updateCallerContext(callerContext: Any?) {
        mIsDirty = true
    }

    private inner class RoundedCornerPostprocessor : BasePostprocessor() {
        fun getRadii(source: Bitmap, computedCornerRadii: FloatArray, mappedRadii: FloatArray) {
            mScaleType.getTransform(
                    sMatrix,
                    Rect(0, 0, source.width, source.height),
                    source.width,
                    source.height,
                    0.0f,
                    0.0f)
            sMatrix.invert(sInverse)
            mappedRadii[0] = sInverse.mapRadius(computedCornerRadii[0])
            mappedRadii[1] = mappedRadii[0]
            mappedRadii[2] = sInverse.mapRadius(computedCornerRadii[1])
            mappedRadii[3] = mappedRadii[2]
            mappedRadii[4] = sInverse.mapRadius(computedCornerRadii[2])
            mappedRadii[5] = mappedRadii[4]
            mappedRadii[6] = sInverse.mapRadius(computedCornerRadii[3])
            mappedRadii[7] = mappedRadii[6]
        }

        override fun process(output: Bitmap, source: Bitmap) {
            cornerRadii(sComputedCornerRadii)
            output.setHasAlpha(true)
            if (FloatUtil.floatsEqual(sComputedCornerRadii[0], 0f)
                    && FloatUtil.floatsEqual(sComputedCornerRadii[1], 0f)
                    && FloatUtil.floatsEqual(sComputedCornerRadii[2], 0f)
                    && FloatUtil.floatsEqual(sComputedCornerRadii[3], 0f)) {
                super.process(output, source)
                return
            }
            val paint = Paint()
            paint.isAntiAlias = true
            paint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val canvas = Canvas(output)
            val radii = FloatArray(8)
            getRadii(source, sComputedCornerRadii, radii)
            val pathForBorderRadius = Path()
            pathForBorderRadius.addRoundRect(
                    RectF(0f, 0f, source.width.toFloat(), source.height.toFloat()), radii, Path.Direction.CW)
            canvas.drawPath(pathForBorderRadius, paint)
        }
    }

    private inner class TilePostprocessor : BasePostprocessor() {
        override fun process(source: Bitmap, bitmapFactory: PlatformBitmapFactory): CloseableReference<Bitmap> {
            val destRect = Rect(0, 0, width, height)
            mScaleType.getTransform(
                    sTileMatrix, destRect, source.width, source.height, 0.0f, 0.0f)
            val paint = Paint()
            paint.isAntiAlias = true
            val shader: Shader = BitmapShader(source, mTileMode, mTileMode)
            shader.setLocalMatrix(sTileMatrix)
            paint.shader = shader
            val output = bitmapFactory.createBitmap(width, height)
            return try {
                val canvas = Canvas(output.get())
                canvas.drawRect(destRect, paint)
                output.clone()
            } finally {
                CloseableReference.closeSafely(output)
            }
        }
    }

    private val mSources: MutableList<ImageSource>
    private var mImageSource: ImageSource? = null
    private var mCachedImageSource: ImageSource? = null
    private var mDefaultImageDrawable: Drawable? = null
    private var mLoadingImageDrawable: Drawable? = null
    private var mBackgroundImageDrawable: RoundedColorDrawable? = null
    private var mBackgroundColor = 0x00000000
    private var mBorderColor = 0
    private var mOverlayColor = 0
    private var mBorderWidth = 0f
    private var mBorderRadius = YogaConstants.UNDEFINED
    private var mBorderCornerRadii: FloatArray? = null
    private var mScaleType: ScalingUtils.ScaleType
    private var mTileMode = ImageResizeMode.defaultTileMode()
    private var mIsDirty = false
    private val mRoundedCornerPostprocessor: RoundedCornerPostprocessor
    private val mTilePostprocessor: TilePostprocessor
    private var mIterativeBoxBlurPostProcessor: IterativeBoxBlurPostProcessor? = null
    private var mControllerListener: ControllerListener<ImageInfo?>? = null
    private var mControllerForTesting: ControllerListener<ImageInfo?>? = null
    private var mFadeDurationMs = -1
    private var mProgressiveRenderingEnabled = false
    private var mHeaders: ReadableMap? = null
    fun setShouldNotifyLoadEvents(shouldNotify: Boolean) {
        mControllerListener = if (!shouldNotify) {
            null
        } else {
            val mEventDispatcher = UIManagerHelper.getEventDispatcherForReactTag(context as ReactContext, id)
            object : BaseControllerListener<ImageInfo?>() {
                override fun onSubmit(id: String, callerContext: Any) {
                    mEventDispatcher!!.dispatchEvent(
                            ImageLoadEvent(getId(), ImageLoadEvent.ON_LOAD_START))
                }

                override fun onFinalImageSet(
                        id: String, imageInfo: ImageInfo?, animatable: Animatable?) {
                    if (imageInfo != null) {
                        mEventDispatcher!!.dispatchEvent(
                                ImageLoadEvent(
                                        getId(),
                                        ImageLoadEvent.ON_LOAD,
                                        mImageSource!!.source,
                                        imageInfo.width,
                                        imageInfo.height))
                        mEventDispatcher.dispatchEvent(
                                ImageLoadEvent(getId(), ImageLoadEvent.ON_LOAD_END))
                    }
                }

                override fun onFailure(id: String, throwable: Throwable) {
                    mEventDispatcher!!.dispatchEvent(
                            ImageLoadEvent(
                                    getId(), ImageLoadEvent.ON_ERROR, true, throwable.message))
                }
            }
        }
        mIsDirty = true
    }

    fun setBlurRadius(blurRadius: Float) {
        val pixelBlurRadius = PixelUtil.toPixelFromDIP(blurRadius).toInt()
        mIterativeBoxBlurPostProcessor = if (pixelBlurRadius == 0) {
            null
        } else {
            IterativeBoxBlurPostProcessor(pixelBlurRadius)
        }
        mIsDirty = true
    }

    override fun setBackgroundColor(backgroundColor: Int) {
        if (mBackgroundColor != backgroundColor) {
            mBackgroundColor = backgroundColor
            mBackgroundImageDrawable = RoundedColorDrawable(backgroundColor)
            mIsDirty = true
        }
    }

    fun setBorderColor(borderColor: Int) {
        mBorderColor = borderColor
        mIsDirty = true
    }

    fun setOverlayColor(overlayColor: Int) {
        mOverlayColor = overlayColor
        mIsDirty = true
    }

    fun setBorderWidth(borderWidth: Float) {
        mBorderWidth = PixelUtil.toPixelFromDIP(borderWidth)
        mIsDirty = true
    }

    fun setBorderRadius(borderRadius: Float) {
        if (!FloatUtil.floatsEqual(mBorderRadius, borderRadius)) {
            mBorderRadius = borderRadius
            mIsDirty = true
        }
    }

    fun setBorderRadius(borderRadius: Float, position: Int) {
        if (mBorderCornerRadii == null) {
            mBorderCornerRadii = FloatArray(4)
            Arrays.fill(mBorderCornerRadii, YogaConstants.UNDEFINED)
        }
        if (!FloatUtil.floatsEqual(mBorderCornerRadii!![position], borderRadius)) {
            mBorderCornerRadii!![position] = borderRadius
            mIsDirty = true
        }
    }

    fun setScaleType(scaleType: ScalingUtils.ScaleType) {
        val systemScaleType = when (scaleType) {
            ScalingUtils.ScaleType.FIT_XY -> ScaleType.FIT_XY
            ScalingUtils.ScaleType.FIT_START -> ScaleType.FIT_START
            ScalingUtils.ScaleType.FIT_CENTER -> ScaleType.FIT_CENTER
            ScalingUtils.ScaleType.FIT_END -> ScaleType.FIT_END
            ScalingUtils.ScaleType.CENTER_CROP -> ScaleType.CENTER_CROP
            ScalingUtils.ScaleType.CENTER_INSIDE -> ScaleType.CENTER_INSIDE
            ScalingUtils.ScaleType.CENTER -> ScaleType.CENTER
            else -> ScaleType.MATRIX
        }
        setScaleType(systemScaleType)
        mScaleType = scaleType
        mIsDirty = true
    }

    fun setTileMode(tileMode: Shader.TileMode) {
        mTileMode = tileMode
        mIsDirty = true
    }

    fun setResizeMethod(resizeMethod: ImageResizeMethod) {
        mResizeMethod = resizeMethod
        mIsDirty = true
    }

    fun setSource(sources: ReadableArray?) {
        mSources.clear()
        if (sources == null || sources.size() == 0) {
            val imageSource = ImageSource(context, REMOTE_TRANSPARENT_BITMAP_URI)
            mSources.add(imageSource)
        } else {
            // Optimize for the case where we have just one uri, case in which we don't need the sizes
            if (sources.size() == 1) {
                val source = sources.getMap(0)
                val uri = source!!.getString("uri")
                val imageSource = ImageSource(context, uri)
                mSources.add(imageSource)
                if (Uri.EMPTY == imageSource.uri) {
                    warnImageSource(uri)
                }
            } else {
                for (idx in 0 until sources.size()) {
                    val source = sources.getMap(idx)
                    val uri = source!!.getString("uri")
                    val imageSource = ImageSource(
                            context, uri, source.getDouble("width"), source.getDouble("height"))
                    mSources.add(imageSource)
                    if (Uri.EMPTY == imageSource.uri) {
                        warnImageSource(uri)
                    }
                }
            }
        }
        mIsDirty = true
    }

    fun setDefaultSource(name: String?) {
        mDefaultImageDrawable = ResourceDrawableIdHelper.getInstance().getResourceDrawable(context, name)
        mIsDirty = true
    }

    fun setLoadingIndicatorSource(name: String?) {
        val drawable = ResourceDrawableIdHelper.getInstance().getResourceDrawable(context, name)
        mLoadingImageDrawable = if (drawable != null) AutoRotateDrawable(drawable, 1000) else null
        mIsDirty = true
    }

    fun setProgressiveRenderingEnabled(enabled: Boolean) {
        mProgressiveRenderingEnabled = enabled
        // no worth marking as dirty if it already rendered..
    }

    fun setFadeDuration(durationMs: Int) {
        mFadeDurationMs = durationMs
        // no worth marking as dirty if it already rendered..
    }

    private fun cornerRadii(computedCorners: FloatArray) {
        val defaultBorderRadius: Float = if (!YogaConstants.isUndefined(mBorderRadius)) mBorderRadius else 0f
        computedCorners[0] = if (mBorderCornerRadii != null && !YogaConstants.isUndefined(mBorderCornerRadii!![0])) mBorderCornerRadii!![0] else defaultBorderRadius
        computedCorners[1] = if (mBorderCornerRadii != null && !YogaConstants.isUndefined(mBorderCornerRadii!![1])) mBorderCornerRadii!![1] else defaultBorderRadius
        computedCorners[2] = if (mBorderCornerRadii != null && !YogaConstants.isUndefined(mBorderCornerRadii!![2])) mBorderCornerRadii!![2] else defaultBorderRadius
        computedCorners[3] = if (mBorderCornerRadii != null && !YogaConstants.isUndefined(mBorderCornerRadii!![3])) mBorderCornerRadii!![3] else defaultBorderRadius
    }

    fun setHeaders(headers: ReadableMap?) {
        mHeaders = headers
    }

    fun maybeUpdateView() {
        if (!mIsDirty) {
            return
        }
        if (hasMultipleSources() && (width <= 0 || height <= 0)) {
            // If we need to choose from multiple uris but the size is not yet set, wait for layout pass
            return
        }
        setSourceImage()
        if (mImageSource == null) {
            return
        }
        val doResize = shouldResize(mImageSource!!)
        if (doResize && (width <= 0 || height <= 0)) {
            // If need a resize and the size is not yet set, wait until the layout pass provides one
            return
        }
        if (isTiled() && (width <= 0 || height <= 0)) {
            // If need to tile and the size is not yet set, wait until the layout pass provides one
            return
        }
//        val hierarchy = hierarchy
//        hierarchy.setActualImageScaleType(mScaleType)
        //下面两个占位图已通过Glide实现
//        if (mDefaultImageDrawable != null) {
//            hierarchy.setPlaceholderImage(mDefaultImageDrawable, mScaleType)
//        }
//        if (mLoadingImageDrawable != null) {
//            hierarchy.setPlaceholderImage(mLoadingImageDrawable, ScalingUtils.ScaleType.CENTER)
//        }
        val usePostprocessorScaling = (mScaleType !== ScalingUtils.ScaleType.CENTER_CROP
                && mScaleType !== ScalingUtils.ScaleType.FOCUS_CROP)
        cornerRadii(sComputedCornerRadii)
        if (mBackgroundImageDrawable != null) {
            mBackgroundImageDrawable!!.setBorder(mBorderColor, mBorderWidth)
            mBackgroundImageDrawable!!.radii = sComputedCornerRadii
            //此处直接用系统api设置背景资源，并且留下边框的内间距
            backgroundDrawable = mBackgroundImageDrawable
            padding = mBorderWidth.toInt()
//            hierarchy.setBackgroundImage(mBackgroundImageDrawable)
        }
//        hierarchy.roundingParams = roundingParams
//        hierarchy.fadeDuration = if (mFadeDurationMs >= 0) mFadeDurationMs else if (mImageSource!!.isResource) 0 else REMOTE_IMAGE_FADE_DURATION_MS
//        val postprocessors: MutableList<Postprocessor> = LinkedList()
//        if (usePostprocessorScaling) {
//            postprocessors.add(mRoundedCornerPostprocessor)
//        }
//        if (mIterativeBoxBlurPostProcessor != null) {
//            postprocessors.add(mIterativeBoxBlurPostProcessor!!)
//        }
//        if (isTiled()) {
//            postprocessors.add(mTilePostprocessor)
//        }
//        val postprocessor = MultiPostprocessor.from(postprocessors)
//        val resizeOptions = if (doResize) ResizeOptions(width, height) else null
//        val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(mImageSource!!.uri)
//                .setPostprocessor(postprocessor)
//                .setResizeOptions(resizeOptions)
//                .setAutoRotateEnabled(true)
//                .setProgressiveRenderingEnabled(mProgressiveRenderingEnabled)
//        val imageRequest: ImageRequest = ReactNetworkImageRequest.fromBuilderWithHeaders(imageRequestBuilder, mHeaders)
        requestImage(mImageSource, mDefaultImageDrawable ?: mLoadingImageDrawable)
//        mGlobalImageLoadListener?.onLoadAttempt(mImageSource!!.uri)

        // This builder is reused
//        mDraweeControllerBuilder?.reset()
//        mDraweeControllerBuilder
//                ?.setAutoPlayAnimations(true)
//                ?.setCallerContext(mCallerContext)
//                ?.setOldController(controller)
//                ?.setImageRequest(imageRequest)
        if (mCachedImageSource != null) {
//            val cachedImageRequest = ImageRequestBuilder.newBuilderWithSource(mCachedImageSource!!.uri)
//                    .setPostprocessor(postprocessor)
//                    .setResizeOptions(resizeOptions)
//                    .setAutoRotateEnabled(true)
//                    .setProgressiveRenderingEnabled(mProgressiveRenderingEnabled)
//                    .build()
//            mDraweeControllerBuilder?.setLowResImageRequest(cachedImageRequest)
            requestImage(mCachedImageSource, mDefaultImageDrawable ?: mLoadingImageDrawable)
        }
//        if (mControllerListener != null && mControllerForTesting != null) {
//            val combinedListener: ForwardingControllerListener<ImageInfo?> = ForwardingControllerListener()
//            combinedListener.addListener(mControllerListener)
//            combinedListener.addListener(mControllerForTesting)
//        } else if (mControllerForTesting != null) {
//        } else if (mControllerListener != null) {
//        }
        mIsDirty = false

        // Reset again so the DraweeControllerBuilder clears all it's references. Otherwise, this causes
        // a memory leak.
    }

    /**
     * 请求图片
     */
    private fun requestImage(imageSource: ImageSource?, placeHolderDrawable: Drawable?) {
        val realContext = CommonUtil.getActivityContext(context)
        if (activityAlive(realContext)) {
            imageSource?.let {
                val requestOptions = RequestOptions()
                if (placeHolderDrawable != null) {
                    requestOptions.placeholder(placeHolderDrawable)
                }
                if (mBorderRadius > 0) {
                    requestOptions.transform(GlideRoundTransform(mBorderRadius))
                }else if (mBorderCornerRadii != null) {
                    requestOptions.transform(GlideRoundTransform(sComputedCornerRadii[0], sComputedCornerRadii[1], sComputedCornerRadii[2], sComputedCornerRadii[3]))
                }
                //如果图片地址格式化为Uri后是空，则认为是本地sdcard路径
                if (it.uri == null || TextUtils.isEmpty(it.uri.toString())) {
                    val localPathUri = Uri.fromFile(File(it.source))
                    Glide.with(context)
                            .load(localPathUri)
                            .apply(requestOptions)
                            .into(this)
                } else {
                    //如果Uri的头是res，则认为是本地drawable资源，否则是网络图片
                    if (it.uri.scheme == "res") {
                        //不能转成drawable去加载，转成drawable后glide会直接取加载drawable资源，则资源文件为gif图时只能加载为静态图，转成资源id则glide会依据资源的类型，去解析加载（gif资源时会解析为GifDrawable去加载）
                        val localDrawableId = ResourceDrawableIdHelper.getInstance().getResourceDrawableId(context, it.source)
                        Glide.with(context)
                                .load(localDrawableId)
                                .apply(requestOptions)
                                .into(this)
                    } else {
                        Glide.with(context)
                                .load(it.uri)
                                .apply(requestOptions)
                                .into(this)
                    }
                }
            }
        }
    }

    private fun activityAlive(realContext: Activity?) =
            realContext != null && !realContext.isFinishing && if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) !realContext.isDestroyed else true

    // VisibleForTesting
    fun setControllerListener(controllerListener: ControllerListener<ImageInfo?>?) {
        mControllerForTesting = controllerListener
        mIsDirty = true
        maybeUpdateView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            mIsDirty = mIsDirty || hasMultipleSources() || isTiled()
            maybeUpdateView()
        }
    }

    /** ReactImageViews only render a single image.  */
    override fun hasOverlappingRendering(): Boolean {
        return false
    }

    private fun hasMultipleSources(): Boolean {
        return mSources.size > 1
    }

    private fun isTiled(): Boolean {
        return mTileMode != Shader.TileMode.CLAMP
    }

    private fun setSourceImage() {
        mImageSource = null
        if (mSources.isEmpty()) {
            val imageSource = ImageSource(context, REMOTE_TRANSPARENT_BITMAP_URI)
            mSources.add(imageSource)
        } else if (hasMultipleSources()) {
            val multiSource = MultiSourceHelper.getBestSourceForSize(width, height, mSources)
            mImageSource = multiSource.bestResult
            mCachedImageSource = multiSource.bestResultInCache
            return
        }
        mImageSource = mSources[0]
    }

    private fun shouldResize(imageSource: ImageSource): Boolean {
        // Resizing is inferior to scaling. See http://frescolib.org/docs/resizing-rotating.html#_
        // We resize here only for images likely to be from the device's camera, where the app developer
        // has no control over the original size
        return if (mResizeMethod == ImageResizeMethod.AUTO) {
            (UriUtil.isLocalContentUri(imageSource.uri)
                    || UriUtil.isLocalFileUri(imageSource.uri))
        } else if (mResizeMethod == ImageResizeMethod.RESIZE) {
            true
        } else {
            false
        }
    }

    private fun warnImageSource(uri: String?) {
        if (ReactBuildConfig.DEBUG) {
            Toast.makeText(
                    context,
                    "Warning: Image source \"$uri\" doesn't exist",
                    Toast.LENGTH_SHORT)
                    .show()
        }
    }

    companion object {
        const val REMOTE_IMAGE_FADE_DURATION_MS = 300
        const val REMOTE_TRANSPARENT_BITMAP_URI = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        private val sComputedCornerRadii = FloatArray(4)

        /*
   * Implementation note re rounded corners:
   *
   * Fresco's built-in rounded corners only work for 'cover' resize mode -
   * this is a limitation in Android itself. Fresco has a workaround for this, but
   * it requires knowing the background color.
   *
   * So for the other modes, we use a postprocessor.
   * Because the postprocessor uses a modified bitmap, that would just get cropped in
   * 'cover' mode, so we fall back to Fresco's normal implementation.
   */
        private val sMatrix = Matrix()
        private val sInverse = Matrix()

        // Fresco lacks support for repeating images, see https://github.com/facebook/fresco/issues/1575
        // We implement it here as a postprocessing step.
        private val sTileMatrix = Matrix()

        // We can't specify rounding in XML, so have to do so here
        private fun buildHierarchy(context: Context): GenericDraweeHierarchy {
            return GenericDraweeHierarchyBuilder(context.resources)
                    .setRoundingParams(RoundingParams.fromCornersRadius(0f))
                    .build()
        }
    }

    init {
        mScaleType = ImageResizeMode.defaultValue()
        mRoundedCornerPostprocessor = RoundedCornerPostprocessor()
        mTilePostprocessor = TilePostprocessor()
        mSources = LinkedList()
    }
}