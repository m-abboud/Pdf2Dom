package org.fit.pdfdom;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug utility to extract all fonts in a given PDF
 */
public class PDFFontExtractor extends PDFTextStripper
{
    public static void main(String[] args) throws IOException
    {
        String extractPath = "extracted-pdf-fonts/";

        File pdf = new File("C:\\projects\\Pdf2Dom\\src\\test\\resources\\brno30.pdf");
//        File pdf = new File("C:\\temp-downloads");
        if (pdf.isDirectory())
        {
            List<File> pdfFiles = (List<File>) FileUtils.listFiles(pdf, new String[]{"pdf"}, true);
            for (File fileOn : pdfFiles)
                try
                {
                    extractPdfFonts(extractPath, fileOn);
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                }
        } else
            extractPdfFonts(extractPath, pdf);

    }

    private static void extractPdfFonts(String extractPath, File pdfFile) throws IOException
    {
        File fontExtractDir = new File(extractPath);
        if (!fontExtractDir.exists())
            fontExtractDir.mkdir();

        PDDocument pdf = PDDocument.load(pdfFile);

        PDFFontExtractor fontExtractor = new PDFFontExtractor(extractPath);
        fontExtractor.pdfFile = pdfFile;

        fontExtractor.extractFonts(pdf);

        pdf.close();
    }

    private static Logger log = LoggerFactory.getLogger(PDFFontExtractor.class);
    private PDPage pdpage;
    private final String extractPath;
    private File pdfFile;
    private List<PDFont> extractedFonts = new ArrayList<PDFont>();

    public PDFFontExtractor(String extractPath) throws IOException
    {
        super();
        this.extractPath = extractPath;
    }

    public void extractFonts(PDDocument pdf) throws IOException
    {
        Writer output = new StringWriter();
        writeText(pdf, output);
        output.close();
    }

    public void processPage(PDPage page) throws IOException
    {
        pdpage = page;
        tryExtractPageFonts();
        super.processPage(page);
    }

    protected void tryExtractPageFonts()
    {
        PDResources resources = pdpage.getResources();
        if (resources == null)
            return;

        try
        {
            extractFontResources(resources);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void extractFontResources(PDResources resources) throws IOException
    {
        for (COSName key : resources.getFontNames())
        {
            PDFont font = resources.getFont(key);
            extractFont(font);
        }

        for (COSName name : resources.getXObjectNames())
        {
            PDXObject xobject = resources.getXObject(name);
            if (xobject instanceof PDFormXObject)
            {
                PDFormXObject xObjectForm = (PDFormXObject) xobject;
                PDResources formResources = xObjectForm.getResources();
                if (formResources != null)
                    extractFontResources(formResources);
            }
        }

    }

    private void extractFont(PDFont font) throws IOException
    {
        String fileEnding = "";
        PDStream fontStream = null;
        PDFontDescriptor fontDesc = font.getFontDescriptor();

        if (font instanceof PDTrueTypeFont)
        {
            fontStream = fontDesc.getFontFile2();
            fileEnding = ".ttf";
        } else if (font instanceof PDType0Font)
        {
            PDCIDFont descendantFont = ((PDType0Font) font).getDescendantFont();
            if (descendantFont instanceof PDCIDFontType2)
            {
                fontStream = fontDesc.getFontFile2();
                fileEnding = ".ttf";
            } else
                log.warn("Skipped: " + font.getName() + " type0");
        } else if (font instanceof PDType1CFont)
        {
            fontStream = fontDesc.getFontFile3();
            fileEnding = ".cff";
        } else
            log.info("Skipped: " + font.getName() + " type unkown");

        if (!fileEnding.isEmpty() && fontStream != null)
        {
            String fontFilePath = extractPath + font.getName() + fileEnding;
            if (!hasExtractedFont(font))
            {
                extractedFonts.add(font);
                FileUtils.writeByteArrayToFile(new File(fontFilePath), fontStream.toByteArray());
                log.warn("Extracted: " + fontFilePath);
            }
        }
    }

    private boolean hasExtractedFont(PDFont font)
    {
        for (PDFont fontOn : extractedFonts)
            if(fontOn.getName().equals(font.getName()) && fontOn.getClass() == font.getClass())
                return true;

        return false;
    }
}
