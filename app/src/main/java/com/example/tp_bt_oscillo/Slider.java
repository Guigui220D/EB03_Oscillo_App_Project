package com.example.tp_bt_oscillo;

import static androidx.core.math.MathUtils.clamp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

public class Slider extends View
{
    public interface SliderChangeListener
    {
        void onChange(float value);
    }

    public void setM_sliderChangeListener(SliderChangeListener m_sliderChangeListener) {
        this.m_sliderChangeListener = m_sliderChangeListener;
    }

    SliderChangeListener m_sliderChangeListener;



    // Création dynamique du slider
    public Slider(Context context)
    {
        super(context);
        init(context, null);
    }

    // Création statique du slider
    public Slider(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        m_cursorPaint = new Paint();
        m_barPaint = new Paint();
        m_valuePaint = new Paint();

        m_cursorColor = ContextCompat.getColor(context, R.color.sliderCursor);
        m_barColor = ContextCompat.getColor(context, R.color.sliderBar1);
        m_valueColor = ContextCompat.getColor(context, R.color.sliderBar2);

        m_cursorDiameter = dpToPx(DEFAUlT_CURSOR_DIAMETER);
        m_barWidth = dpToPx(DEFAULT_BAR_WIDTH);
        m_barLength = dpToPx(DEFAULT_BAR_LENGTH);

        m_cursorPaint.setStrokeWidth(m_cursorDiameter);
        m_barPaint.setStrokeWidth(m_barWidth);
        m_valuePaint.setStrokeWidth(m_barWidth);

        m_cursorPaint.setStrokeCap(Paint.Cap.ROUND);
        m_barPaint.setStrokeCap(Paint.Cap.ROUND);
        m_valuePaint.setStrokeCap(Paint.Cap.ROUND);

        m_cursorPaint.setColor(m_cursorColor);
        m_barPaint.setColor(m_barColor);
        m_valuePaint.setColor(m_valueColor);

        setMinimumHeight((int)dpToPx(MIN_CURSOR_DIAMETER + MIN_BAR_LENGTH));
        setMinimumWidth((int)dpToPx(MIN_CURSOR_DIAMETER));
    }

    private float valueToRatio(float value)
    {
        return clamp((value - m_min) / (m_max - m_min), 0, 1);
    }

    private float ratioToValue(float ratio)
    {
        return ratio * (m_max - m_min) + m_min;
    }

    private float dpToPx(float dp)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private Point toPos(float value)
    {
        int x = getPaddingLeft() + (int)(m_cursorDiameter / 2.f);
        int y = getPaddingTop() + (int)((1.f - valueToRatio(value)) * m_barLength + m_cursorDiameter / 2.f);
        return new Point(x, y);
    }

    private float toValue(Point pos)
    {
        return ratioToValue(1 - clamp(((float)(pos.y - getPaddingTop()) - m_cursorDiameter / 2f) / m_barLength, 0, 1));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        adaptDims();

        Point start = toPos(m_max);
        Point stop = toPos(m_min);
        canvas.drawLine(start.x, start.y, stop.x, stop.y, m_barPaint);

        Point value = toPos(m_value);
        Point zero = toPos(0);
        canvas.drawLine(zero.x, zero.y, value.x, value.y, m_valuePaint);

        canvas.drawCircle(value.x, value.y, m_cursorDiameter / 2.f, m_cursorPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int suggestedHeight = (int)Math.max(getMinimumHeight(), m_cursorDiameter+m_barLength+getPaddingTop()+getPaddingBottom());
        int suggestedWidth = (int)Math.max(getMinimumWidth(), m_cursorDiameter+getPaddingLeft()+getPaddingRight());

        int height = resolveSize(suggestedHeight, heightMeasureSpec);
        int width = resolveSize(suggestedWidth, widthMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_MOVE:
                if (!m_afterDoubleClick) {
                    m_value = toValue(new Point((int) event.getX(), (int) event.getY()));
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
                    m_value = clamp(0, m_min, m_max);
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
        //return super.onTouchEvent(event);
    }

    private void adaptDims()
    {
        float pt = getPaddingTop();
        float pb = getPaddingBottom();
        float pr = getPaddingRight();
        float pl = getPaddingLeft();

        float minSliderWidth = getMinimumWidth() - pl - pr;
        // le plus petit curseur excède la largeur du view
        if (minSliderWidth >= getWidth()) {
            // suppression des paddings, réduction du curseur
            m_cursorDiameter = getWidth();
            pr = 0;
            pl = 0;
        }
        // le plus petit curseur + padding excède la largeur du view
        else if(minSliderWidth + pl + pr >= getWidth())
        {
            // réduction des paddings
            m_cursorDiameter = minSliderWidth;
            float ratio = (getWidth() - minSliderWidth) / (pl + pr);
            pl *= ratio;
            pr *= ratio;
        }
        // Le curseur + padding dépasse la largeur affectée
        else if (Math.max(m_cursorDiameter, m_barWidth)+pl+pr >= getWidth())
        {
            // réduction du slider
            m_cursorDiameter = getWidth() - pl - pr;
        }

        if (m_cursorDiameter < m_barWidth)
            m_barWidth = m_cursorDiameter;

        /*
        float minSliderHeight = getMinimumHeight() - pb - pt;
        // le plus petit slider excède la hauteur du view
        if (minSliderHeight + m_barWidth >= getHeight())
        {
            // suppression des padding, raccourcissement du slider
            m_barLength = getHeight() - m_barWidth;
            m_cursorDiameter = 0;
            pt = 0;
            pb = 0;
        }
        // le plus petit slider + curseur excède la hauteur du view
        else if (minSliderHeight + m_cursorDiameter >= getHeight())
        {
            // suppression des padding, rétrécissement du curseur
            m_cursorDiameter = getHeight() - m_barLength;
            pt = 0;
            pb = 0;
        }
        // le slider + curseur + padding excède la hauteur du view
        else if (minSliderHeight + pl + pr + m_cursorDiameter >= getHeight())
        {
            // réduction des paddings
            float ratio = (getHeight() - minSliderHeight) / (pt + pb);
            pt *= ratio;
            pb *= ratio;
        }
        */

        if (m_cursorDiameter < m_barWidth)
            m_cursorDiameter = m_barWidth;

        // TODO : la meme chose sur l'axe vertical

        setPadding((int)pl, (int)pt, (int)pr, (int)pb);

    }

    public float getM_value() {
        return m_value;
    }

    public void setM_value(float m_value) {
        this.m_value = m_value;
        invalidate();
    }

    private boolean m_doubleClick = false;
    private boolean m_afterDoubleClick = false;

    private float m_value = 25;
    private float m_min = -100, m_max = 100;

    private float m_cursorDiameter;
    private float m_barWidth;
    private float m_barLength;

    private int m_cursorColor;
    private int m_valueColor;
    private int m_barColor;

    public final static float MIN_BAR_LENGTH = 160;
    public final static float MIN_CURSOR_DIAMETER = 30;
    public final static float MIN_BAR_WIDTH = 10;
    public final static float DEFAULT_BAR_LENGTH = 160;
    public final static float DEFAUlT_CURSOR_DIAMETER = 40;
    public final static float DEFAULT_BAR_WIDTH = 20;

    private Paint m_barPaint, m_cursorPaint, m_valuePaint;
}
