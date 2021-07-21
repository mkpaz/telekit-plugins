package org.telekit.plugins.linetest.tool;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import org.telekit.controls.util.Controls;
import org.telekit.plugins.linetest.domain.MeasurementTask;
import org.telekit.plugins.linetest.domain.MeasurementTask.Status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static javafx.geometry.Pos.CENTER;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.base.util.CommonUtils.hush;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.LINETEST_RUNNING;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.LINETEST_SECONDS;
import static org.telekit.plugins.linetest.tool.LinetestView.TASK_LAYER_MIN_WIDTH;

class RunningTaskLayer extends VBox {

    static final long MAX_TIMER_DURATION_IN_SECONDS = Duration.ofMinutes(10).toSeconds();

    Label taskDurationLabel;
    Timer taskProgressTimer;

    private final LinetestView view;

    public RunningTaskLayer(LinetestView view) {
        this.view = Objects.requireNonNull(view);
        createView();
    }

    private void createView() {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        taskDurationLabel = Controls.create(Label::new, "duration");

        setMinWidth(TASK_LAYER_MIN_WIDTH);
        setAlignment(CENTER);
        getStyleClass().add("task-layer");
        getChildren().setAll(
                Controls.create(() -> new Label(t(LINETEST_RUNNING)), "status"),
                progressIndicator,
                taskDurationLabel
        );
    }

    void updateSeconds(long seconds) {
        Platform.runLater(() -> taskDurationLabel.setText(seconds + " " + t(LINETEST_SECONDS)));
    }

    void update(MeasurementTask task) {
        Objects.requireNonNull(task);
        if (view.getViewModel().getTaskStatus(task) != Status.RUNNING) { return; }

        reset();
        taskProgressTimer = new Timer();

        taskProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long duration = ChronoUnit.SECONDS.between(task.getDateTime(), LocalDateTime.now());
                if (duration <= MAX_TIMER_DURATION_IN_SECONDS) {
                    updateSeconds(duration);
                } else {
                    cancel();
                }
            }
        }, 0, 1000);
    }

    void reset() {
        if (taskProgressTimer != null) {
            hush(() -> {
                taskProgressTimer.cancel();
                taskProgressTimer.purge();
            });
        }
    }

    void toFront(MeasurementTask task) {
        update(task);
        toFront();
    }
}