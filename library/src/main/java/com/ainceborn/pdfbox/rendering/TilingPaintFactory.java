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
package com.ainceborn.pdfbox.rendering;

import android.graphics.Paint;

import com.ainceborn.harmony.awt.geom.AffineTransform;
import com.ainceborn.pdfbox.cos.COSDictionary;
import com.ainceborn.pdfbox.pdmodel.graphics.color.PDColor;
import com.ainceborn.pdfbox.pdmodel.graphics.color.PDColorSpace;
import com.ainceborn.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import com.ainceborn.pdfbox.util.Matrix;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Android Paint for a tiling pattern, which consists of a small repeating graphical figure.
 * Uses BitmapShader instead of TexturePaint.
 *
 * @author Tilman Hausherr
 * @author Kanstantsin Valeitsenak
 */
class TilingPaintFactory
{
    private static final Logger LOG = LogManager.getLogManager().getLogger(TilingPaintFactory.class.getName());

    private final PageDrawer drawer;
    private final Map<TilingPaintParameter, WeakReference<TilingPaint>> weakCache
            = new WeakHashMap<>();

    TilingPaintFactory(PageDrawer drawer)
    {
        this.drawer = drawer;
    }

    Paint create(PDTilingPattern pattern, PDColorSpace colorSpace,
                 PDColor color, AffineTransform xform) throws IOException
    {
        TilingPaint paint = null;
        TilingPaintParameter tilingPaintParameter
                = new TilingPaintParameter(drawer.getInitialMatrix(), pattern.getCOSObject(), colorSpace, color, xform);
        WeakReference<TilingPaint> weakRef = weakCache.get(tilingPaintParameter);
        if (weakRef != null)
        {
            // PDFBOX-4058: additional WeakReference makes gc work better
            paint = weakRef.get();
        }
        if (paint == null)
        {
            paint = new TilingPaint(drawer, pattern, colorSpace, color, xform);
            weakCache.put(tilingPaintParameter, new WeakReference<>(paint));
        }
        return paint.getPaint();
    }

    // class to characterize a TilingPaint object. It is important that TilingPaint does not
    // keep any objects from this class, so that the weak cache works.
    private static class TilingPaintParameter
    {
        private final Matrix matrix;
        private final COSDictionary patternDict;
        private final PDColorSpace colorSpace;
        private final PDColor color;
        private final AffineTransform xform;

        private TilingPaintParameter(Matrix matrix, COSDictionary patternDict, PDColorSpace colorSpace,
                                     PDColor color, AffineTransform xform)
        {
            this.matrix = matrix.clone();
            this.patternDict = patternDict;
            this.colorSpace = colorSpace;
            this.color = color;
            this.xform = xform;
        }

        // this may not catch all equals, but at least those related to one resource dictionary.
        // it isn't needed to investigate further because matrix or transform would be different anyway.
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof TilingPaintParameter))
            {
                return false;
            }
            final TilingPaintParameter other = (TilingPaintParameter) obj;
            if (!Objects.equals(this.matrix, other.matrix))
            {
                return false;
            }
            if (!Objects.equals(this.patternDict, other.patternDict))
            {
                return false;
            }
            if (!Objects.equals(this.colorSpace, other.colorSpace))
            {
                return false;
            }
            if (this.color == null && other.color != null)
            {
                return false;
            }
            if (this.color != null && other.color == null)
            {
                return false;
            }
            if (this.color != null && this.color.getColorSpace() != other.color.getColorSpace())
            {
                return false;
            }
            try
            {
                if (this.color != null && other.color != null &&
                        this.color != other.color && this.color.toRGB() != other.color.toRGB())
                {
                    return false;
                }
            }
            catch (IOException ex)
            {
                LOG.log(Level.INFO,"Couldn't convert color to RGB - treating as not equal", ex);
                return false;
            }
            return Objects.equals(this.xform, other.xform);
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 23 * hash + (this.matrix != null ? this.matrix.hashCode() : 0);
            hash = 23 * hash + (this.patternDict != null ? this.patternDict.hashCode() : 0);
            hash = 23 * hash + (this.colorSpace != null ? this.colorSpace.hashCode() : 0);
            hash = 23 * hash + (this.color != null ? this.color.hashCode() : 0);
            hash = 23 * hash + (this.xform != null ? this.xform.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString()
        {
            return "TilingPaintParameter{" + "matrix=" + matrix + ", pattern=" + patternDict
                    + ", colorSpace=" + colorSpace + ", color=" + color + ", xform=" + xform + '}';
        }
    }
}
