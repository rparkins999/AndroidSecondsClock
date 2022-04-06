/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Much of the code is copied from ProgressBar.java, AbsSeekBar.java, and SeekBar.java,
 * which are Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Slider is a longclickable SeekBar which can be vertical or horizontal.
 *
 * Because it can be drawn in any of the four cardinal directions, but the style
 * doesn't know about this, we have to distinguish between "as styled" dimensions,
 * which get rotated, and "as drawn" dimensions, which don't.
 * This can make the code a bit contorted and confusing - take care!
 * The thumb and the track drawables don't get rotated, which is OK for most
 * default styles, but if the app sets a custom thumb or track which is changed
 * by 90 degree rotations, it needs to use the correct orientation.
 *
 * The overall size of the Slider is the size of the background, if there is one:
 * the track is drawn inside that area reduced by the padding, but the thumb
 * extends into the padding.
 */
public class Slider extends View {

    // direction of increasing value
    public static final int SLIDER_RIGHTWARDS = 0;
    public static final int SLIDER_UPWARDS = 1;
    public static final int SLIDER_LEFTWARDS = 2;
    public static final int SLIDER_DOWNWARDS = 3;

    private int mDirection = SLIDER_RIGHTWARDS; // default

    /* These are the padding values in pixels requested by the theme
     * or overridden by the app. Note these are raw values, as from the style,
     * for a SEEKBAR_RIGHTWARDS. For the other directions,
     * we need to switch them round before passing them to the parent View.
     * We need to remember these because we adjust the actual padding
     * for direction and to allow for the thumb extending outside the track,
     * and if the thumb or direction changes we need to do that again using
     * the original requested padding values.
     * The getPadding??? methods will return the padding from the parent View.
     * This is in principle a bug but I don't use those methods.
     */
    private int mPadLeft; // default 0 set in constructor
    private int mPadTop; // default 0 set in constructor
    private int mPadRight; // default 0 set in constructor
    private int mPadBottom; // default 0 set in constructor
    private boolean mPaddingIsRelative = false; // relative to layout direction
    private boolean mMeasured = false; // avoid calling updateThumbAndTrackPos() too early

    /* These are the minimum and maximum width and height values in pixels
     * as requested by the theme or overridden by the app.
     * Note these are raw values, as from the style, for a SLIDER_RIGHTWARDS,
     * and they don't include padding. For the other directions,
     * we need to switch them round before using them in onMeasure().
     * We need to remember these in case the app changes the direction.
     */
    private int mMinWidth; // default 24 set in constructor
    private int mMaxWidth; // default 48 set in constructor
    private int mMinHeight; // default 24 set in constructor
    private int mMaxHeight; // default 48 set in constructor

    // These come from the theme, but may be overridden by the app.
    private Drawable mThumb = null;
    private ColorStateList mThumbTintList = null;
    private Drawable mTrack = null;
    private ColorStateList mTrackTintList = null;

    // These are the dimensions of the thumb and track as drawn
    private int mThumbHalfWidth;
    private int mThumbHalfHeight;
    private int mTrackHalfWidth;
    private int mTrackHalfHeight;

    // These are the canvas translation offsets as drawn
    private int mOffsetX;
    private int mOffsetY;

    private int mValue; // current value
    private int mMin; // minimum for mValue, default from theme or 0
    private int mMax; // maximum for mValue, default from theme or 100

    public interface OnValueChangeListener {
        void onValueChanged(Slider slider, int value);
    }

    /* Callback to be called when the value changes.
     * The callback will only be called if the thumb is moved by a touch action
     * or a keypress: it will not be called as a result of a call to setValue().
     */
    private OnValueChangeListener mValueChangeListener = null;

    /** Interpolator used for smooth progress animations. */
    private static final AccelerateDecelerateInterpolator VALUE_ANIM_INTERPOLATOR =
        new AccelerateDecelerateInterpolator();

    /* Sets the thumb drawable bounds relative to the canvas,
     * which is positioned inside the padding.
     */
    private void setThumbPos(int value) {
        float scale = (value - mMin) / (float)(mMax - mMin);
        Drawable thumb = mThumb;
        if (thumb == null) { return; } // no thumb to position
        int layoutDirection = getLayoutDirection();
        final int thumbPosX; // centre of thumb as drawn
        final int thumbPosY; // centre of thumb as drawn
        if (mDirection == SLIDER_UPWARDS) {
            thumbPosX = mTrackHalfWidth;
            thumbPosY = (int)((1F - scale) * 2 * mTrackHalfHeight + 0.5f);
            thumb.setBounds(thumbPosX - mThumbHalfWidth,
                thumbPosY - mThumbHalfHeight,
                thumbPosX + mThumbHalfWidth,
                thumbPosY + mThumbHalfHeight);
        } else if (mDirection == SLIDER_DOWNWARDS) {
            thumbPosX = mTrackHalfWidth;
            thumbPosY = (int)(scale * 2 * mTrackHalfWidth + 0.5f);
            thumb.setBounds(thumbPosX - mThumbHalfWidth,
                thumbPosY - mThumbHalfHeight,
                thumbPosX + mThumbHalfWidth,
                thumbPosY + mThumbHalfHeight);
        } else if (  (mDirection == SLIDER_LEFTWARDS)
            ^ (layoutDirection == LAYOUT_DIRECTION_RTL)) // (XOR)
        { // leftwards after applying layout direction
            thumbPosX = (int)((1F - scale) * 2 * mTrackHalfWidth + 0.5f);
            thumbPosY = mTrackHalfHeight;
            thumb.setBounds(thumbPosX - mThumbHalfWidth,
                thumbPosY - mThumbHalfHeight,
                thumbPosX + mThumbHalfWidth,
                thumbPosY +  mThumbHalfHeight);
        } else { // rightwards after applying layout direction
            thumbPosX = (int)(scale * 2 * mTrackHalfWidth + 0.5f);
            thumbPosY = mTrackHalfHeight;
            thumb.setBounds(thumbPosX - mThumbHalfWidth,
                thumbPosY - mThumbHalfHeight,
                thumbPosX + mThumbHalfWidth,
                thumbPosY + mThumbHalfHeight);
        }
        invalidate();
    }

    /** Duration of smooth progress animations. */
    private static final int VALUE_ANIM_DURATION = 80;
    private ObjectAnimator mLastValueAnimator; // current value animator
    private final IntProperty<Slider> VISUAL_VALUE =
        new IntProperty<Slider>("thumbPos") {
            @Override
            public void setValue(Slider object, int value) {
                object.setThumbPos(value);
                if (mValueChangeListener != null) {
                    mValueChangeListener.onValueChanged(object, value);
                }
            }
            @Override
            public Integer get(Slider object) { return mValue; }
        };

    // Set up the callback: a null argument removes any existing callback.
    public void setOnChangeListener (@Nullable OnValueChangeListener l) {
        mValueChangeListener = l;
    }

    // Amount of motion in pixels that is a real move and not finger shake.
    private final int mScaledTouchSlop; // set in constructor
    // Initial position of an ACTION_DOWN, used to check for finger shake
    private float mStartX;
    private float mStartY;
    // Initial progress at ACTION_DOWN, used to restore after ACTION_CANCEL.
    private int mStartValue;
    /* True if we have decided it was a long click,
     * ignore MotionEvents until ACTION_CANCEL or ACTION_DOWN
     */
    private boolean mLongClicked = false;
    // True if timer is running and we haven't decided yet whether it is a long click.
    private boolean mTimerRunning = false;
    // True if we have decided it's a drag action along the track.
    private boolean mIsDragging = false;

    // Run when the long click timer times out.
    private final Runnable longClickTimer = new Runnable() {
        @Override
        public void run() {
            requestDisallowInterceptTouchEvent();
            mLongClicked = true;
            mTimerRunning = false;
            performLongClick();
        }
    };

    /* View's requestLayout() only draws the area inside the padding.
     * Since our thumb gets drawn overlapping the padding, we need to force
     * a redraw of the whole view, so we override requestLayout() to do so.
     */
    @Override
    public void requestLayout() {
        super.requestLayout();
        invalidate();
    }

    // Constructors
    public Slider(Context context) { this(context, null); }
    public Slider(Context context, AttributeSet attrs) {
        this(context, attrs, context.getResources().getIdentifier(
            "android:attr/seekBarStyle", "", "android"));
    }
    public Slider(Context context, AttributeSet attrs, int seekStyleAttr) {
        this(context, attrs, seekStyleAttr, 0);
    }
    public Slider(Context context, AttributeSet attrs,
                  int seekStyleAttr, int defStyleRes)
    {
        super(context, attrs, seekStyleAttr, defStyleRes);
        int progressStyleAttr = context.getResources().getIdentifier(
            "android:attr/progressBarStyle", "", "android");

        /* We can't get all the attributes into a single TypedArray as
         * the built-in SeekBar does, because they come in an arbitrary order
         * and the indexes are in R.styleable, which isn't accessible to us.
         */
        Resources res = context.getResources();
        /* Get the default thumb from the theme.
         * It can be null, in which case no thumb is displayed.
         * It can be overridden by setThumb() and read by getThumb().
         */
        int resId = res.getIdentifier(
            "android:attr/thumb", "", "android");
        TypedArray a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        final Drawable thumb = a.getDrawable(0);
        a.recycle();
        /* Get the default thumb ColorStateList from the theme.
         * It can be null, in which case the default foreground colour is used.
         * It can be overridden by setThumbTintList() and read by getThumbTintList().
         */
        resId = res.getIdentifier(
            "android:attr/thumbTint", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mThumbTintList = a.getColorStateList(0);
        a.recycle();
        /* Get the default track from the theme.
         * It can be null, in which case no track is displayed.
         * It can be overridden by setTrack() and read by getTrack().
         */
        resId = res.getIdentifier(
            "android:attr/progressDrawable", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        Drawable track = a.getDrawable(0);
        a.recycle();
        if ((track != null) && (track.getIntrinsicWidth() <= 0)) {
            /* The default track is a shape which expands horizontally,
             * but not vertically. This won't work for directions
             * SEEKBAR_UPWARDS and SEEKBAR_DOWNWARDS, so we replace it
             * with a plain square expandable along both axes.
             */
            int intrinsic = Math.max(track.getIntrinsicHeight(), 8);
            ShapeDrawable sd = new ShapeDrawable();
            sd.setIntrinsicWidth(intrinsic);
            sd.setIntrinsicHeight(intrinsic);
            track = sd;
        }
        /* Get the default track ColorStateList from the theme.
         * It can be null, in which case the default foreground colour is used.
         * It can be overridden by setTrackTintList() and read by getTrackTintList().
         */
        resId = res.getIdentifier(
            "android:attr/progressTint", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mTrackTintList = a.getColorStateList(0);
        a.recycle();
        /* Get the default left padding from the theme.
         * The default is 0 if no value is specified in the theme.
         * It may be overridden by calling setPadding() or setPaddingRelative().
         * For a horizontal seekbar, the actual padding may be larger than this
         * value to accommodate the thumb when it is at the one end of the track.
         */
        resId = res.getIdentifier(
            "android:attr/paddingLeft", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mPadLeft = a.getDimensionPixelSize(0, 0);
        a.recycle();
        /* Get the default top padding from the theme.
         * The default is 0 if no value is specified in the theme.
         * It may be overridden by calling setPadding() or setPaddingRelative().
         * For a vertical slider, the actual padding may be larger than this
         * value to accommodate the thumb when it is at one end of the track.
         */
        resId = res.getIdentifier(
            "android:attr/paddingTop", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mPadTop = a.getDimensionPixelSize(0, 0);
        a.recycle();
        /* Get the default right padding from the theme.
         * The default is 0 if no value is specified in the theme.
         * It may be overridden by calling setPadding() or setPaddingRelative().
         * For a horizontal slider, the actual padding may be larger than this
         * value to accommodate the thumb when it is at the one end of the track.
         */
        resId = res.getIdentifier(
            "android:attr/paddingRight", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mPadRight = a.getDimensionPixelSize(0, 0);
        a.recycle();
        /* Get the default bottom padding from the theme.
         * The default is 0 if no value is specified in the theme.
         * It may be overridden by calling setPadding() or setPaddingRelative().
         * For a vertical slider, the actual padding may be larger than this
         * value to accommodate the thumb when it is at the one end of the track.
         */
        resId = res.getIdentifier(
            "android:attr/paddingBottom", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mPadBottom = a.getDimensionPixelSize(0, 0);
        a.recycle();
        /* Get the default minimum width from the theme.
         * The default is 24 if no value is specified in the theme.
         * It may be overridden by calling setMinWidth().
         */
        resId = res.getIdentifier(
            "android:attr/minWidth", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mMinWidth = a.getDimensionPixelSize(0, 24);
        a.recycle();
        /* Get the default maximum width from the theme.
         * The default is 48 if no value is specified in the theme.
         * It may be overridden by calling setMaxWidth().
         */
        resId = res.getIdentifier(
            "android:attr/maxWidth", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mMaxWidth = a.getDimensionPixelSize(0, 48);
        if (mMaxWidth < mMinWidth) { mMaxWidth = mMinWidth; }
        a.recycle();
        /* Get the default minimum height from the theme.
         * The default is 24 if no value is specified in the theme.
         * It may be overridden by calling setMinHeight().
         */
        resId = res.getIdentifier(
            "android:attr/minHeight", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mMinHeight = a.getDimensionPixelSize(0, 24);
        a.recycle();
        /* Get the default maximum height from the theme.
         * The default is 48 if no value is specified in the theme.
         * It may be overridden by calling setMaxHeight().
         */
        resId = res.getIdentifier(
            "android:attr/maxHeight", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, seekStyleAttr, 0);
        mMaxHeight = a.getDimensionPixelSize(0, 48);
        if (mMaxHeight < mMinHeight) { mMaxHeight = mMinHeight; }
        a.recycle();
        /* Get the default minimum value from the theme.
         * The default is 0 if no value is specified in the theme.
         * It may be overridden by calling setMin().
         */
        resId = res.getIdentifier(
            "android:attr/min", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, progressStyleAttr, 0);
        mMin = a.getInt(0, 0);
        a.recycle();
        /* Get the default maximum value from the theme.
         * The default is 100 if no value is specified in the theme.
         * It may be overridden by calling setMax().
         */
        resId = res.getIdentifier(
            "android:attr/max", "", "android");
        a = context.obtainStyledAttributes(
            null, new int[] {resId}, progressStyleAttr, 0);
        mMax = a.getInt(0, 100);
        if (mMax < mMin) { mMax = mMin; }
        a.recycle();
        if (thumb != null) {
            mThumb = thumb.mutate();
            if (mThumbTintList == null) {
                mThumb.setTintList(ColorStateList.valueOf(0xFFFFFFFF));
            } else { mThumb.setTintList(mThumbTintList); }
        }
        if (track != null) {
            mTrack = track.mutate();
            if (mTrackTintList == null) {
                mTrack.setTintList(ColorStateList.valueOf(0xFFFFFFFF));
            } else { mTrack.setTintList(mTrackTintList); }
        }

        /* If the touch position has moved along the track by less than
         * this amount in pixels, the movement is considered to be finger shake
         * and not a deliberate move. Movement perpendicular to the track is
         * always considered to be finger shake at this level, but may be
         * considered to be a scrolling action by an ancestor view.
         * We have to store it here because when we need it we don't have
         * the context any more.
         */
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLongClickable(true);
    }

    // Get and set the minimum width as from the style for a SLIDER_RIGHTWARDS
    public int getMinWidth() { return mMinWidth; }
    public synchronized void setMinWidth(int w) {
        if (w > mMaxWidth) { mMinWidth = mMaxWidth; }
        else { mMinWidth = w; }
        requestLayout();
    }

    // Get and set the maximum width as from the style for a SLIDER_RIGHTWARDS
    public int getMaxWidth() { return mMaxWidth; }
    public synchronized void setMaxWidth(int w) {
        if (w < mMinWidth) { mMaxWidth = mMinWidth; }
        else { mMaxWidth = w; }
        requestLayout();
    }

    // Get and set the minimum height as from the style for a SLIDER_RIGHTWARDS
    public int getMinHeight() { return mMinHeight; }
    public synchronized void setMinHeight(int h) {
        if (h > mMaxHeight) { mMinHeight = mMaxHeight; }
        else { mMinHeight = h; }
        requestLayout();
    }

    // Get and set the maximum height as from the style for a SLIDER_RIGHTWARDS
    public int getMaxHeight() { return mMaxHeight; }
    public synchronized void setMaxHeight(int h) {
        if (h < mMinHeight) { mMaxHeight = mMinHeight; }
        else { mMaxHeight = h; }
        requestLayout();
    }

    /* This sets the real width and height, as actually drawn including padding,
     * or at least it tries to. The layout logic may not give us exactly what we ask for.
     * If there isn't enough room, we may get less, and if a layout parameter
     * is MATCH_PARENT, we may get more.
     */
    public void setWidthAndHeight(int w, int h) {
        // Force at least enough room for thumb and track
        w = Math.max(w - 2 * mThumbHalfWidth - 2 * mTrackHalfWidth, 0);
        h = Math.max(h - 2 * mThumbHalfHeight - 2 * mTrackHalfHeight, 0);
        switch (mDirection) {
            case SLIDER_UPWARDS:
            case SLIDER_DOWNWARDS:
                int temp = w;
                w = h;
                h = temp;
                //FALLTHRU
            case SLIDER_RIGHTWARDS:
            case SLIDER_LEFTWARDS:
                mMinWidth = w;
                mMaxWidth = w;
                mMinHeight = h;
                mMaxHeight = h;
                break;
        }
        requestLayout();
    }

    // Get and set the minimum value
    public int getMin() { return mMin; }
    public synchronized void setMin(int min) {
        if (min > mMax) { mMin = mMax; }
        else { mMin = min; }
        int range = mMax - mMin;
    }

    // Get and set the maximum value
    public int getMax() { return mMax; }
    public synchronized void setMax(int max) {
        if (max < mMin) { mMax = mMin; }
        else { mMax = max; }
        int range = mMax - mMin;
    }

    /* Set up the dimensions of the track and the thumb and adjust the padding if
     * necessary to fit in width w (pixels) and height h (pixels) as drawn.
     * w and h include padding if any.  This is called via onSizeChanged()
     * when the view's size has changed and internally if the app changes
     * any of the thumb, track, direction, or padding.
     * It shrinks the thumb and the track if the dimensions are too small
     * and expands the track length (but not width) to fill the space available.
     */
    private void updateThumbAndTrackPos(int w, int h, boolean paddingChanged) {
        if (!mMeasured) { return; }
        int delta;
        Drawable thumb = mThumb; // avoid possible data race
        if (thumb != null) {
            mThumbHalfWidth = thumb.getIntrinsicWidth() / 2;
            mThumbHalfHeight = thumb.getIntrinsicHeight() / 2;
        } else {
            mThumbHalfWidth = 0;
            mThumbHalfHeight = 0;
        }
        Drawable track = mTrack; // avoid possible data race
        if (track != null) {
            mTrackHalfWidth = track.getIntrinsicWidth() / 2;
            mTrackHalfHeight = track.getIntrinsicHeight() / 2;
        } else { // shouldn't happen, but just to be safe
            mTrackHalfWidth = 0;
            mTrackHalfHeight = 0;
        }
        int left = mPadLeft;
        int top = mPadTop;
        int right = mPadRight;
        int bottom = mPadBottom;
        /* Everything inside this switch is left/right and up/down symmetric,
         * so we don't have to worry about layout direction.
         */
        switch (mDirection) {
            case SLIDER_RIGHTWARDS: default: // just to stop lint barfing
            case SLIDER_LEFTWARDS:
                // Ensure room for the thumb to project over the end of the track
                if (left < mThumbHalfWidth) {
                    left = mThumbHalfWidth;
                    paddingChanged = true;
                }
                if (right < mThumbHalfWidth) {
                    right = mThumbHalfWidth;
                    paddingChanged = true;
                }
                if (w > 0) {
                    delta = left + right + 2 * mTrackHalfWidth;
                    if (delta > w) { // not enough room for thumb and track
                        float ratio = ((float) w) / delta;
                        left = (int) (left * ratio); // shrink padding
                        right = (int) (right * ratio); // shrink padding
                        if (mThumbHalfWidth > left) {
                            mThumbHalfWidth = left; // shrink thumb
                        }
                        if (mThumbHalfWidth > right) {
                            mThumbHalfWidth = right; // shrink thumb
                        }
                        paddingChanged = true;
                    }
                    mTrackHalfWidth = (w - left - right) / 2; // shrink or expand track
                }
                delta = mThumbHalfHeight - mTrackHalfHeight;
                if (delta > 0) { // thumb is thicker than track
                    if (top < delta) { // ensure enough room for thumb
                        top = delta;
                        paddingChanged = true;
                    }
                    if (bottom < delta) { // ensure enough room for thumb
                        bottom = delta;
                        paddingChanged = true;
                    }
                }
                if (h > 0) {
                    delta = top + bottom + Math.max(2 * mTrackHalfHeight, mMinHeight);
                    if (delta > h) { // not enough room for thumb and track
                        float ratio = ((float) h) / (top + bottom + 2 * mTrackHalfHeight);
                        top = (int) (top * ratio); // shrink padding
                        bottom = (int) (bottom * ratio); // shrink padding
                        paddingChanged = true;
                        mTrackHalfHeight = (h - top - bottom) / 2; // shrink track
                        if (mThumbHalfHeight > top + mTrackHalfHeight) {
                            mThumbHalfHeight = top + mTrackHalfHeight; // shrink thumb
                        }
                        if (mThumbHalfHeight > bottom + mTrackHalfHeight) {
                            mThumbHalfHeight = bottom + mTrackHalfHeight; // shrink thumb
                        }
                    } else if (delta < h) {
                        int extra = (h - delta) / 2;
                        top += extra;
                        bottom += extra;
                        paddingChanged = true;
                    }
                    if ((w > 0) && (track != null)) {
                        track.setBounds(0, 0,
                            2 * mTrackHalfWidth, 2 * mTrackHalfHeight);
                    }
                }
                break;
            case SLIDER_UPWARDS:
            case SLIDER_DOWNWARDS:
                // mPad... are the style values, which will get rotated.
                // Ensure room for the thumb to project over the end of the track
                if (left < mThumbHalfHeight) {
                    left = mThumbHalfHeight;
                    paddingChanged = true;
                }
                if (right < mThumbHalfHeight) {
                    right = mThumbHalfHeight;
                    paddingChanged = true;
                }
                if (h > 0) {
                    delta = left + right + 2 * mTrackHalfHeight;
                    if (delta > h) { // not enough room for thumb and track
                        float ratio = ((float) h) / delta;
                        left = (int) (left * ratio); // shrink padding
                        right = (int) (right * ratio); // shrink padding
                        if (mThumbHalfHeight > left) {
                            mThumbHalfHeight = left; // shrink thumb
                        }
                        if (mThumbHalfHeight > right) {
                            mThumbHalfHeight = right; // shrink thumb
                        }
                        paddingChanged = true;
                    }
                    mTrackHalfHeight = (h - left - right) / 2; // shrink or expand track
                }
                delta = mThumbHalfWidth - mTrackHalfWidth;
                if (delta > 0) { // thumb is thicker than track
                    if (top < delta) { // ensure enough room for thumb
                        top = delta;
                        paddingChanged = true;
                    }
                    if (bottom < delta) { // ensure enough room for thumb
                        bottom = delta;
                        paddingChanged = true;
                    }
                }
                if (w > 0) {
                    // mMinHeight is the minimum width for a rotated slider
                    delta = top + bottom + Math.max(2 * mTrackHalfWidth, mMinHeight);
                    if (delta > w) { // not enough room for thumb and track
                        float ratio = ((float) w) / (top + bottom + 2 * mTrackHalfWidth);
                        top = (int) (top * ratio); // shrink padding
                        bottom = (int) (bottom * ratio); // shrink padding
                        paddingChanged = true;
                        mTrackHalfWidth = (w - top - bottom) / 2; // shrink track
                        if (mThumbHalfWidth > top + mTrackHalfWidth) {
                            mThumbHalfWidth = top + mTrackHalfWidth; // shrink thumb
                        }
                        if (mThumbHalfWidth > bottom + mTrackHalfWidth) {
                            mThumbHalfWidth = bottom + mTrackHalfWidth; // shrink thumb
                        }
                    } else if (delta < w) {
                        int extra = (w - delta) / 2;
                        top += extra;
                        bottom += extra;
                        paddingChanged = true;
                    }
                    if ((h > 0) && (track != null)) {
                        track.setBounds(0, 0,
                            2 * mTrackHalfWidth, 2 * mTrackHalfHeight);
                    }
                }
                break;
        }
        if (paddingChanged) {
            int layoutDirection =
                mPaddingIsRelative ? getLayoutDirection() : LAYOUT_DIRECTION_LTR;
            switch (mDirection) {
                case SLIDER_RIGHTWARDS:
                    switch (layoutDirection) {
                        case LAYOUT_DIRECTION_RTL:
                            super.setPadding(right, top, left, bottom);
                            mOffsetX = right;
                            break;
                        case LAYOUT_DIRECTION_LTR:
                        default:
                            super.setPadding(left, top, right, bottom);
                            mOffsetX = left;
                            break;
                    }
                    mOffsetY = h / 2 - mTrackHalfHeight;
                    break;
                case SLIDER_LEFTWARDS:
                    switch (layoutDirection) {
                        case LAYOUT_DIRECTION_RTL:
                            super.setPadding(left, top, right, bottom);
                            mOffsetX = left;
                            break;
                        case LAYOUT_DIRECTION_LTR:
                        default:
                            super.setPadding(right, top, left, bottom);
                            mOffsetX = right;
                            break;
                    }
                    mOffsetY = h / 2 - mTrackHalfHeight;
                    break;
                case SLIDER_UPWARDS:
                    switch (layoutDirection) {
                        case LAYOUT_DIRECTION_RTL:
                            super.setPadding(top, left, bottom, right);
                            break;
                        case LAYOUT_DIRECTION_LTR:
                        default:
                            super.setPadding(top, right, bottom, left);
                            break;
                    }
                    mOffsetX = w / 2 - mTrackHalfHeight;
                    mOffsetY = top;
                    break;
                case SLIDER_DOWNWARDS:
                    switch (layoutDirection) {
                        case LAYOUT_DIRECTION_RTL:
                            super.setPadding(top, right, bottom, left);
                            break;
                        case LAYOUT_DIRECTION_LTR:
                        default:
                            super.setPadding(top, left, bottom, right);
                            break;
                    }
                    mOffsetX = w / 2 - mTrackHalfHeight;
                    mOffsetY = top;
                    break;
            }
        }
        setThumbPos(mValue);
    }

    /* Set padding allowing for extra space either side of the track if thumb is
     * wider and extra space for thumb when it is at one end of the track.
     */
    private void localSetPadding(int left, int top, int right, int bottom) {
        // Remember what the app asked for.
        mPadLeft = left;
        mPadTop = top;
        mPadRight = right;
        mPadBottom = bottom;
        /* Ensure that the actual padding is big enough for the thumb
         * and rotate if not SLIDER_RIGHTWARDS.
         */
        updateThumbAndTrackPos(getWidth(), getHeight(), true);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingIsRelative = false;
        localSetPadding(left, top, right, bottom);
    }

    @Override
    public void setPaddingRelative(int left, int top, int right, int bottom) {
        mPaddingIsRelative = true;
        localSetPadding(left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateThumbAndTrackPos(w, h, false);
    }

    /* Called when creating the widget to specify the direction. The direction
     * can be changed dynamically, but this is not normally done.
     */
    public void setDirection(int direction) {
        /* Padding hasn't actually changed, but if the direction changes,
         * the map from our padding to the view's padding will change.
         */
        boolean paddingChanged = mDirection != direction;
        mDirection = direction;
        updateThumbAndTrackPos(getWidth(), getHeight(), paddingChanged);
    }

    /* Note that usually thumb != mThumb.
     * This is so that we can copy the thumb state when this is called from setThumb().
     */
    private void applyThumbTint(@NonNull Drawable thumb) {
        if (mThumbTintList != null) {
            thumb = thumb.mutate();
            thumb.setTintList(mThumbTintList);
            if (thumb.isStateful() ) {
                int[] state = null;
                if (thumb != mThumb) { state = mThumb.getState(); }
                else if (mTrack.isStateful()) {
                    // Note that if the states are different this won't work.
                    // For now, let's consider that an app bug.
                    state = mTrack.getState();
                }
                if (state != null) { thumb.setState(state); }
            }
        }
    }

    // Will cause the thumb to be redrawn if necessary.
    public void setThumbTintList(@Nullable ColorStateList tint) {
        mThumbTintList = tint;
        if (mThumb != null) { applyThumbTint(mThumb); }
    }

    /**
     * Returns the tint applied to the thumb drawable, if specified.
     *
     * @return the tint applied to the thumb drawable
     * @attr ref android.R.styleable#SeekBar_thumbTint
     */
    @Nullable
    public ColorStateList getThumbTintList() {
        return mThumbTintList;
    }

    /**
     * Set the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     * <p>
     *
     * @param thumb Drawable representing the thumb
     *
     * This internal version is used to avoid calling updateThumbAndTrackPos()
     * and requestLayout() multiple times during construction.
     */
    private synchronized boolean setThumbInternal(Drawable thumb) {
        final boolean needUpdate;
        if ((mThumb != null) && (thumb != mThumb)) {
            mThumb.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = thumb != null;
        }
        if ((thumb != null) && (thumb != mThumb)) {
            thumb.setCallback(this);
            applyThumbTint(thumb);
        }
        mThumb = thumb;
        return needUpdate;
    }

    /**
     * Set the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     * <p>
     *
     * @param thumb Drawable representing the thumb
     */
    public void setThumb(Drawable thumb) {
       if (setThumbInternal(thumb)) {
           updateThumbAndTrackPos(getWidth(), getHeight(), false);
           requestLayout();
       }
    }

    /**
     * Return the drawable used to represent the scroll thumb - the component that
     * the user can drag back and forth indicating the current value by its position.
     *
     * @return The current thumb drawable
     */
    public Drawable getThumb() {
        return mThumb;
    }

    /* Note that usually track != mTrack.
     * This is so that we can copy the track state when this is called from setTrack().
     */
    private void applyTrackTint(@NonNull Drawable track) {
        if (mTrackTintList != null) {
            track = track.mutate();
            track.setTintList(mTrackTintList);
            if (track.isStateful()) {
                int[] state = null;
                if (track != mTrack) { state = mTrack.getState();}
                else if (mThumb.isStateful()) {
                    // Note that if the states are different this won't work.
                    // For now, let's consider that an app bug.
                     state = mThumb.getState();
                }
                if (state != null) { track.setState(state); }
            }
        }
    }

    public void setTrackTintList(@Nullable ColorStateList tint) {
        mTrackTintList = tint;
        if (mTrack != null) { applyTrackTint(mTrack); }
    }

    /**
     * Returns the tint applied to the thumb drawable, if specified.
     *
     * @return the tint applied to the track drawable
     * @attr ref android.R.styleable#progressTint
     */
    @Nullable
    public ColorStateList getTrackTintList() {
        return mTrackTintList;
    }

    /**
     * Sets the track that will be drawn for the SeekBar.
     * <p>
     *
     * @param track Drawable representing the track
     *
     * This internal version is used to avoid calling updateThumbAndTrackPos()
     * and requestLayout() twice during construction.
     */
    private synchronized boolean setTrackInternal(Drawable track) {
        final boolean needUpdate;
        if ((mTrack != null) && (track != mTrack)) {
            needUpdate = true;
        } else {
            needUpdate = track != null;
        }
        if ((track != null) && (track != mTrack)) {
            applyTrackTint(track);
        }
        mTrack = track;
        return needUpdate;
    }

    /**
     * Set the drawable used to represent the track - along which the user can
     * drag the thumb back and forth indicating the current value by its position.
     * <p>
     *
     * @param track Drawable representing the track
     */
    public void setTrack(Drawable track) {
        if (setTrackInternal(track)) {
            updateThumbAndTrackPos(getWidth(), getHeight(), false);
            requestLayout();
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return (who == mThumb) || (who == mTrack) || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mThumb != null) {
            mThumb.jumpToCurrentState();
        }
        if (mTrack != null) {
            mTrack.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final Drawable track = mTrack;
        if (   (track != null) && (track.isStateful())
            && track.setState(getDrawableState()))
        {
            invalidateDrawable(track);
        }

        final Drawable thumb = mThumb;
        if (   (thumb != null) && (thumb.isStateful())
            && thumb.setState(getDrawableState()))
        {
            invalidateDrawable(thumb);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (mThumb != null) {
            mThumb.setHotspot(x, y);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int saveCount = canvas.save();
        /* getPaddingLeft() and getPaddingTop() return the rotated padding values
         * that we gave to the view, so we don't need to consider direction here.
         */
        canvas.translate(mOffsetX, mOffsetY);
        if (mTrack != null) {
            mTrack.draw(canvas);
        }
        if (mThumb != null) {
            mThumb.draw(canvas);
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
         if (!mMeasured) {
            mMeasured = true;
            updateThumbAndTrackPos(0, 0, false);
        }
        int dw = 2 * mTrackHalfWidth;
        int dh = 2 * mTrackHalfHeight;
        switch (mDirection) {
            case SLIDER_RIGHTWARDS:
            case SLIDER_LEFTWARDS:
                dw = (Math.max(mMinWidth, Math.min(mMaxWidth, dw)));
                dh = (Math.max(mMinHeight, Math.min(mMaxHeight, dh)));
                break;
            case SLIDER_UPWARDS:
            case SLIDER_DOWNWARDS:
                dw = (Math.max(mMinHeight, Math.min(mMaxHeight, dw)));
                dh = (Math.max(mMinWidth, Math.min(mMaxWidth, dh)));
                break;
        }
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    /* Called when we have decided that the user is doing a drag or a long click,
     * to prevent a containing [Horizontal]ScrollView from reinterpreting the
     * user's action as a scroll or a fling.
     */
    private void requestDisallowInterceptTouchEvent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void startDrag() {
        removeCallbacks(longClickTimer); // cancel the timeout
        requestDisallowInterceptTouchEvent();
        mTimerRunning = false;
        mIsDragging = true;
        setPressed(true);
        invalidate();
    }

    /* Reset the displayed state of the thumb and the track to "not touched".
     * Normally the track doesn't change, but we allow for the possibility that it does.
     * Clears mIsDragging and returns the old value.
     */
    private boolean actionDone() {
        removeCallbacks(longClickTimer); // cancel the timeout
        mTimerRunning = false;
        mLongClicked = false;
        setPressed(false);
        boolean result = mIsDragging;
        mIsDragging = false;
        /* We have to invalidate the whole view because the thumb was drawn on top
         * of the track and the background and we must reinstate what was under it.
         */
        invalidate();
        return result;
    }

    private void setHotspot(float x, float y) {
        final Drawable bg = getBackground();
        if (bg != null) {
            bg.setHotspot(x, y);
        }
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value, boolean animate) {
        if (value < mMin) { value = mMin; }
        if (value > mMax) { value = mMax; }
        if (value == mValue) { return; } // no change from current
        if (animate) {
            // should be able to re-use by doing
            // animator.setValues(vStart, vEnd);
            final ObjectAnimator animator =
                ObjectAnimator.ofInt(this, VISUAL_VALUE, value);
            animator.setAutoCancel(true);
            animator.setDuration(VALUE_ANIM_DURATION);
            animator.setInterpolator(VALUE_ANIM_INTERPOLATOR);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLastValueAnimator = null;
                }
            });
            animator.start();
            mLastValueAnimator = animator;
        } else {
            if (mLastValueAnimator != null) {
                mLastValueAnimator.cancel();
                mLastValueAnimator = null;
            }
            mValue = value;
            setThumbPos(value);
        }
    }

    public void setValue(int value) {
        setValue(value, false);
    }

    private synchronized void setValueInternal(int value, boolean animate) {
        setValue(value, animate);
        if ((mValueChangeListener != null) && !animate) {
            mValueChangeListener.onValueChanged(this, value);
        }
    }

    private void trackTouchEvent(int x, int y) {
        final float scale;
        // One of these is redundant, but it isn't worth the cost of testing which
        final int width = getWidth();
        final int height = getHeight();
        switch (mDirection) {
            case SLIDER_RIGHTWARDS: default:
                if (x > width - getPaddingRight()) { scale = 1.0F; }
                else if (x < getPaddingLeft()) { scale = 0.0F; }
                else {
                    scale = (x - getPaddingLeft()) /
                        (float)(width - getPaddingLeft() - getPaddingRight());
                }
                break;
            case SLIDER_UPWARDS:
                if (y < getPaddingTop()) { scale = 1.0F; }
                else if (y > height - getPaddingBottom()) { scale = 0.0F; }
                else {
                    scale = (height - getPaddingBottom() - y) /
                        (float)(height - getPaddingTop() - getPaddingBottom());
                }
                break;
            case SLIDER_LEFTWARDS:
                if (x > width - getPaddingRight()) { scale = 0.0F; }
                else if (x < getPaddingLeft()) { scale = 1.0F; }
                else {
                    scale = (width - getPaddingRight() - x) /
                        (float)(width - getPaddingLeft() - getPaddingRight());
                }
                break;
            case SLIDER_DOWNWARDS:
                if (y < getPaddingTop()) { scale = 0.0F; }
                else if (y > height - getPaddingBottom()) { scale = 1.0F; }
                else {
                    scale = (y - getPaddingTop()) /
                        (float)(height - getPaddingTop() - getPaddingBottom());
                }
                break;
        }
        setHotspot(x, y);
        setValueInternal(
            Math.round(getMin() + scale * (getMax() - getMin())), true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) { return false; }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = event.getX();
                mStartY = event.getY();
                // It is possible to get a down event when we think that the pointer
                // is already down: we abandon any action in progress.
                removeCallbacks(longClickTimer); // cancel any active timeout
                /* We give it twice the normal long click timeout because of the
                 * user's reaction time to see that they have
                 * touched the thumb before moving it.
                 */
                postDelayed(longClickTimer,
                    2L * ViewConfiguration.getLongPressTimeout());
                mTimerRunning = true;
                mLongClicked = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mLongClicked) {
                    if (mIsDragging) {
                        trackTouchEvent(Math.round(event.getX()),
                                        Math.round(event.getY()));
                    } else if (mTimerRunning) {
                        switch (mDirection) {
                            case SLIDER_RIGHTWARDS:
                            case SLIDER_LEFTWARDS:
                                if (   (event.getX() - mStartX > mScaledTouchSlop)
                                    || (mStartX - event.getX() > mScaledTouchSlop))
                                {
                                    // real move action
                                    startDrag();
                                    trackTouchEvent(Math.round(event.getX()),
                                                    Math.round(event.getY()));
                                }
                                break;
                            case SLIDER_UPWARDS:
                            case SLIDER_DOWNWARDS:
                                if (   (event.getY() - mStartY > mScaledTouchSlop)
                                    || (mStartY - event.getY() > mScaledTouchSlop))
                                {
                                    // real move action
                                    startDrag();
                                    trackTouchEvent(Math.round(event.getX()),
                                                    Math.round(event.getY()));
                                }
                                break;
                        }
                    } // else should be impossible
                } // else ignore it after deciding this is a long click
                break;
                case MotionEvent.ACTION_UP:
                    if (!mLongClicked) {
                        setPressed(true);
                        trackTouchEvent(Math.round(event.getX()),
                                        Math.round(event.getY()));
                        actionDone(); // calls setPressed(false)
                    } else { mLongClicked = false; }
                break;
            case MotionEvent.ACTION_CANCEL:
                /* This is usually caused by a containing View
                 * in its onInterceptTouchEvent() method deciding that the user is
                 * doing a fling or a scroll, and it will take over handling of
                 * subsequent MotionEvents until it sees an ACTION_UP.
                 * However it can happen for other reasons.
                 */
                if (actionDone()) {
                    // Was dragging, jump back to where we started without animation
                    setValueInternal(mStartValue, false);
                }
                break;
        }
        requestFocus();
        return true; // if we're enabled, we always handle it
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Slider.class.getName();
    }
}
