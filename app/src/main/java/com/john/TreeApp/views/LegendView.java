package com.john.TreeApp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom View that draws a semi-transparent legend box mapping species
 * colour to name. Place it in a FrameLayout overlaid on the map.
 */
public class LegendView extends View {

    public static class Entry {
        public final int colour;  // ARGB colour int
        public final String label; // display text

        public Entry(int colour, String label) {
            this.colour = colour;
            this.label = label;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    // Paints
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Dimensions (dp → px resolved in constructor)
    private final float padding;
    private final float circleRadius;
    private final float rowHeight;
    private final float cornerRadius;
    private final float textSize;
    private final float titleSize;
    private final float colGap;   // gap between circle and text

    public LegendView(Context context) {
        this(context, null);
    }

    public LegendView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = context.getResources().getDisplayMetrics().density;
        padding      = 12 * density;
        circleRadius =  7 * density;
        rowHeight    = 22 * density;
        cornerRadius = 10 * density;
        textSize     = 13 * density;
        titleSize    = 14 * density;
        colGap       =  8 * density;

        bgPaint.setColor(Color.argb(210, 255, 255, 255));
        bgPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(titleSize);
        titlePaint.setFakeBoldText(true);
        titlePaint.setAntiAlias(true);
    }

    /** Replace all legend entries and trigger a redraw. */
    public void setEntries(List<Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Measure based on content
        float maxTextWidth = 0;
        for (Entry e : entries) {
            float w = textPaint.measureText(e.label);
            if (w > maxTextWidth) maxTextWidth = w;
        }

        float titleWidth = titlePaint.measureText("Species");
        if (titleWidth > maxTextWidth) maxTextWidth = titleWidth;

        float contentWidth  = padding + circleRadius * 2 + colGap + maxTextWidth + padding;
        float contentHeight = padding + titleSize + 4 + rowHeight * entries.size() + padding;

        setMeasuredDimension((int) Math.ceil(contentWidth), (int) Math.ceil(contentHeight));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        // Background rounded rectangle
        RectF bg = new RectF(0, 0, w, h);
        canvas.drawRoundRect(bg, cornerRadius, cornerRadius, bgPaint);

        // Title
        float titleY = padding + titleSize;
        canvas.drawText("Species", padding, titleY, titlePaint);

        // Entries
        float y = titleY + 4 + rowHeight * 0.8f;
        for (Entry entry : entries) {
            circlePaint.setColor(entry.colour);
            circlePaint.setStyle(Paint.Style.FILL);
            float cx = padding + circleRadius;
            float cy = y - circleRadius * 0.5f;
            canvas.drawCircle(cx, cy, circleRadius, circlePaint);

            // Thin dark border on circle for readability
            circlePaint.setColor(Color.argb(120, 0, 0, 0));
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(1.5f);
            canvas.drawCircle(cx, cy, circleRadius, circlePaint);

            canvas.drawText(entry.label, padding + circleRadius * 2 + colGap, y, textPaint);
            y += rowHeight;
        }
    }
}
