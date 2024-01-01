package com.example.melobeats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class LightingView extends View {
    private Paint paint;

    public LightingView(Context context) {
        super(context);
        init();
    }

    public LightingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LightingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        // Configure paint properties (e.g., color, style, etc.) for the lighting effect
    }

    @Override
    protected void onDraw(Canvas canvas) {

    }


}
