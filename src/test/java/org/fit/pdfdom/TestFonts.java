package org.fit.pdfdom;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mabb.fontverter.woff.WoffFont;
import org.mabb.fontverter.woff.WoffParser;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

public class TestFonts
{
    @Test
    public void convertPdfWithBareCffFont_outputHtmlHasWoffFontInStyle() throws Exception
    {
        Document html = TestUtils.parseWithPdfDomTree("/fonts/bare-cff.pdf");
        Element style = html.select("style").get(0);

        Assert.assertThat(style.outerHtml(), containsString("@font-face"));
        Assert.assertThat(style.outerHtml(), containsString("x-font-woff"));
    }

    @Test
    public void convertPdfWithBareCffFont_outputHtmlFontIsReadable() throws Exception
    {
        Document html = TestUtils.parseWithPdfDomTree("/fonts/bare-cff.pdf");
        Element style = html.select("style").get(0);

        Matcher matcher = Pattern.compile("x-font-woff;base64,([^']*)'").matcher(style.outerHtml());
        Assert.assertTrue(matcher.find());

        String base64Data = matcher.group(1);
        byte[] fontData = Base64.decodeBase64(base64Data);
        WoffFont font = new WoffParser().parse(fontData);

        Assert.assertThat(font.getTables().size(), greaterThan(1));
    }

    @Test
    public void convertPdfWithBareCffFont_divElementStyleIsUsingFont() throws Exception
    {
        Document html = TestUtils.parseWithPdfDomTree("/fonts/bare-cff.pdf");

        Element div = html.select("div.p").get(0);
        String divStyle = div.attr("style");

        Assert.assertThat(divStyle, containsString("font-family:"));
    }
//
//    @Test
//    public void convertPdfWithTtfFonts() throws Exception
//    {
//        // !todo! volvo manual replace with actual test pdf before pull request/merge!
//        Document html = TestUtils.parseWithPdfDomTree("/fonts/ttfs.pdf");
//
//        Element div = html.select("div.p").get(0);
//        String divStyle = div.attr("style");
//
//        Assert.assertThat(divStyle, containsString("font-family:"));
//    }
//
//    @Test
//    public void convertBr() throws Exception
//    {
//        Document html = TestUtils.parseWithPdfDomTree("/brno30.pdf");
//
//    }
//
//    @Test
//    public void convertPdfWitsashTtfFonts() throws Exception
//    {
//        // !todo! volvo manual replace with actual test pdf before pull request/merge!
//        Document html = TestUtils.parseWithPdfDomTree("/HorariosMadrid_Segovia.pdf");
//
//    }

}
