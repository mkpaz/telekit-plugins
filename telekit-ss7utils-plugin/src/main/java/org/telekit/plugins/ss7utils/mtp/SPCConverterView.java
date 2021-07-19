package org.telekit.plugins.ss7utils.mtp;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.kordamp.ikonli.material2.Material2MZ;
import org.telekit.base.desktop.Component;
import org.telekit.base.di.Initializable;
import org.telekit.controls.util.BindUtils;
import org.telekit.controls.util.Controls;
import org.telekit.controls.util.TableUtils;
import org.telekit.plugins.ss7utils.InvalidInputException;
import org.telekit.plugins.ss7utils.i18n.SS7UtilsMessages;
import org.telekit.plugins.ss7utils.mtp.SignallingPointCode.Format;
import org.telekit.plugins.ss7utils.mtp.SignallingPointCode.Type;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static javafx.collections.FXCollections.emptyObservableList;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;
import static org.apache.commons.lang3.StringUtils.*;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.controls.i18n.ControlsMessages.*;
import static org.telekit.controls.util.Containers.horizontalSpacer;
import static org.telekit.controls.util.Containers.verticalSpacer;
import static org.telekit.controls.util.Controls.menuItem;
import static org.telekit.controls.util.TableUtils.setColumnConstraints;

@Singleton
public class SPCConverterView extends HBox implements Initializable, Component {

    static final int NAME_PADDING = 16;
    static final double PREF_WIDTH = 400;

    ComboBox<Type> typeChoice;
    TextField spcText;
    ComboBox<Format> formatChoice;
    TableView<Pair<String, String>> resultTable;
    Button convertBtn;

    @Inject
    public SPCConverterView() {
        createView();
    }

    private void createView() {
        typeChoice = new ComboBox<>();

        spcText = new TextField();
        HBox.setHgrow(spcText, Priority.ALWAYS);

        formatChoice = new ComboBox<>();
        formatChoice.setButtonCell(new SPCFormatCell());
        formatChoice.setCellFactory(property -> new SPCFormatCell());

        resultTable = createResultTable();
        resultTable.setPrefHeight(200);

        HBox spcBox = new HBox();
        spcBox.setSpacing(0);
        spcBox.setAlignment(Pos.CENTER_LEFT);
        spcBox.getChildren().addAll(typeChoice, spcText, formatChoice);

        convertBtn = Controls.create(() -> new Button(t(SS7UtilsMessages.ACTION_UPDATE)), "large");
        convertBtn.setGraphic(Controls.fontIcon(Material2MZ.REFRESH));
        convertBtn.setOnAction(e -> convert());

        VBox content = new VBox();
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(5);
        content.getChildren().addAll(
                verticalSpacer(),
                new Label(t(SS7UtilsMessages.SS7UTILS_SIGNALLING_POINT_CODE)),
                spcBox,
                resultTable,
                convertBtn,
                verticalSpacer()
        );

        getChildren().addAll(
                horizontalSpacer(),
                content,
                horizontalSpacer()
        );
        setPadding(new Insets(10));
        setId("spc-converter");
    }

    private TableView<Pair<String, String>> createResultTable() {
        TableColumn<Pair<String, String>, Integer> keyColumn = TableUtils.createColumn(t(FORMAT), "key");
        setColumnConstraints(keyColumn, 100, USE_COMPUTED_SIZE, false, CENTER_LEFT);

        TableColumn<Pair<String, String>, String> valueColumn = TableUtils.createColumn(t(VALUE), "value");
        setColumnConstraints(valueColumn, 200, USE_COMPUTED_SIZE, false, CENTER_LEFT);

        TableView<Pair<String, String>> table = new TableView<>();
        table.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().setAll(List.of(keyColumn, valueColumn));

        // COPY DATA

        Function<Pair<String, String>, String> rowToString = Pair::getValue;

        ContextMenu contextMenu = new ContextMenu();
        table.setContextMenu(contextMenu);
        contextMenu.getItems().add(
                menuItem(t(ACTION_COPY), null, e -> TableUtils.copySelectedRowsToClipboard(table, rowToString))
        );

        table.setOnKeyPressed(e -> {
            if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY).match(e)) {
                TableUtils.copySelectedRowsToClipboard(table, rowToString);
            }
        });

        return table;
    }

    @Override
    public void initialize() {
        typeChoice.getItems().addAll(Type.values());
        typeChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) { onTypeChanged(value); }
        });
        typeChoice.getSelectionModel().select(Type.ITU);

        spcText.setOnKeyPressed(keyCode -> {
            if (keyCode.getCode().equals(KeyCode.ENTER) && isNotBlank(spcText.getText())) {
                convert();
            }
        });

        convertBtn.disableProperty().bind(BindUtils.isBlank(spcText.textProperty()));

        formatChoice.getItems().setAll(Type.ITU.formats());
        formatChoice.getSelectionModel().selectFirst();
    }

    private void onTypeChanged(Type type) {
        formatChoice.getItems().setAll(type.formats());
        formatChoice.getSelectionModel().selectFirst();
    }

    private void convert() {
        String spcStr = trim(spcText.getText());
        Type type = typeChoice.getSelectionModel().getSelectedItem();
        Format fmt = formatChoice.getSelectionModel().getSelectedItem();

        if (isEmpty(spcStr) || fmt == null) { return; }

        try {
            SignallingPointCode spc = SignallingPointCode.parse(spcStr, type, fmt);
            updateResult(spc);
        } catch (InvalidInputException e) {
            resultTable.setItems(emptyObservableList());
        }
    }

    private void updateResult(SignallingPointCode spc) {
        List<Pair<String, String>> result = new ArrayList<>();
        result.add(ImmutablePair.of("DEC", spc.toString(Format.DEC)));
        result.add(ImmutablePair.of("HEX", spc.toString(Format.HEX)));
        result.add(ImmutablePair.of("BIN", spc.toString(Format.BIN)));

        if (spc.getLength() == Type.ITU.getBitLength()) {
            result.add(ImmutablePair.of("ITU [3-8-3]", spc.toString(Format.STRUCT_383)));
            result.add(ImmutablePair.of("RUS [8-6]", spc.toString(Format.STRUCT_86)));
        }

        if (spc.getLength() == Type.ANSI.getBitLength()) {
            result.add(ImmutablePair.of("ANSI [8-8-8", spc.toString(Format.STRUCT_888)));
        }

        resultTable.setItems(FXCollections.observableArrayList(result));
    }

    private static String pad(String name) {
        return rightPad(name, NAME_PADDING);
    }

    @Override
    public Region getRoot() { return this; }

    @Override
    public void reset() {}

    ///////////////////////////////////////////////////////////////////////////

    private static class SPCFormatCell extends ListCell<Format> {

        @Override
        protected void updateItem(Format format, boolean empty) {
            super.updateItem(format, empty);

            if (format != null) {
                setText(format.getDescription());
            } else {
                setText(null);
            }
        }
    }
}
