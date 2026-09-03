package com.alidogukan.avora.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.alidogukan.avora.R;
import com.alidogukan.avora.models.WeatherDay;

import java.util.Collections;
import java.util.List;

/**
 * Small, dependency-free temperature chart for the weekly forecast.
 */
public class WeatherTemperatureChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<WeatherDay> days = Collections.emptyList();

    public WeatherTemperatureChartView(Context c, AttributeSet a) {
        super(c, a);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(dp(2));
        text.setTextSize(dp(11));
        grid.setColor(Color.rgb(232, 236, 232));
        grid.setStrokeWidth(1);
    }

    public void setDays(List<WeatherDay> d) {
        days = d == null ? Collections.emptyList() : d;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas c) {
        super.onDraw(c);
        if (days.isEmpty()) return;

        float l = dp(28), r = getWidth() - dp(14), t = dp(30), b = getHeight() - dp(34);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;

        for (WeatherDay d : days) {
            min = Math.min(min, val(d.getTemperatureMin()));
            max = Math.max(max, val(d.getTemperatureMax()));
        }

        if (max <= min) {
            max = min + 5;
        }

        for (int i = 0; i < 4; i++) {
            float y = t + (b - t) * i / 3f;
            c.drawLine(l, y, r, y, grid);
        }

        drawSeries(c, true, l, r, t, b, min, max, 0xFFFB8C00);
        drawSeries(c, false, l, r, t, b, min, max, 0xFF1E88E5);

        text.setColor(0xFF6F756F);
        text.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < days.size(); i++) {
            float x = l + (r - l) * i / Math.max(1, days.size() - 1);
            String date = days.get(i).getDate();
            c.drawText(date.length() > 5 ? date.substring(5) : date, x, getHeight() - dp(12), text);
        }

        text.setTextAlign(Paint.Align.LEFT);
        text.setColor(0xFF1F241F);
        text.setTextSize(dp(13));
        c.drawText(getContext().getString(R.string.weather_temperature_chart_title), dp(16), dp(20), text);
    }

    private void drawSeries(Canvas c, boolean high, float l, float r, float t, float b, double min, double max, int color) {
        line.setColor(color);
        Path p = new Path();
        for (int i = 0; i < days.size(); i++) {
            double v = val(high ? days.get(i).getTemperatureMax() : days.get(i).getTemperatureMin());
            float x = l + (r - l) * i / Math.max(1, days.size() - 1);
            float y = (float) (b - (v - min) / (max - min) * (b - t));
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
            c.drawCircle(x, y, dp(3), dot(color));
        }
        c.drawPath(p, line);
    }

    private Paint dot(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        return p;
    }

    private double val(Double v) {
        return v == null ? 0 : v;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
