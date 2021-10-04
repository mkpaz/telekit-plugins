package telekit.plugins.linetest.tool;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import telekit.base.desktop.Component;
import telekit.base.desktop.Overlay;
import telekit.base.desktop.mvvm.View;
import telekit.base.di.Initializable;
import telekit.base.domain.Action;
import telekit.controls.dialogs.Dialogs;
import telekit.controls.util.Controls;
import telekit.controls.util.NodeUtils;
import telekit.plugins.linetest.domain.Equipment;
import telekit.plugins.linetest.domain.MeasurementTask;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Path;

import static javafx.geometry.Pos.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static telekit.base.i18n.I18n.t;
import static telekit.base.util.FileSystemUtils.getParentPath;
import static telekit.base.util.FileSystemUtils.sanitizeFileName;
import static telekit.controls.util.Containers.*;
import static telekit.controls.util.Controls.fontIcon;
import static telekit.controls.util.NodeUtils.toggleVisibility;
import static telekit.plugins.linetest.domain.MeasurementTask.Status;
import static telekit.plugins.linetest.i18n.LinetestMessages.*;

@Singleton
public class LinetestView extends VBox implements Initializable, View<LinetestViewModel> {

    static final double SIDEBAR_WIDTH = 200;
    static final double HEADER_HEIGHT = 20;
    static final double TASK_LAYER_HEIGHT_BREAKPOINT = 400;
    static final double TASK_LAYER_MIN_WIDTH = 500;
    static final PseudoClass SUCCESS = PseudoClass.getPseudoClass("success");
    static final PseudoClass WARNING = PseudoClass.getPseudoClass("warning");
    static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    static final Color COLOR_SUCCESS = Color.rgb(46, 125, 50, 0.3);
    static final Color COLOR_WARNING = Color.rgb(221, 44, 0, 0.3);

    Button createTestBtn;
    Button phoneBookBtn;
    ListView<MeasurementTask> taskList;
    DefaultTaskLayer defaultTaskLayer;
    RunningTaskLayer runningTaskLayer;
    FinishedTaskLayer finishedTaskLayer;

    private final LinetestViewModel model;
    private final Overlay overlay;

    private MeasurementTaskDialog measurementTaskDialog;
    private PhoneBookDialog phoneBookDialog;
    private Path lastVisitedDirectory;

    @Inject
    public LinetestView(LinetestViewModel model, Overlay overlay) {
        this.model = model;
        this.overlay = overlay;

        createView();
    }

    private void createView() {
        // BOTTOM

        createTestBtn = Controls.create(() -> new Button(t(LINETEST_CREATE_NEW_TEST)), "large");
        createTestBtn.setGraphic(fontIcon(Material2OutlinedAL.ADD_BOX));
        createTestBtn.setOnAction(e -> showMeasurementTaskDialog(Action.ADD));

        phoneBookBtn = Controls.create(() -> new Button(t(LINETEST_PHONE_BOOK)), "large");
        phoneBookBtn.setGraphic(fontIcon(Material2OutlinedAL.BOOK));
        phoneBookBtn.setOnAction(e -> showPhoneBookDialog());

        HBox actionBox = hbox(10, CENTER, new Insets(10, 10, 0, 10), "action-box");
        VBox.setVgrow(actionBox, NEVER);
        actionBox.getChildren().setAll(createTestBtn, phoneBookBtn);

        // LEFT

        MenuItem loadTasksItem = new MenuItem(t(ACTION_REFRESH));
        loadTasksItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        loadTasksItem.setOnAction(e -> {
            model.loadTasksCommand().execute();
            NodeUtils.begForFocus(taskList, 3);
        });

        MenuItem historyExportItem = new MenuItem(t(HISTORY));
        historyExportItem.setOnAction(e -> exportTaskHistory());

        MenuButton taskListMenuBtn = Controls.create(MenuButton::new, "link-button");
        taskListMenuBtn.setGraphic(Controls.fontIcon(Material2MZ.MORE_VERT));
        taskListMenuBtn.setCursor(Cursor.HAND);
        taskListMenuBtn.getItems().setAll(loadTasksItem, historyExportItem);

        HBox leftHeaderBox = hbox(0, CENTER_LEFT, Insets.EMPTY);
        leftHeaderBox.getChildren().setAll(
                new Label(t(LINETEST_TASKS)),
                horizontalSpacer(),
                taskListMenuBtn
        );
        setFixedHeight(leftHeaderBox, HEADER_HEIGHT);

        taskList = createTaskList();
        VBox.setVgrow(taskList, ALWAYS);

        // Task list top should be aligned with finished task layer, but finished
        // task layer should be slightly less than other layers, otherwise you would
        // see the borders of its components, hence increased spacing.
        VBox leftBox = vbox(7, TOP_LEFT, new Insets(2, 2, 3, 2));
        leftBox.getChildren().setAll(
                leftHeaderBox,
                taskList
        );

        // RIGHT

        HBox rightHeaderBox = hbox(0, CENTER_LEFT, new Insets(0, 0, 0, 2));
        rightHeaderBox.getChildren().setAll(new Label(t(LINETEST_MEASUREMENT_RESULT)));
        setFixedHeight(rightHeaderBox, HEADER_HEIGHT);

        defaultTaskLayer = new DefaultTaskLayer(this);
        runningTaskLayer = new RunningTaskLayer(this);
        finishedTaskLayer = new FinishedTaskLayer(this);

        StackPane taskPane = create(StackPane::new, "task");
        taskPane.getChildren().setAll(defaultTaskLayer, runningTaskLayer, finishedTaskLayer);
        defaultTaskLayer.toFront(null);

        VBox rightBox = vbox(5, TOP_LEFT, new Insets(2));
        rightBox.getChildren().setAll(rightHeaderBox, taskPane);

        // ROOT

        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.3);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        HBox.setHgrow(splitPane, ALWAYS);
        SplitPane.setResizableWithParent(leftBox, false);
        splitPane.getItems().setAll(leftBox, rightBox);

        setAlignment(TOP_LEFT);
        setPadding(new Insets(10));
        getChildren().setAll(splitPane, actionBox);
        Component.propagateMouseEventsToParent(splitPane);
        setId("linetest");
    }

    private ListView<MeasurementTask> createTaskList() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem runItem = Controls.menuItem(t(ACTION_RUN), null, e -> runSelectedTask());
        runItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
        runItem.disableProperty().bind(selectedTaskHasStatus(Status.RUNNING, Status.FINISHED));

        MenuItem editItem = Controls.menuItem(t(ACTION_EDIT), null, e -> showMeasurementTaskDialog(Action.EDIT));
        editItem.disableProperty().bind(selectedTaskHasStatus(Status.RUNNING, Status.FINISHED));

        MenuItem duplicateItem = Controls.menuItem(t(ACTION_DUPLICATE), null, e -> showMeasurementTaskDialog(Action.DUPLICATE));
        duplicateItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
        duplicateItem.disableProperty().bind(selectedTaskHasStatus(Status.RUNNING));

        MenuItem deleteItem = Controls.menuItem(t(ACTION_DELETE), null, e -> model.removeTaskCommand().execute());
        deleteItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        deleteItem.disableProperty().bind(selectedTaskHasStatus(Status.RUNNING));

        contextMenu.getItems().setAll(
                runItem,
                new SeparatorMenuItem(),
                editItem,
                duplicateItem,
                deleteItem
        );

        ListView<MeasurementTask> list = Controls.create(ListView::new, "tasks");
        list.setMinWidth(SIDEBAR_WIDTH);
        list.setCellFactory(v -> new TaskListCell(model));
        list.contextMenuProperty().bind(
                Bindings.when(list.getSelectionModel().selectedItemProperty().isNull())
                        .then((ContextMenu) null)
                        .otherwise(contextMenu)
        );

        return list;
    }

    private BooleanBinding selectedTaskHasStatus(Status... values) {
        return Bindings.createBooleanBinding(() -> {
            MeasurementTask task = model.selectedTaskProperty().get();
            return task != null && model.taskHasStatus(task, values);
        }, model.selectedTaskProperty());
    }

    @Override
    public void initialize() {
        model.setTaskSelectionModel(taskList.getSelectionModel());
        taskList.setItems(model.sortedTasks());

        model.selectedTaskProperty().bind(taskList.getSelectionModel().selectedItemProperty());
        model.selectedTaskProperty().addListener((obs, old, value) -> updateDisplayedResult(value));

        finishedTaskLayer.heightProperty().addListener((obs, old, value) -> {
            if (value != null) { finishedTaskLayer.toggleGauges(value.doubleValue() > TASK_LAYER_HEIGHT_BREAKPOINT); }
        });

        setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.N) { showMeasurementTaskDialog(Action.ADD); }
        });

        taskList.getSelectionModel().selectFirst();
    }

    private void updateDisplayedResult(MeasurementTask task) {
        if (task == null || task.getResult() == null) {
            finishedTaskLayer.reset();
            runningTaskLayer.reset();
        }

        if (task == null) {
            defaultTaskLayer.toFront(null);
            return;
        }

        switch (model.getTaskStatus(task)) {
            case NEW, ERROR -> defaultTaskLayer.toFront(task);
            case RUNNING -> runningTaskLayer.toFront(task);
            case FINISHED -> finishedTaskLayer.toFront(task);
        }
    }

    private void showMeasurementTaskDialog(Action action) {
        MeasurementTaskDialog dialog = getOrCreateMeasurementTaskDialog();
        MeasurementTask task = action != Action.ADD ? model.selectedTaskProperty().get() : null;
        dialog.setData(action, task);
        overlay.show(dialog, HPos.CENTER);
    }

    private MeasurementTaskDialog getOrCreateMeasurementTaskDialog() {
        if (measurementTaskDialog != null) { return measurementTaskDialog; }

        measurementTaskDialog = new MeasurementTaskDialog(model.getProviderRegistry());
        measurementTaskDialog.setOnCommit((action, task) -> {
            switch (action) {
                case ADD, DUPLICATE -> model.addTaskCommand().execute(task);
                case EDIT -> model.updateTaskCommand().execute(task);
            }
            overlay.hide();
        });
        measurementTaskDialog.setOnCloseRequest(overlay::hide);

        return measurementTaskDialog;
    }

    private void showPhoneBookDialog() {
        phoneBookDialog = defaultIfNull(phoneBookDialog, new PhoneBookDialog(
                model.getPhoneBookService(),
                model.getProviderRegistry()
        ));
        phoneBookDialog.setOnCommit(phoneBookEntry -> {
            MeasurementTaskDialog measurementTaskDialog = getOrCreateMeasurementTaskDialog();
            measurementTaskDialog.setData(Action.ADD, new MeasurementTask(phoneBookEntry.getRequest()));
            overlay.show(measurementTaskDialog);
        });
        phoneBookDialog.setOnCloseRequest(overlay::hide);
        overlay.show(phoneBookDialog);
    }

    void runSelectedTask() {
        MeasurementTask task = model.selectedTaskProperty().get();
        if (task != null) { model.runTaskCommand().execute(task); }
    }

    @Override
    public Region getRoot() { return this; }

    @Override
    public void reset() {}

    @Override
    public LinetestViewModel getViewModel() { return model; }

    @Override
    public @Nullable Node getPrimaryFocusNode() { return taskList; }

    Path getLastVisitedDirectory() {
        return lastVisitedDirectory;
    }

    void setLastVisitedDirectory(Path lastVisitedDirectory) {
        this.lastVisitedDirectory = lastVisitedDirectory;
    }

    private void exportTaskHistory() {
        String filename = "linetest_history.csv";
        File outputFile = Dialogs.fileChooser()
                .addFilter(t(FILE_DIALOG_CSV), "*.csv")
                .initialDirectory(lastVisitedDirectory)
                .initialFileName(sanitizeFileName(filename))
                .build()
                .showSaveDialog(getWindow());
        if (outputFile == null) { return; }

        lastVisitedDirectory = getParentPath(outputFile);
        model.exportHistoryCommand().execute(outputFile);
    }

    ///////////////////////////////////////////////////////////////////////////

    static class TaskListCell extends ListCell<MeasurementTask> {

        private FontIcon statusIcon;
        private Label statusIconLabel;
        private Label equipmentLabel;
        private Label addressLabel;
        private Label statusTextLabel;
        private HBox root;

        private final LinetestViewModel model;

        public TaskListCell(LinetestViewModel model) {
            this.model = model;
            createView();
        }

        private void createView() {
            statusIcon = fontIcon(Material2OutlinedMZ.PAUSE_CIRCLE_OUTLINE, "status");

            statusIconLabel = new Label();
            statusIconLabel.setGraphic(statusIcon);

            equipmentLabel = Controls.create(Label::new, "equipment");

            addressLabel = Controls.create(Label::new, "address");

            statusTextLabel = Controls.create(Label::new, "data");

            VBox dataBox = vbox(2, CENTER_LEFT, Insets.EMPTY);
            dataBox.getChildren().setAll(equipmentLabel, addressLabel, statusTextLabel);

            root = hbox(10, CENTER_LEFT, Insets.EMPTY, "task-list-cell");
            root.getChildren().setAll(statusIcon, dataBox);
        }

        @Override
        protected void updateItem(MeasurementTask task, boolean empty) {
            super.updateItem(task, empty);

            if (empty) {
                statusIconLabel.setGraphic(null);
                equipmentLabel.setText(null);
                addressLabel.setText(null);
                statusTextLabel.setText(null);
                setGraphic(null);
                return;
            }

            Status taskStatus = model.getTaskStatus(task);
            switch (taskStatus) {
                case NEW -> {
                    statusIcon.setIconCode(Material2OutlinedMZ.PAUSE_CIRCLE_OUTLINE);
                    statusTextLabel.setText(null);
                    toggleVisibility(statusTextLabel, false);
                }
                case RUNNING -> {
                    statusIcon.setIconCode(Material2OutlinedMZ.PENDING);
                    statusTextLabel.setText(t(LINETEST_RUNNING));
                    toggleVisibility(statusTextLabel, true);
                }
                case FINISHED -> {
                    statusIcon.setIconCode(Material2OutlinedMZ.VERIFIED);
                    statusTextLabel.setText(task.getFormattedDateTime());
                    toggleVisibility(statusTextLabel, true);
                }
                case ERROR -> {
                    statusIcon.setIconCode(task.isConnectionFailed() ?
                            Material2OutlinedAL.LINK_OFF :
                            Material2OutlinedMZ.REPORT_PROBLEM
                    );
                    statusTextLabel.setText(task.getFormattedDateTime());
                    toggleVisibility(statusTextLabel, true);
                }
            }

            statusIconLabel.setGraphic(statusIcon);

            statusIcon.pseudoClassStateChanged(SUCCESS, taskStatus == Status.FINISHED);
            statusIcon.pseudoClassStateChanged(ERROR, taskStatus == Status.ERROR);

            Equipment eq = model.getProviderRegistry().findEquipmentById(task.getProvider());
            equipmentLabel.setText(eq != null ? eq.getVendor() + " " + eq.getModel() : t(LINETEST_UNKNOWN_EQUIPMENT));

            String address = String.format("host=%s, line=%s", task.getConnectionParams().getHost(), task.getLine());
            addressLabel.setText(address);

            setGraphic(root);
        }
    }
}
