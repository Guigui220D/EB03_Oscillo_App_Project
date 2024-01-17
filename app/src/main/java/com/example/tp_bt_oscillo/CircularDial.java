package com.example.tp_bt_oscillo;

import static androidx.core.math.MathUtils.clamp;

import static java.lang.Math.sqrt;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

public class CircularDial extends View {
    // Création dynamique du slider
    public CircularDial(Context context)
    {
        super(context);
        init(context, null);
    }

    // Création statique du slider
    public CircularDial(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public interface SliderChangeListener
    {
        void onChange(float value);
    }

    private void init(Context context, AttributeSet attrs)
    {
        float dpInnerRadius = DEFAULT_DISPLAY_DIAMETER / 2.f;
        float dpOuterRadius = DEFAULT_DIAL_DIAMETER / 2.f;
        float dpTotalRadius = DEFAULT_DIAL_DIAMETER / 2.f + DEFAULT_OUTLINE_THICKNESS;
        float dpTickThickness = DEFAULT_TICK_THICKNESS;

        if (attrs != null) {
            TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.CircularDial, 0, 0);

            dpOuterRadius = attr.getDimension(R.styleable.CircularDial_diameter, dpOuterRadius);
            dpTotalRadius = dpOuterRadius + attr.getDimension(R.styleable.CircularDial_outline, dpToPx(DEFAULT_OUTLINE_THICKNESS));
            dpInnerRadius = attr.getDimension(R.styleable.CircularDial_displayDiameter, dpOuterRadius / 2.f);
            dpTickThickness = attr.getDimension(R.styleable.CircularDial_tickThickness, dpTickThickness);

            attr.recycle();
        }

        m_innerRadius = dpToPx(dpInnerRadius);
        m_outerRadius = dpToPx(dpOuterRadius);
        m_totalRadius = dpToPx(dpTotalRadius);

        m_outlinePaint = new Paint();
        m_insidePaint = new Paint();
        m_selectedPaint = new Paint();
        m_unselectedPaint = new Paint();
        m_bigTicksPaint = new Paint();
        m_smallTicksPaint = new Paint();
        m_textPaint = new Paint();

        m_outlinePaint.setColor(ContextCompat.getColor(context, R.color.lightGrey));
        m_insidePaint.setColor(ContextCompat.getColor(context, R.color.colorPrimaryDarker));
        m_selectedPaint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        m_unselectedPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimary));
        m_bigTicksPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        m_smallTicksPaint.setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        m_textPaint.setColor(ContextCompat.getColor(context, R.color.white));

        m_bigTicksPaint.setStrokeWidth(dpToPx(dpTickThickness));
        m_smallTicksPaint.setStrokeWidth(dpToPx(dpTickThickness / 1.5f));

        m_bigTicksPaint.setStrokeCap(Paint.Cap.ROUND);
        m_smallTicksPaint.setStrokeCap(Paint.Cap.ROUND);

        m_textPaint.setTextAlign(Paint.Align.CENTER);
        m_textPaint.setTextSize(dpToPx(m_innerRadius / 1.5f));

        setMinimumHeight((int)dpToPx(MIN_SLIDER_RADIUS * 2.f));
        setMinimumWidth((int)dpToPx(MIN_SLIDER_RADIUS * 2.f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //adaptDims(); TODO

        final float outline = m_totalRadius - m_outerRadius;
        final RectF rect = new RectF(
                outline,
                outline,
                outline + m_outerRadius * 2.f,
                outline + m_outerRadius * 2.f);

        canvas.drawCircle(m_totalRadius, m_totalRadius, m_totalRadius, m_outlinePaint);
        canvas.drawCircle(m_totalRadius, m_totalRadius, m_outerRadius, m_unselectedPaint);
        canvas.drawArc(rect, -90.f,ratioToAngle(m_value), true, m_selectedPaint);
        canvas.drawCircle(m_totalRadius, m_totalRadius, m_innerRadius, m_insidePaint);

        final float sliderWidth = m_outerRadius - m_innerRadius;

        for (int i = 0; i < 8; i++) {
            float x = (float)Math.cos((Math.PI / 4.f) * (float)i);
            float y = (float)Math.sin((Math.PI / 4.f) * (float)i);

            float radiusIn = m_innerRadius + 0.2f * sliderWidth;
            float radiusOut = m_innerRadius + 0.8f * sliderWidth;
            canvas.drawLine(
                    radiusIn * x + m_totalRadius, radiusIn * y + m_totalRadius,
                    radiusOut * x + m_totalRadius, radiusOut * y + m_totalRadius, m_bigTicksPaint);
        }

        for (int i = 0; i < 8; i++) {
            float x = (float)Math.cos((Math.PI / 4.f) * (float)i + Math.PI / 8.f);
            float y = (float)Math.sin((Math.PI / 4.f) * (float)i + Math.PI / 8.f);

            float radiusIn = m_innerRadius + 0.35f * sliderWidth;
            float radiusOut = m_innerRadius + 0.65f * sliderWidth;
            canvas.drawLine(
                    radiusIn * x + m_totalRadius, radiusIn * y + m_totalRadius,
                    radiusOut * x + m_totalRadius, radiusOut * y + m_totalRadius, m_smallTicksPaint);
        }

        canvas.drawText(String.valueOf((int)(m_value * 100.f)) + "%", m_totalRadius, m_totalRadius + m_innerRadius / 3.f, m_textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_MOVE:
                if (!m_afterDoubleClick) {
                    float newValue = angleToRatio(positionToAngle(new Point((int) event.getX(), (int) event.getY())));
                    // Eviter le saut de 0 à 360;
                    if (m_value < 0.1f && newValue > 0.5f) {
                        newValue = 0.f;
                    } else if (m_value > 0.9f && newValue < 0.5f) {
                        newValue = 1.f;
                    }

                    m_value = newValue;

                    invalidate();
                    if (m_sliderChangeListener != null)
                        m_sliderChangeListener.onChange(m_value);
                }
                break;

            case MotionEvent.ACTION_DOWN:
                if (m_doubleClick)
                {
                    m_doubleClick = false;
                    m_afterDoubleClick = true;
                    m_value = 0.f;
                    invalidate();
                    if (m_sliderChangeListener != null)
                        m_sliderChangeListener.onChange(m_value);
                }
                else
                {
                    m_doubleClick = true;
                    postDelayed(() -> {m_doubleClick = false;}, 500);
                }
                break;

            case MotionEvent.ACTION_UP:
                m_afterDoubleClick = false;
                break;

            //case MotionEvent.
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // TODO: current size values
        int suggestedHeight = (int)Math.max(getMinimumHeight(), m_totalRadius * 2.f + getPaddingTop() + getPaddingBottom());
        int suggestedWidth = (int)Math.max(getMinimumWidth(), m_totalRadius * 2.f + getPaddingLeft() + getPaddingRight());

        int height = resolveSize(suggestedHeight, heightMeasureSpec);
        int width = resolveSize(suggestedWidth, widthMeasureSpec);

        setMeasuredDimension(width, height);
    }

    private float dpToPx(float dp)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float angleToRatio(float angle) {
        return angle / 360.f;
    }

    private float ratioToAngle(float ratio) {
        return ratio * 360.f;
    }

    private float positionToAngle(Point point) {
        float x = -(point.y - m_totalRadius); // Ajustement du repère pour trigonométrie plus simple
        float y = +(point.x - m_totalRadius); // + cast en float (Centré autour du centre)
        float l = (float)sqrt(x * x + y * y);
        x /= l; y /= l; // Normalisation du vecteur

        float a = (float)Math.asin(y); // Détermination de l'angle
        if (x < 0) { // Mirroir de la coordonnée pour soulever l'ambiguité
            a += Math.PI - (a * 2.f);
        } else if (y < 0) {
            a += Math.PI * 2.f;
        }

        // Conversion en degrés
        return (float)(a / Math.PI) * 180.f;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Log.i("TEST", "save state");
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState, m_value);
        return savedState;
    }

    private static class SavedState extends BaseSavedState {
        private float savedValue;

        public SavedState(Parcelable superState, float savedValue) {
            super(superState);
            this.savedValue = savedValue;
        }

        private SavedState(Parcel in) {
            super(in);
            savedValue = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(savedValue);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            m_value = savedState.savedValue;
            invalidate();
            Log.i("TEST", "restore state");
            if (m_sliderChangeListener != null)
                m_sliderChangeListener.onChange(m_value);
        } else {
            super.onRestoreInstanceState(state);
        }
    }


    public void setSliderChangeListener(SliderChangeListener m_sliderChangeListener) {
        this.m_sliderChangeListener = m_sliderChangeListener;
        if (m_sliderChangeListener != null)
            m_sliderChangeListener.onChange(m_value);
    }

    public void setValue(float value) {
        m_value = clamp(value, 0.f, 1.f);
        invalidate();
    }

    public float getValue() {
        return m_value;
    }

    private SliderChangeListener m_sliderChangeListener;

    private Paint
            m_outlinePaint, m_insidePaint,
            m_selectedPaint, m_unselectedPaint,
            m_bigTicksPaint, m_smallTicksPaint,
            m_textPaint;

    private float m_value = 0.3f;

    private float m_innerRadius, // Display
            m_outerRadius, // Display + slider
            m_totalRadius; // Display + slider + outline

    private boolean m_doubleClick, m_afterDoubleClick;

    public final static float DEFAULT_DIAL_DIAMETER = 160.f; // Diameter of the dial (without outline)
    public final static float DEFAULT_OUTLINE_THICKNESS = 5.f; // Thickness of the dial outline
    public final static float DEFAULT_DISPLAY_DIAMETER = 80.f; // Diameter of the inside circle (where the % is displayed)
    public final static float DEFAULT_TICK_THICKNESS = 3.f; // Thickness of the tick strokes (big ticks)

    public final static float MIN_SLIDER_RADIUS = 160.f / 4.f;
}
