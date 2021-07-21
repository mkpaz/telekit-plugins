package org.telekit.plugins.linetest.tool;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.SectionBuilder;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.telekit.controls.dialogs.Dialogs;
import org.telekit.controls.util.Controls;
import org.telekit.controls.util.NodeUtils;
import org.telekit.controls.util.TableUtils;
import org.telekit.plugins.linetest.domain.LineStatus;
import org.telekit.plugins.linetest.domain.MeasuredValue;
import org.telekit.plugins.linetest.domain.Measurement;
import org.telekit.plugins.linetest.domain.MeasurementTask;
import org.telekit.plugins.linetest.domain.MeasurementTask.Status;
import org.telekit.plugins.linetest.i18n.LinetestMessages;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static javafx.geometry.Pos.*;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.SOMETIMES;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.base.util.FileSystemUtils.getParentPath;
import static org.telekit.base.util.FileSystemUtils.sanitizeFileName;
import static org.telekit.controls.util.Containers.*;
import static org.telekit.controls.util.TableUtils.setColumnConstraints;
import static org.telekit.plugins.linetest.domain.MeasuredValue.ValueType;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.*;
import static org.telekit.plugins.linetest.tool.LinetestView.ERROR;
import static org.telekit.plugins.linetest.tool.LinetestView.WARNING;
import static org.telekit.plugins.linetest.tool.LinetestView.*;

class FinishedTaskLayer extends VBox {

    private static final String TIP_RING_COL_ID = "measuredValueTipRingCol";
    private static final String TIP_GROUND_COL_ID = "measuredValueTipGroundCol";
    private static final String RING_GROUND_COL_ID = "measuredValueRingGroundCol";

    TableView<MeasuredValue> measuredValuesTable;
    Label lineStatusLabel;
    HBox gaugeBox;
    Gauge resistanceGauge;
    Gauge capacitanceGauge;
    TextArea logText;

    private final LinetestView view;
    private final ThresholdHelper thresholdHelper;

    public FinishedTaskLayer(LinetestView view) {
        this.view = Objects.requireNonNull(view);

        ProviderRegistry providerRegistry = view.getViewModel().getProviderRegistry();
        this.thresholdHelper = new ThresholdHelper(providerRegistry);

        createView();
    }

    private void createView() {
        measuredValuesTable = createMeasuredValuesTable();
        measuredValuesTable.setPrefHeight(300);
        measuredValuesTable.setMinWidth(TASK_LAYER_MIN_WIDTH);
        VBox.setVgrow(measuredValuesTable, SOMETIMES);

        lineStatusLabel = new Label(LineStatus.UNKNOWN.name());

        Button saveButton = new Button(t(ACTION_SAVE));
        saveButton.setOnAction(e -> printTask());

        HBox lineStatusBox = hbox(5, CENTER_LEFT, Insets.EMPTY);
        lineStatusBox.getChildren().setAll(
                new Label(t(LINETEST_LINE_STATUS) + " :"),
                lineStatusLabel,
                horizontalSpacer(),
                saveButton
        );

        resistanceGauge = createResistanceGauge();
        capacitanceGauge = createCapacitanceGauge();

        gaugeBox = hbox(20, CENTER, new Insets(10), "gauge-box");
        setFixedHeight(gaugeBox, 160);
        gaugeBox.setMinWidth(TASK_LAYER_MIN_WIDTH);
        gaugeBox.getChildren().setAll(resistanceGauge, capacitanceGauge);

        logText = Controls.create(TextArea::new, "log", "monospace", "no-focus-border");
        logText.setEditable(false);
        VBox.setVgrow(logText, ALWAYS);

        setMinWidth(TASK_LAYER_MIN_WIDTH);
        setAlignment(TOP_LEFT);
        setPadding(new Insets(2));
        getStyleClass().add("task-layer");
        getChildren().setAll(
                measuredValuesTable,
                lineStatusBox,
                gaugeBox,
                logText
        );
    }

    private TableView<MeasuredValue> createMeasuredValuesTable() {
        TableColumn<MeasuredValue, ValueType> valueTypeColumn = TableUtils.createColumn("", "valueType");
        valueTypeColumn.setSortable(false);
        valueTypeColumn.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ValueType valueType, boolean empty) {
                super.updateItem(valueType, empty);
                setText(!empty ? valueType.getTitle() : null);
            }
        });
        setColumnConstraints(valueTypeColumn, 120, USE_COMPUTED_SIZE, true, CENTER_LEFT);

        TableColumn<MeasuredValue, ValueType> statusColumn = TableUtils.createColumn(t(STATUS), "valueType");
        statusColumn.setSortable(false);
        statusColumn.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ValueType valueType, boolean empty) {
                super.updateItem(valueType, empty);

                final MeasuredValue measuredValue = getTableRow().getItem();
                if (empty || measuredValue == null) {
                    setText(null);
                    return;
                }

                final int maxErrorLevel = thresholdHelper.getMaxErrorLevel(measuredValue);
                switch (maxErrorLevel) {
                    case ThresholdHelper.NO_ERROR -> setText("ok");
                    case ThresholdHelper.ERROR_LEVEL_WARN -> setText(t(LinetestMessages.WARNING).toLowerCase());
                    case ThresholdHelper.ERROR_LEVEL_CRIT -> setText(t(LinetestMessages.ERROR).toLowerCase());
                    default -> setText(null);
                }
            }
        });
        setColumnConstraints(statusColumn, 120, USE_COMPUTED_SIZE, false, CENTER);

        Callback<TableColumn<MeasuredValue, Double>, TableCell<MeasuredValue, Double>> measuredValueCellFactory =
                x -> new MeasuredValueTableCell(thresholdHelper);

        TableColumn<MeasuredValue, Double> tipRingColumn = TableUtils.createColumn(t(LINETEST_TIP_RING), "tipRing");
        tipRingColumn.setId(TIP_RING_COL_ID);
        tipRingColumn.setSortable(false);
        tipRingColumn.setCellFactory(measuredValueCellFactory);
        setColumnConstraints(tipRingColumn, 120, USE_COMPUTED_SIZE, false, Pos.CENTER);

        TableColumn<MeasuredValue, Double> tipGroundColumn = TableUtils.createColumn(t(LINETEST_TIP_GROUND), "tipGround");
        tipGroundColumn.setId(TIP_GROUND_COL_ID);
        tipGroundColumn.setSortable(false);
        tipGroundColumn.setCellFactory(measuredValueCellFactory);
        setColumnConstraints(tipGroundColumn, 120, USE_COMPUTED_SIZE, false, Pos.CENTER);

        TableColumn<MeasuredValue, Double> ringGroundColumn = TableUtils.createColumn(t(LINETEST_RING_GROUND), "ringGround");
        ringGroundColumn.setId(RING_GROUND_COL_ID);
        ringGroundColumn.setSortable(false);
        ringGroundColumn.setCellFactory(measuredValueCellFactory);
        setColumnConstraints(ringGroundColumn, 120, USE_COMPUTED_SIZE, false, Pos.CENTER);

        TableView<MeasuredValue> table = Controls.create(TableView::new, "measured-values-table");
        table.setSelectionModel(null);
        table.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        table.getColumns().setAll(List.of(
                valueTypeColumn,
                statusColumn,
                tipRingColumn,
                tipGroundColumn,
                ringGroundColumn
        ));

        return table;
    }

    private Gauge createResistanceGauge() {
        return GaugeBuilder.create()
                .skinType(Gauge.SkinType.HORIZONTAL)
                .value(10)
                .title("Rab")
                .unit("MOhm")
                .minorTickSpace(0.2)
                .majorTickSpace(1)
                .maxValue(5)
                .knobType(Gauge.KnobType.FLAT)
                .needleSize(Gauge.NeedleSize.THIN)
                .sections(
                        SectionBuilder.create().start(0).stop(2).color(COLOR_WARNING).build(),
                        SectionBuilder.create().start(2).stop(5).color(COLOR_SUCCESS).build()
                )
                .sectionsVisible(true)
                .build();
    }

    private Gauge createCapacitanceGauge() {
        return GaugeBuilder.create()
                .skinType(Gauge.SkinType.HORIZONTAL)
                .title("Cab")
                .unit("uF")
                .minorTickSpace(0.1)
                .majorTickSpace(0.5)
                .tickLabelDecimals(1)
                .decimals(2)
                .maxValue(2)
                .knobType(Gauge.KnobType.FLAT)
                .needleSize(Gauge.NeedleSize.THIN)
                .sections(
                        SectionBuilder.create().start(0).stop(0.1).color(COLOR_WARNING).build(),
                        SectionBuilder.create().start(0.1).stop(1.5).color(COLOR_SUCCESS).build(),
                        SectionBuilder.create().start(1.5).stop(2).color(COLOR_WARNING).build()
                )
                .sectionsVisible(true)
                .build();
    }

    void update(MeasurementTask task) {
        Objects.requireNonNull(task);
        if (view.getViewModel().getTaskStatus(task) != Status.FINISHED) { return; }

        thresholdHelper.update(task);

        Measurement measurement = Objects.requireNonNull(task.getResult());
        measuredValuesTable.setItems(FXCollections.observableArrayList(measurement.getMeasuredValuesAsList()));

        MeasuredValue resistance = measurement.getMeasuredValue(ValueType.RESISTANCE);
        if (resistance != null) { resistanceGauge.setValue(resistance.getTipRing() / 1e6); } // KOhm

        MeasuredValue capacitance = measurement.getMeasuredValue(ValueType.CAPACITANCE);
        if (capacitance != null) { capacitanceGauge.setValue(capacitance.getTipRing() * 1e6); } // uF

        lineStatusLabel.setText(measurement.getLineStatus().getTitle());

        logText.setText(measurement.getRawOutput());
    }

    void reset() {
        measuredValuesTable.setItems(FXCollections.emptyObservableList());
        lineStatusLabel.setText(null);
        resistanceGauge.setValue(0);
        capacitanceGauge.setValue(0);
        logText.setText(null);
    }

    void toFront(MeasurementTask task) {
        update(task);
        toFront();
    }

    void toggleGauges(boolean visible) {
        NodeUtils.toggleVisibility(gaugeBox, visible);
    }

    private void printTask() {
        String filename = "linetest_result.pdf";
        File outputFile = Dialogs.fileChooser()
                .addFilter(t(FILE_DIALOG_PDF), "*.pdf")
                .initialDirectory(view.getLastVisitedDirectory())
                .initialFileName(sanitizeFileName(filename))
                .build()
                .showSaveDialog(view.getWindow());
        if (outputFile == null) { return; }

        view.setLastVisitedDirectory(getParentPath(outputFile));
        view.getViewModel().printTaskCommand().execute(outputFile);
    }

    ///////////////////////////////////////////////////////////////////////////

    static class MeasuredValueTableCell extends TableCell<MeasuredValue, Double> {

        private final ThresholdHelper helper;

        public MeasuredValueTableCell(ThresholdHelper helper) {
            this.helper = helper;
        }

        @Override
        protected void updateItem(Double cellValue, boolean empty) {
            super.updateItem(cellValue, empty);

            final MeasuredValue measuredValue = getTableRow().getItem();
            if (empty || measuredValue == null) {
                setText(null);
                return;
            }

            final ValueType type = measuredValue.getValueType();
            final boolean tipRing = TIP_RING_COL_ID.equals(getTableColumn().getId());

            setText(MeasuredValue.format(type, cellValue));

            pseudoClassStateChanged(ERROR, helper.checkCrit(type, cellValue, tipRing));
            pseudoClassStateChanged(WARNING, helper.checkWarn(type, cellValue, tipRing));
        }
    }
}
