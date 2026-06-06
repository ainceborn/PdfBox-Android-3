/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Android Paint for a tiling pattern, which consists of a small repeating graphical figure.
 * Uses BitmapShader instead of TexturePaint.
 *
 * @author Kanstantsin Valeitsenak
 */

package com.ainceborn.pdfbox.rendering;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

import com.ainceborn.harmony.awt.geom.AffineTransform;
import com.ainceborn.pdfbox.pdmodel.common.PDRectangle;
import com.ainceborn.pdfbox.pdmodel.graphics.color.PDColor;
import com.ainceborn.pdfbox.pdmodel.graphics.color.PDColorSpace;
import com.ainceborn.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import com.ainceborn.pdfbox.util.Matrix;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class TilingPaint {

    private static final String TAG = "TilingPaint";

    /**
     * Защита от OOM.
     * 2048 обычно достаточно для pattern texture.
     */
    private static final int MAX_BITMAP_EDGE = 2048;

    private final Paint paint;
    private final Matrix patternMatrix;

    TilingPaint(PageDrawer drawer,
                PDTilingPattern pattern,
                AffineTransform xform) throws IOException {
        this(drawer, pattern, null, null, xform);
    }

    TilingPaint(PageDrawer drawer,
                PDTilingPattern pattern,
                PDColorSpace colorSpace,
                PDColor color,
                AffineTransform xform) throws IOException {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        patternMatrix = Matrix.concatenate(
                drawer.getInitialMatrix(),
                pattern.getMatrix());

        Bitmap bitmap = getImage(
                drawer,
                pattern,
                colorSpace,
                color,
                xform,
                getAnchorRect(pattern)
        );

        BitmapShader shader = new BitmapShader(
                bitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
        );

        android.graphics.Matrix m = buildPatternMatrix(drawer, pattern, xform);

        shader.setLocalMatrix(m);

        paint.setShader(shader);

        this.paint = paint;
    }

    private android.graphics.Matrix buildPatternMatrix(PageDrawer drawer,
                                      PDTilingPattern pattern,
                                      AffineTransform xform
    ) {

        // PDF base transform
        android.graphics.Matrix m = new android.graphics.Matrix();

        // drawer initial (page → device)
        m.set(drawer.getInitialMatrix().createAffineTransform().toMatrix());

        // pattern space transform
        m.preConcat(pattern.getMatrix().createAffineTransform().toMatrix());

        // external transform (content transform)
        if (xform != null) {
            m.preConcat(xform.toMatrix());
        }

        return m;
    }

    Paint getPaint() {
        return paint;
    }

    private Bitmap getImage(PageDrawer drawer,
                            PDTilingPattern pattern,
                            PDColorSpace colorSpace,
                            PDColor color,
                            AffineTransform xform,
                            RectF anchorRect) throws IOException {

        float width = Math.abs(anchorRect.width());
        float height = Math.abs(anchorRect.height());

        Matrix xformMatrix = new Matrix(xform);

        float xScale = Math.abs(xformMatrix.getScalingFactorX());
        float yScale = Math.abs(xformMatrix.getScalingFactorY());

        width *= xScale;
        height *= yScale;

        int rasterWidth = Math.max(1, ceiling(width));
        int rasterHeight = Math.max(1, ceiling(height));

        if (rasterWidth > MAX_BITMAP_EDGE) {
            rasterWidth = MAX_BITMAP_EDGE;
        }

        if (rasterHeight > MAX_BITMAP_EDGE) {
            rasterHeight = MAX_BITMAP_EDGE;
        }

        Log.d(TAG, "Pattern bitmap: "
                + rasterWidth + "x" + rasterHeight);

        Bitmap bitmap = Bitmap.createBitmap(
                rasterWidth,
                rasterHeight,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        if (pattern.getYStep() < 0) {
            canvas.translate(0, rasterHeight);
            canvas.scale(1f, -1f);
        }

        if (pattern.getXStep() < 0) {
            canvas.translate(rasterWidth, 0);
            canvas.scale(-1f, 1f);
        }

        canvas.scale(xScale, yScale);

        Matrix newPatternMatrix =
                Matrix.getScaleInstance(
                        Math.abs(patternMatrix.getScalingFactorX()),
                        Math.abs(patternMatrix.getScalingFactorY()));

        PDRectangle bbox = pattern.getBBox();

        if (bbox != null) {
            newPatternMatrix.translate(
                    -bbox.getLowerLeftX(),
                    -bbox.getLowerLeftY());
        }

        drawer.drawTilingPattern(
                canvas,
                pattern,
                colorSpace,
                color,
                newPatternMatrix);

        return bitmap;
    }

    private static int ceiling(double num) {
        BigDecimal decimal = BigDecimal.valueOf(num);
        decimal = decimal.setScale(
                5,
                RoundingMode.CEILING);

        return decimal.intValue();
    }

    private RectF getAnchorRect(PDTilingPattern pattern)
            throws IOException {

        PDRectangle bbox = pattern.getBBox();

        if (bbox == null) {
            throw new IOException(
                    "Pattern /BBox is missing");
        }

        float xStep = pattern.getXStep();

        if (Float.compare(xStep, 0) == 0) {
            xStep = bbox.getWidth();
        }

        float yStep = pattern.getYStep();

        if (Float.compare(yStep, 0) == 0) {
            yStep = bbox.getHeight();
        }

        float xScale = patternMatrix.getScalingFactorX();
        float yScale = patternMatrix.getScalingFactorY();

        float width = xStep * xScale;
        float height = yStep * yScale;

        float left = bbox.getLowerLeftX() * xScale;
        float top = bbox.getLowerLeftY() * yScale;

        return new RectF(
                left,
                top,
                left + width,
                top + height);
    }
}