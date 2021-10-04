package telekit.plugins.linetest.tool;

import org.jetbrains.annotations.Nullable;
import telekit.base.domain.exception.TelekitException;
import telekit.base.net.connection.ConnectionParams;
import telekit.plugins.linetest.domain.Equipment;
import telekit.plugins.linetest.domain.MeasuredValue;
import telekit.plugins.linetest.domain.Measurement;
import telekit.plugins.linetest.domain.MeasurementTask;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static telekit.base.i18n.BaseMessages.MGG_UNABLE_TO_SAVE_DATA_TO_FILE;
import static telekit.base.i18n.I18n.t;
import static telekit.plugins.linetest.domain.MeasuredValue.ValueType;
import static telekit.plugins.linetest.domain.MeasuredValue.format;

final class MeasurementTaskHistoryExporter {

    private static final List<String> EMPTY_VALUE = List.of("", "", "");
    private static final String SEPARATOR = ",";

    private final ProviderRegistry providerRegistry;

    public MeasurementTaskHistoryExporter(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public void export(Collection<MeasurementTask> tasks, File outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, UTF_8);
             BufferedWriter out = new BufferedWriter(osw)) {

            int index = 1;
            String header = """
                    #Index
                    Status
                    Date
                    Equipment
                    Line
                    Connection URL
                    Duration (sec)
                    Line Status
                    Rab
                    Rag
                    Rbg
                    Cab
                    Cag
                    Cbg
                    Uab~
                    Uag~
                    Ubg~
                    Uab=
                    Uag=
                    Ubg=""".replaceAll("\n", SEPARATOR);

            out.write(header);
            out.write("\n");

            for (MeasurementTask task : tasks) {
                List<String> cols = new ArrayList<>();
                cols.add(String.valueOf(index));
                cols.add(!task.isFailed() ? "OK" : "FAIL");
                cols.add(task.getFormattedDateTime());

                Equipment eq = providerRegistry.findEquipmentById(task.getProvider());
                cols.add(eq != null ? eq.printInOneLine() : "");

                cols.add(task.getLine());

                ConnectionParams connectionParams = task.getConnectionParams();
                cols.add(connectionParams != null ? String.valueOf(connectionParams.toUri()) : "");

                cols.add(String.valueOf(task.getDuration()));

                Measurement measurement = task.getResult();
                cols.add(measurement != null ? measurement.getLineStatus().getTitle() : "");
                cols.addAll(getMeasuredValues(measurement, ValueType.RESISTANCE));
                cols.addAll(getMeasuredValues(measurement, ValueType.CAPACITANCE));
                cols.addAll(getMeasuredValues(measurement, ValueType.AC_VOLTAGE));
                cols.addAll(getMeasuredValues(measurement, ValueType.DC_VOLTAGE));

                String line = cols.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(SEPARATOR));
                out.write(line);
                out.write("\n");

                index++;
            }
        } catch (IOException e) {
            throw new TelekitException(t(MGG_UNABLE_TO_SAVE_DATA_TO_FILE), e);
        }
    }

    private List<String> getMeasuredValues(@Nullable Measurement measurement, ValueType valueType) {
        MeasuredValue value = measurement != null ? measurement.getMeasuredValue(valueType) : null;
        if (value != null) {
            return Arrays.asList(
                    format(valueType, value.getTipRing()),
                    format(valueType, value.getTipGround()),
                    format(valueType, value.getRingGround())
            );
        }

        return EMPTY_VALUE;
    }
}
