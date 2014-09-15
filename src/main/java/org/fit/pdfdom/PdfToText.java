package org.fit.pdfdom;


import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import com.ibm.icu.text.*;

public class PdfToText
{

    public static void main(String[] args)
    {
        PDDocument pddDocument;
        try
        {
            pddDocument = PDDocument.load(new File("test/pdf/arabic/ar1.pdf"));
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSuppressDuplicateOverlappingText(false);
            String Text = textStripper.getText(pddDocument);
            System.out.println(Text);
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
