package org.telekit.plugins.ss7utils.mtp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.material2.Material2MZ;
import org.telekit.base.desktop.Component;
import org.telekit.base.di.Initializable;
import org.telekit.base.domain.exception.InvalidInputException;
import org.telekit.base.util.TextBuilder;
import org.telekit.controls.util.BindUtils;
import org.telekit.controls.util.Controls;
import org.telekit.plugins.ss7utils.i18n.SS7UtilsMessages;
import org.telekit.plugins.ss7utils.mtp.SignallingPointCode.Format;
import org.telekit.plugins.ss7utils.mtp.SignallingPointCode.Type;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.apache.commons.lang3.StringUtils.*;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.controls.util.Containers.*;

@Singleton
public class SPCConverterView extends HBox implements Initializable, Component {

    static final int NAME_PADDING = 16;
    static final double PREF_WIDTH = 400;

    ComboBox<Type> typeChoice;
    TextField spcText;
    ComboBox<Format> formatChoice;
    TextArea result;
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

        result = Controls.create(TextArea::new, "monospace");

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
                result,
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
            result.setText(t(SS7UtilsMessages.SS7UTILS_MSG_INVALID_POINT_CODE));
        }
    }

    private void updateResult(SignallingPointCode spc) {
        TextBuilder text = new TextBuilder();

        text.appendLine(pad("DEC:"), spc.toString(Format.DEC));
        text.appendLine(pad("HEX:"), spc.toString(Format.HEX));
        text.appendLine(pad("BIN:"), spc.toString(Format.BIN));

        if (spc.getLength() == Type.ITU.getBitLength()) {
            text.appendLine(pad("ITU [3-8-3]:"), spc.toString(Format.STRUCT_383));
            text.appendLine(pad("RUS [8-6]:"), spc.toString(Format.STRUCT_86));
        }

        if (spc.getLength() == Type.ANSI.getBitLength()) {
            text.appendLine(pad("ANSI [8-8-8]:"), spc.toString(Format.STRUCT_888));
        }

        result.setText(text.toString());
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
