package org.esupportail.esupsignature.service.validation;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

class PdfPrintingCopyTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 90, 180, 270})
    void printingCopyPreservesVisibleContentWithoutInteractiveObjects(int rotation) throws Exception {
        byte[] original;
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(144, 216));
            page.setRotation(rotation);
            source.addPage(page);
            source.getDocumentCatalog().setOCProperties(new PDOptionalContentProperties());
            PDAnnotationText annotation = new PDAnnotationText();
            annotation.setRectangle(new PDRectangle(20, 20, 20, 20));
            page.getAnnotations().add(annotation);
            try (PDPageContentStream content = new PDPageContentStream(source, page)) {
                content.setNonStrokingColor(Color.RED);
                content.addRect(40, 60, 30, 40);
                content.fill();
            }
            source.save(output);
            original = output.toByteArray();
        }
        byte[] result = new PdfService(null, null, null, null, null).flattenForPrinting(original);
        try (PDDocument source = Loader.loadPDF(original); PDDocument copy = Loader.loadPDF(result)) {
            assertThat(copy.getNumberOfPages()).isEqualTo(1);
            assertThat(copy.getPage(0).getAnnotations()).isEmpty();
            assertThat(copy.getDocumentCatalog().getOCProperties()).isNull();
            assertThat(copy.getDocumentCatalog().getAcroForm()).isNull();
            assertThat(copy.getSignatureDictionaries()).isEmpty();
            BufferedImage expected = new PDFRenderer(source).renderImageWithDPI(0, 72);
            BufferedImage actual = new PDFRenderer(copy).renderImageWithDPI(0, 72);
            assertThat(actual.getWidth()).isEqualTo(expected.getWidth());
            assertThat(actual.getHeight()).isEqualTo(expected.getHeight() + 20);
            // Raster resampling changes edge pixels, but must preserve the visible page.
            long difference = 0;
            for (int y = 0; y < expected.getHeight(); y++) {
                for (int x = 0; x < expected.getWidth(); x++) {
                    Color expectedColor = new Color(expected.getRGB(x, y));
                    Color actualColor = new Color(actual.getRGB(x, y));
                    difference += Math.abs(expectedColor.getRed() - actualColor.getRed());
                    difference += Math.abs(expectedColor.getGreen() - actualColor.getGreen());
                    difference += Math.abs(expectedColor.getBlue() - actualColor.getBlue());
                }
            }
            double meanDifference = (double) difference / (expected.getWidth() * expected.getHeight() * 3);
            assertThat(meanDifference).isLessThan(2);

        }
    }
}
