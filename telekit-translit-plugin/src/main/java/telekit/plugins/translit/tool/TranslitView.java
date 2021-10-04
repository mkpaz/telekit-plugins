package telekit.plugins.translit.tool;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.material2.Material2AL;
import telekit.base.desktop.Component;
import telekit.base.di.Initializable;
import telekit.controls.util.BindUtils;
import telekit.controls.util.Controls;
import telekit.plugins.translit.i18n.TranslitMessages;

import javax.inject.Singleton;
import java.util.List;

import static javafx.geometry.Pos.CENTER_LEFT;
import static org.apache.commons.lang3.StringUtils.trim;
import static telekit.base.i18n.I18n.t;
import static telekit.controls.util.Containers.*;

@Singleton
public class TranslitView extends GridPane implements Initializable, Component {

    static final List<String> SUPPORTED_LANGUAGES = List.of("RU");

    public ComboBox<String> langChoice;
    public Button runBtn;
    public TextArea origText;
    public TextArea resultText;

    public TranslitView() {
        createView();
    }

    private void createView() {
        langChoice = new ComboBox<>();

        runBtn = Controls.create(() -> new Button(t(TranslitMessages.TRANSLIT_TRANSLITERATE)), "large");
        runBtn.setGraphic(Controls.fontIcon(Material2AL.LANGUAGE));
        runBtn.setOnAction(e -> transliterate());

        HBox origBox = hbox(10, CENTER_LEFT, Insets.EMPTY);
        origBox.getChildren().addAll(
                new Label(TranslitMessages.TEXT),
                langChoice,
                horizontalSpacer(),
                runBtn
        );

        origText = new TextArea();
        origText.setWrapText(true);

        HBox resultBox = hbox(10, CENTER_LEFT, Insets.EMPTY);

        Label resultLabel = new Label(t(TranslitMessages.TRANSLIT_TRANSLITERATED_TEXT) + " [EN]");

        resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);

        add(origBox, 0, 0);
        add(origText, 0, 1);

        add(resultLabel, 1, 0);
        add(resultText, 1, 1);

        getRowConstraints().addAll(
                rowConstraints(Priority.NEVER),
                rowConstraints(Priority.ALWAYS)
        );

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(50);

        getColumnConstraints().addAll(columnConstraints, columnConstraints);

        setVgap(5);
        setHgap(10);
        setPadding(new Insets(10));
        setId("translit");
    }

    @Override
    public void initialize() {
        langChoice.setItems(FXCollections.observableArrayList(SUPPORTED_LANGUAGES));
        langChoice.getSelectionModel().selectFirst();
        runBtn.disableProperty().bind(BindUtils.isBlank(origText.textProperty()));
    }

    public void transliterate() {
        String lang = langChoice.getSelectionModel().getSelectedItem();
        String text = trim(origText.getText());

        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        Transliterator exec = switch (lang) {
            case "RU" -> new RUTransliterator();
            default -> null;
        };

        if (exec != null) {
            resultText.setText(exec.transliterate(text));
        }
    }

    @Override
    public Region getRoot() { return this; }

    @Override
    public void reset() {}

    @Override
    public @Nullable Node getPrimaryFocusNode() { return origText; }
}
