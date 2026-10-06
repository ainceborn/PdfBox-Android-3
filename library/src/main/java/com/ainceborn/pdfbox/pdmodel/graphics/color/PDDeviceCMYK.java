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
package com.ainceborn.pdfbox.pdmodel.graphics.color;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.ainceborn.pdfbox.cos.COSName;

/**
 * Allows colors to be specified according to the subtractive CMYK (cyan, magenta, yellow, black)
 * model typical of printers and other paper-based output devices.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public class PDDeviceCMYK extends PDDeviceColorSpace
{
   public static PDDeviceCMYK INSTANCE;
   static
   {
      INSTANCE = new PDDeviceCMYK();
   }

   private final PDColor initialColor = new PDColor(new float[]{0, 0, 0, 1}, this);

   protected PDDeviceCMYK() {}

   @Override
   public String getName()
   {
      return COSName.DEVICECMYK.getName();
   }

   @Override
   public int getNumberOfComponents()
   {
      return 4;
   }

   @Override
   public float[] getDefaultDecode(int bitsPerComponent)
   {
      return new float[] {0, 1, 0, 1, 0, 1, 0, 1};
   }

   @Override
   public PDColor getInitialColor()
   {
      return initialColor;
   }

   @Override
   public float[] toRGB(float[] value)
   {
      // CMYK values are always 0..1 here.
      float c = value[0];
      float m = value[1];
      float y = value[2];
      float k = value[3];

      // Convert to RGB in range 0..1
      float r = (1 - c) * (1 - k);
      float g = (1 - m) * (1 - k);
      float b = (1 - y) * (1 - k);

      return new float[]{r, g, b};
   }

   @Override
   public Bitmap toRGBImage(Bitmap raster)
   {
      int width = raster.getWidth();
      int height = raster.getHeight();
      Bitmap rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      int[] pixels = new int[width * height];
      raster.getPixels(pixels, 0, width, 0, 0, width, height);

      for (int i = 0; i < pixels.length; i++) {
         int p = pixels[i];
         // C, M, Y, K are stored in R, G, B, A channels respectively
         float c = Color.red(p) / 255f;
         float m = Color.green(p) / 255f;
         float y = Color.blue(p) / 255f;
         float k = Color.alpha(p) / 255f;

         float red = (1 - c) * (1 - k);
         float green = (1 - m) * (1 - k);
         float blue = (1 - y) * (1 - k);

         int r = Math.round(red * 255f);
         int g = Math.round(green * 255f);
         int b = Math.round(blue * 255f);

         pixels[i] = Color.rgb(r, g, b);
      }

      rgbBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
      return rgbBitmap;
   }
}

