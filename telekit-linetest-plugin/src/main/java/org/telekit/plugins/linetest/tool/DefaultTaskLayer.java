package org.telekit.plugins.linetest.tool;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.telekit.controls.util.Controls;
import org.telekit.plugins.linetest.domain.Measurement;
import org.telekit.plugins.linetest.domain.MeasurementTask;
import org.telekit.plugins.linetest.domain.MeasurementTask.Status;

import java.util.Objects;

import static javafx.geometry.Pos.CENTER;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.controls.util.Containers.hbox;
import static org.telekit.controls.util.NodeUtils.toggleVisibility;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.*;
import static org.telekit.plugins.linetest.tool.LinetestView.TASK_LAYER_MIN_WIDTH;

class DefaultTaskLayer extends VBox {

    private HBox statusBox;
    private FontIcon statusIcon;
    private Label statusLabel;
    private TextArea errorText;
    private Button runButton;

    private final LinetestView view;

    public DefaultTaskLayer(LinetestView view) {
        this.view = Objects.requireNonNull(view);
        createView();
    }

    private void createView() {
        statusIcon = Controls.fontIcon(Material2OutlinedAL.BLOCK);
        statusLabel = new Label();
        statusBox = hbox(5, CENTER, Insets.EMPTY, "status");
        statusBox.getChildren().setAll(statusIcon, statusLabel);

        errorText = Controls.create(TextArea::new, "error", "no-focus-border", "monospace");
        errorText.setEditable(false);
        errorText.setMinWidth(200);
        errorText.setPrefWidth(500);
        errorText.setMaxWidth(500);

        runButton = Controls.button(t(LINETEST_RUN_TEST), Material2OutlinedMZ.PLAY_CIRCLE_OUTLINE, "large");
        runButton.setOnAction(e -> view.runSelectedTask());

        setMinWidth(TASK_LAYER_MIN_WIDTH);
        setAlignment(CENTER);
        getStyleClass().add("task-layer");
        getChildren().setAll(statusBox, errorText, runButton);
    }

    void update(@Nullable MeasurementTask task) {
        if (task == null) {
            statusIcon.setIconCode(Material2OutlinedAL.BLOCK);
            statusLabel.setText(t(NO_DATA));

            errorText.setText(null);
            toggleVisibility(errorText, false);

            runButton.setText(t(LINETEST_RUN_TEST));
            toggleVisibility(runButton, false);

            statusBox.pseudoClassStateChanged(LinetestView.ERROR, false);

            return;
        }

        Status status = view.getViewModel().getTaskStatus(task);

        if (status == Status.NEW) {
            statusIcon.setIconCode(Material2OutlinedAL.BLOCK);
            statusLabel.setText(t(NO_DATA));

            errorText.setText(null);
            toggleVisibility(errorText, false);

            runButton.setText(t(LINETEST_RUN_TEST));
            toggleVisibility(runButton, true);

            statusBox.pseudoClassStateChanged(LinetestView.ERROR, false);

            return;
        }

        if (status == Status.ERROR) {
            statusIcon.setIconCode(task.isConnectionFailed() ?
                    Material2OutlinedAL.LINK_OFF :
                    Material2OutlinedMZ.REPORT_PROBLEM
            );
            statusLabel.setText(task.isConnectionFailed() ?
                    t(LINETEST_CONNECTION_FAILED) :
                    t(LINETEST_TEST_FAILED)
            );

            if (task.isConnectionFailed()) {
                errorText.setText(t(LINETEST_ERROR_CONNECTION_FAILED, task.getHost()));
            }

            Measurement measurement = task.getResult();
            if (measurement != null && measurement.isTestFailed()) {
                errorText.setText(task.getResult().getRawOutput());
            }
            toggleVisibility(errorText, true);

            runButton.setText(t(LINETEST_TRY_AGAIN));
            toggleVisibility(runButton, true);

            statusBox.pseudoClassStateChanged(LinetestView.ERROR, true);
        }
    }

    void toFront(@Nullable MeasurementTask task) {
        update(task);
        toFront();
    }
}