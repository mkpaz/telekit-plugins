package org.telekit.plugins.linetest.tool;

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.jetbrains.annotations.Nullable;
import org.telekit.base.domain.LineSeparator;
import org.telekit.base.domain.exception.TelekitException;
import org.telekit.base.preferences.Theme;
import org.telekit.base.util.PdfFont;
import org.telekit.plugins.linetest.domain.Equipment;
import org.telekit.plugins.linetest.domain.MeasuredValue;
import org.telekit.plugins.linetest.domain.Measurement;
import org.telekit.plugins.linetest.domain.MeasurementTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.plugins.linetest.domain.MeasuredValue.ValueType.*;
import static org.telekit.plugins.linetest.domain.MeasuredValue.format;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.*;

final class MeasurementTaskPrinter {

    private static final float MARGIN_X = 30;
    private static final float MARGIN_Y = 25;
    private static final int MEASURED_VALUE_PAD = 12;
    private static final int PARAGRAPH_GAP = 15;

    private final ProviderRegistry providerRegistry;
    private final Theme theme;

    private PDDocument doc;

    // external fonts that do have unicode support
    private PdfFont normalFont;
    private PdfFont headerFont;
    private PdfFont monospaceFont;

    public MeasurementTaskPrinter(ProviderRegistry providerRegistry, Theme theme) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry);
        this.theme = Objects.requireNonNull(theme);
    }

    public void print(MeasurementTask task, File outputFile) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(outputFile);

        this.doc = new PDDocument();

        try {
            normalFont = loadFont(theme.getRegularFont(FontWeight.NORMAL, FontPosture.REGULAR), 11);
            headerFont = loadFont(theme.getRegularFont(FontWeight.BOLD, FontPosture.REGULAR), 14);
            monospaceFont = loadFont(theme.getMonospaceFont(FontWeight.NORMAL), 11);

            printMainPage(task);

            Measurement measurement = task.getResult();
            if (measurement != null && measurement.getRawOutput() != null) {
                printRawOutputPage(measurement.getRawOutput());
            }

            doc.save(outputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new TelekitException(t(MGG_UNABLE_TO_SAVE_DATA_TO_FILE), e);
        } finally {
            closeDoc();
        }
    }

    private PdfFont loadFont(InputStream stream, float fontSize) throws IOException {
        return new PdfFont(PDType0Font.load(doc, stream), fontSize);
    }

    private void closeDoc() {
        try {
            doc.close();
        } catch (IOException e) {
            throw new TelekitException(t(MGG_UNABLE_TO_SAVE_DATA_TO_FILE), e);
        }
    }

    private void printMainPage(MeasurementTask task) throws IOException {
        PDRectangle pageRect = PDRectangle.A4;
        PDPage page = new PDPage(pageRect);
        doc.addPage(page);

        float x = pageRect.getLowerLeftX() + MARGIN_X;
        float y = pageRect.getUpperRightY() - MARGIN_Y;

        final Measurement measurement = Objects.requireNonNull(task.getResult());
        final @Nullable Equipment eq = providerRegistry.findEquipmentById(task.getProvider());

        try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {

            String title = t(LINETEST_MEASUREMENT_RESULT);
            headerFont.applyToStream(stream);
            float titleHeight = headerFont.getFontHeight();
            float titleX = (page.getMediaBox().getWidth() - headerFont.getStringWidth(title)) / 2;

            stream.beginText();
            stream.newLineAtOffset(titleX, y);
            stream.showText(title);
            stream.endText();

            // PROPERTIES

            stream.beginText();
            stream.newLineAtOffset(x, y - titleHeight - PARAGRAPH_GAP);
            normalFont.applyToStream(stream);

            printTaskProperty(stream, t(DATE), task.getFormattedDateTime());
            printTaskProperty(stream, t(LINETEST_EQUIPMENT_MODEL), eq != null ? eq.printInOneLine() : "");
            printTaskProperty(stream, "URL", task.getConnectionParams().toUri().toString());
            printTaskProperty(stream, t(LINETEST_LINE_ID), task.getLine());
            printTaskProperty(stream, t(DURATION), task.getDuration() + " " + t(LINETEST_SECONDS));
            printTaskProperty(stream, t(LINETEST_LINE_STATUS), measurement.getLineStatus().getTitle());

            // MEASURED VALUES

            stream.newLineAtOffset(0, -PARAGRAPH_GAP);
            monospaceFont.applyToStream(stream);

            MeasuredValue resistance = measurement.getMeasuredValue(RESISTANCE);
            if (resistance != null) {
                printMeasuredValue(stream, "Rab:", format(RESISTANCE, resistance.getTipRing()));
                printMeasuredValue(stream, "Rag:", format(RESISTANCE, resistance.getRingGround()));
                printMeasuredValue(stream, "Rbg:", format(RESISTANCE, resistance.getTipGround()));
            }

            MeasuredValue capacitance = measurement.getMeasuredValue(CAPACITANCE);
            if (capacitance != null) {
                printMeasuredValue(stream, "Cab:", format(CAPACITANCE, capacitance.getTipRing()));
                printMeasuredValue(stream, "Cag:", format(CAPACITANCE, capacitance.getRingGround()));
                printMeasuredValue(stream, "Cbg:", format(CAPACITANCE, capacitance.getTipGround()));
            }

            MeasuredValue acVoltage = measurement.getMeasuredValue(AC_VOLTAGE);
            if (acVoltage != null) {
                printMeasuredValue(stream, "Uab~:", format(AC_VOLTAGE, acVoltage.getTipRing()));
                printMeasuredValue(stream, "Uag~:", format(AC_VOLTAGE, acVoltage.getRingGround()));
                printMeasuredValue(stream, "Ubg~:", format(AC_VOLTAGE, acVoltage.getTipGround()));
            }

            MeasuredValue dcVoltage = measurement.getMeasuredValue(DC_VOLTAGE);
            if (dcVoltage != null) {
                printMeasuredValue(stream, "Uab=:", format(DC_VOLTAGE, dcVoltage.getTipRing()));
                printMeasuredValue(stream, "Uag=:", format(DC_VOLTAGE, dcVoltage.getRingGround()));
                printMeasuredValue(stream, "Ubg=:", format(DC_VOLTAGE, dcVoltage.getTipGround()));
            }

            stream.endText();
        }
    }

    private void printTaskProperty(PDPageContentStream stream, String key, String value) throws IOException {
        stream.showText(key + ": ");
        stream.showText(value);
        stream.newLine();
    }

    private void printMeasuredValue(PDPageContentStream stream, String key, String value) throws IOException {
        stream.showText(rightPad(key, MEASURED_VALUE_PAD) + value);
        stream.newLine();
    }

    private void printRawOutputPage(String text) throws IOException {
        String[] lines = text.split(LineSeparator.LINE_SPLIT_PATTERN, -1);
        String longestLine = "";
        int lineCount = lines.length;
        for (String line : lines) {
            if (line.length() > longestLine.length()) {
                longestLine = line;
            }
        }

        float titleHeight = headerFont.getFontHeight();
        float contentHeight = (monospaceFont.getFontHeight() * monospaceFont.getLineSpacing()) * lineCount;
        float approxWidth = monospaceFont.getStringWidth(longestLine);
        float approxHeight = titleHeight + PARAGRAPH_GAP + contentHeight;

        PDRectangle pageRect = new PDRectangle(
                Math.max(PDRectangle.A4.getWidth(), approxWidth + MARGIN_X * 2),
                Math.max(PDRectangle.A4.getHeight(), approxHeight + MARGIN_Y * 2)
        );
        PDPage page = new PDPage(pageRect);
        doc.addPage(page);

        float x = pageRect.getLowerLeftX() + MARGIN_X;
        float y = pageRect.getUpperRightY() - MARGIN_Y;

        try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {

            String title = t(LINETEST_RAW_OUTPUT);
            headerFont.applyToStream(stream);
            float titleX = (page.getMediaBox().getWidth() - headerFont.getStringWidth(title)) / 2;

            stream.beginText();
            stream.newLineAtOffset(titleX, y);
            stream.showText(title);
            stream.endText();

            // CONTENT

            stream.beginText();
            stream.newLineAtOffset(x, y - titleHeight - PARAGRAPH_GAP);
            monospaceFont.applyToStream(stream);

            for (String line : lines) {
                stream.showText(line);
                stream.newLine();
            }

            stream.endText();
        }
    }
}
