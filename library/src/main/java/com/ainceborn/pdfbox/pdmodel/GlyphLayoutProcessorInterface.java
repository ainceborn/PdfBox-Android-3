package com.ainceborn.pdfbox.pdmodel;

import com.ainceborn.pdfbox.pdmodel.font.PDFont;
import com.ainceborn.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;

public interface GlyphLayoutProcessorInterface {
    /**
     * Checks if the font is supported
     *
     * @param font to be checked
     * @return true if glyph layout is supported for this font and this font is a PDType0Font
     */
    boolean supportsFont(PDFont font);

    /**
     * Shows a text using glyph positioning (if needed)
     *
     * @param contentStream the content stream
     * @param font to be used
     * @param fontSize font size
     * @param text text to show
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if glyphs are missing
     */
    void showText(ContentStreamForGlyphLayoutInterface contentStream, PDType0Font font, float fontSize, String text) throws IOException;
}