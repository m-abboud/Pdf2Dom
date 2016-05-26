/**
 *
 */
package org.fit.pdfdom;

import java.io.IOException;
import java.util.HashMap;

import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.mabb.fontverter.FVFont;
import org.mabb.fontverter.FontVerter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A table for storing entries about the embedded fonts and their usage.
 *
 * @author burgetr
 */
public class FontTable extends HashMap<String, FontTable.Entry>
{
    Logger log = LoggerFactory.getLogger(FontTable.class);
    private static final long serialVersionUID = 1L;
    private static int nextNameIndex = 1;

    public void addEntry(String fontName, PDFontDescriptor descriptor)
    {
        FontTable.Entry entry = get(fontName);
        if (entry == null)
        {
            String usedName = nextUsedName();
            FontTable.Entry newEntry = new FontTable.Entry(fontName, usedName, descriptor);

            if(newEntry.isEntryValid())
                put(fontName, newEntry);
        }
    }

    public String getUsedName(String fontName)
    {
        FontTable.Entry entry = get(fontName);
        if (entry == null)
            return null;
        else
            return entry.usedName;
    }

    protected String nextUsedName()
    {
        return "F" + (nextNameIndex++);
    }

    public class Entry
    {
        public String fontName;
        public String usedName;
        public PDFontDescriptor descriptor;

        private byte[] cachedFontData;
        private String mimeType = "x-font-truetype";

        public Entry(String fontName, String usedName, PDFontDescriptor descriptor)
        {
            this.fontName = fontName;
            this.usedName = usedName;
            this.descriptor = descriptor;
        }

        public String getDataURL() throws IOException
        {
            char[] cdata = new char[0];
            if (getFontData() != null)
                cdata = Base64Coder.encode(getFontData());

            return String.format("data:application/%s;base64,%s", mimeType, new String(cdata));
        }

        public byte[] getFontData() throws IOException
        {
            if (cachedFontData != null)
                return cachedFontData;

            if (descriptor.getFontFile2() != null)
                cachedFontData = loadTrueTypeFont(descriptor.getFontFile2());
            else if (descriptor.getFontFile() != null)
                cachedFontData = loadType1Font(descriptor.getFontFile());
            else if (descriptor.getFontFile3() != null)
                // FontFile3 docs say any font type besides TTF/OTF or Type 1..
                cachedFontData = loadOtherTypeFont(descriptor.getFontFile3());

            return cachedFontData;
        }

        public boolean isEntryValid() {
            byte[] fontData = new byte[0];
            try
            {
                fontData = getFontData();
            } catch (IOException e)
            {
                log.warn("Error loading font '{}' Message: {} {}", fontName, e.getMessage(), e.getClass());
            }

            return fontData != null && fontData.length != 0;
        }

        private byte[] loadTrueTypeFont(PDStream fontFile) throws IOException
        {
            // could convert to WOFF though for optimal html output.
            mimeType = "x-font-truetype";

            byte[] fontData = fontFile.toByteArray();
            try
            {
                // browser validation can fail for many TTF fonts from pdfs
                FVFont font = FontVerter.readFont(fontData);
                if (!font.doesPassStrictValidation())
                {
                    font.normalize();
                    fontData = font.getData();
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }

            return fontData;
        }

        private byte[] loadType1Font(PDStream fontFile) throws IOException
        {
            log.warn("Type 1 fonts are not supported by Pdf2Dom.");
            return new byte[0];
        }

        private byte[] loadOtherTypeFont(PDStream fontFile) throws IOException
        {
            // Likley Bare CFF which needs to be converted to a font supported by browsers, can be
            // other font types which are not yet supported.
            try
            {
                FVFont font = FontVerter.convertFont(fontFile.toByteArray(), FontVerter.FontFormat.WOFF1);
                mimeType = "x-font-woff";

                return font.getData();
            } catch (Exception ex) {
                log.error("Issue converting Bare CFF font or the font type is not supportedby Pdf2Dom, " +
                        "Font: {} Exception: {} {}", fontName, ex.getMessage(), ex.getClass());

                // don't barf completley for font conversion issue, html will still be useable without.
                return new byte[0];
            }
        }

        @Override
        public int hashCode()
        {
            return fontName.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Entry other = (Entry) obj;
            if (!getOuterType().equals(other.getOuterType())) return false;
            if (fontName == null)
            {
                if (other.fontName != null) return false;
            }
            else if (!fontName.equals(other.fontName)) return false;
            return true;
        }

        private FontTable getOuterType()
        {
            return FontTable.this;
        }

    }

}
