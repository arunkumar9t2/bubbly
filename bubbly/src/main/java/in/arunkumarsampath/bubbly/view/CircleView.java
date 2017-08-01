package in.arunkumarsampath.bubbly.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import in.arunkumarsampath.bubbly.R;

/**
 * Created by Arunkumar on 20/05/17.
 */
public class CircleView extends View {

    final Paint drawPaint;

    @ColorInt
    private int bgColor;

    public CircleView(Context context) {
        this(context, null, 0);
    }

    public CircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bgColor = ContextCompat.getColor(context, R.color.bubbly_default_color);
        drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawPaint.setStyle(Paint.Style.FILL);
        drawPaint.setColor(bgColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float outerRadius = (float) (getMeasuredWidth() / 2.4);
        canvas.drawCircle(getMeasuredWidth() / 2,
                getMeasuredWidth() / 2,
                outerRadius,
                drawPaint);
    }

    @ColorInt
    public int getColor() {
        return bgColor;
    }

    public void setColor(@ColorInt int color) {
        bgColor = color;
        drawPaint.setColor(color);
        invalidate();
    }
}
