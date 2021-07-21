package org.telekit.plugins.linetest.tool;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.telekit.base.desktop.mvvm.*;
import org.telekit.base.di.Initializable;
import org.telekit.base.domain.event.Notification;
import org.telekit.base.domain.event.TaskProgressEvent;
import org.telekit.base.event.DefaultEventBus;
import org.telekit.base.preferences.SharedPreferences;
import org.telekit.base.service.crypto.EncryptionService;
import org.telekit.controls.util.Promise;
import org.telekit.controls.util.TransformationListHandle;
import org.telekit.controls.util.UnconditionalObjectProperty;
import org.telekit.plugins.linetest.database.JdbcStore;
import org.telekit.plugins.linetest.database.MeasurementTaskRepository;
import org.telekit.plugins.linetest.domain.LinetestRequest;
import org.telekit.plugins.linetest.domain.Measurement;
import org.telekit.plugins.linetest.domain.MeasurementTask;
import org.telekit.plugins.linetest.provider.LinetestProvider;
import org.telekit.plugins.linetest.provider.LinetestSession;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static org.telekit.base.Env.MASTER_ENC_SERVICE_QUALIFIER;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.plugins.linetest.domain.MeasurementTask.Status;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.LINETEST_ERROR_HOST_ALREADY_IN_USE;

@Singleton
public class LinetestViewModel implements Initializable, ViewModel {

    private static final Logger LOG = Logger.getLogger(LinetestViewModel.class.getName());
    private static final int TASK_LIST_LIMIT = 200;

    //private final Map<String, LinetestProvider> providerRegistry = new TreeMap<>();
    private final MeasurementTaskRepository taskRepository;
    private final ExecutorService threadPool;
    private final SharedPreferences sharedPreferences;
    private final ProviderRegistry providerRegistry;
    private final RunningTaskRegistry runningTaskRegistry;
    private final PhoneBookService phoneBookService;

    @Inject
    public LinetestViewModel(JdbcStore store,
                             ExecutorService threadPool,
                             SharedPreferences sharedPreferences,
                             @Named(MASTER_ENC_SERVICE_QUALIFIER) EncryptionService encryptionService) {

        this.taskRepository = store.measurementTaskRepository();
        this.threadPool = threadPool;
        this.sharedPreferences = sharedPreferences;

        taskRepository.setEncryptionService(encryptionService);

        providerRegistry = ProviderRegistry.withDefaultProviders();
        runningTaskRegistry = new RunningTaskRegistry();
        phoneBookService = new PhoneBookService();
    }

    @Override
    public void initialize() {
        tasks.getSortedList().setComparator(Comparator.comparing(MeasurementTask::getDateTime).reversed());
        loadTasksFromDatabase();
    }

    private void loadTasksFromDatabase() {
        Promise.supplyAsync(() -> taskRepository.getFromEnd(0, TASK_LIST_LIMIT))
                .then(result -> {
                    tasks.getItems().setAll(result);
                    if (taskSelectionModel.get() != null) {
                        taskSelectionModel.get().selectFirst();
                    }
                })
                .start(threadPool);
    }

    Status getTaskStatus(MeasurementTask task) {
        if (task.isFailed()) { return Status.ERROR; }

        Measurement measurement = task.getResult();
        if (measurement == null) { return runningTaskRegistry.contains(task) ? Status.RUNNING : Status.NEW; }

        return Status.FINISHED;
    }

    boolean taskHasStatus(MeasurementTask task, Status... values) {
        Status taskStatus = getTaskStatus(task);
        for (Status value : values) {
            if (value == taskStatus) { return true; }
        }

        return false;
    }

    ProviderRegistry getProviderRegistry() {
        return providerRegistry;
    }

    PhoneBookService getPhoneBookService() {
        return phoneBookService;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Properties                                                            //
    ///////////////////////////////////////////////////////////////////////////

    private final TransformationListHandle<MeasurementTask> tasks = new TransformationListHandle<>();

    public ObservableList<MeasurementTask> sortedTasks() { return tasks.getSortedList(); }

    private final ObjectProperty<MeasurementTask> selectedTask = new UnconditionalObjectProperty<>(this, "selectedTask");

    public ObjectProperty<MeasurementTask> selectedTaskProperty() { return selectedTask; }

    private final ObjectProperty<SelectionModel<MeasurementTask>> taskSelectionModel = new SimpleObjectProperty<>(this, "selectionModel");

    public void setTaskSelectionModel(SelectionModel<MeasurementTask> selectionModel) { taskSelectionModel.set(Objects.requireNonNull(selectionModel)); }

    ///////////////////////////////////////////////////////////////////////////
    // Commands                                                              //
    ///////////////////////////////////////////////////////////////////////////

    public Command loadTasksCommand() { return loadTasksCommand; }

    private final Command loadTasksCommand = new CommandBase() {
        @Override
        protected void doExecute() {
            loadTasksFromDatabase();
        }
    };

    // ~

    public ConsumerCommand<MeasurementTask> addTaskCommand() { return addTaskCommand; }

    private final ConsumerCommand<MeasurementTask> addTaskCommand = new ConsumerCommandBase<>() {

        @Override
        protected void doExecute(MeasurementTask newTask) {
            Objects.requireNonNull(newTask);

            final List<MeasurementTask> items = tasks.getItems();
            final SelectionModel<MeasurementTask> selectionModel = taskSelectionModel.get();

            Promise.runAsync(() -> taskRepository.add(newTask)).then(() -> {
                items.add(newTask);
                selectionModel.select(newTask);
                if (newTask.isAutoRun()) {
                    runTask(newTask);
                }
            }).start(threadPool);
        }
    };

    // ~

    public ConsumerCommand<MeasurementTask> updateTaskCommand() { return updateTaskCommand; }

    private final ConsumerCommand<MeasurementTask> updateTaskCommand = new ConsumerCommandBase<>() {

        @Override
        protected void doExecute(MeasurementTask task) {
            Objects.requireNonNull(task);

            // to restart finished task it supposed to be duplicated (not updated)
            if (taskHasStatus(task, Status.RUNNING, Status.FINISHED)) { return; }

            Promise.runAsync(() -> taskRepository.update(task)).then(() -> {
                replaceTask(task);
                if (task.isAutoRun()) {
                    runTask(task);
                }
            }).start(threadPool);
        }
    };

    private void replaceTask(MeasurementTask updatedTask) {
        final List<MeasurementTask> items = tasks.getItems();
        final SelectionModel<MeasurementTask> selectionModel = taskSelectionModel.get();

        final int idx = items.indexOf(updatedTask);
        if (idx < 0) { return; }

        MeasurementTask currentlySelectedTask = selectionModel.getSelectedItem();

        // this line will reset selected item value (but not visual selection, funny),
        // so we should determine selected task earlier
        tasks.getItems().set(idx, updatedTask);

        // re-set selection to trigger task details update
        if (Objects.equals(currentlySelectedTask, updatedTask)) {
            taskSelectionModel.get().clearSelection();
            taskSelectionModel.get().select(updatedTask);
        }
    }

    // ~

    public Command removeTaskCommand() { return removeTaskCommand; }

    private final Command removeTaskCommand = new CommandBase() {

        { executable.bind(selectedTask.isNotNull()); }

        @Override
        protected void doExecute() {
            final List<MeasurementTask> items = tasks.getItems();
            final SelectionModel<MeasurementTask> selectionModel = taskSelectionModel.get();
            final MeasurementTask taskToRemove = selectedTask.get();
            final int sortedIndex = tasks.getSortedList().indexOf(taskToRemove);

            if (getTaskStatus(taskToRemove) == Status.RUNNING) { return; }

            Promise.runAsync(() -> taskRepository.remove(taskToRemove)).then(() -> {
                items.remove(taskToRemove);

                // load more tasks from database
                if (items.isEmpty()) {
                    loadTasksFromDatabase();
                    return;
                }

                // mark next task for removal
                if (sortedIndex <= items.size() - 1) {
                    selectionModel.select(sortedIndex);
                } else {
                    selectionModel.selectFirst();
                }
            }).start(threadPool);
        }
    };

    // ~

    public ConsumerCommand<File> exportHistoryCommand() { return exportHistoryCommand; }

    private final ConsumerCommand<File> exportHistoryCommand = new ConsumerCommandBase<>() {

        @Override
        protected void doExecute(File outputFile) {
            MeasurementTaskHistoryExporter exporter = new MeasurementTaskHistoryExporter(providerRegistry);
            Promise.runAsync(() -> {
                Collection<MeasurementTask> tasks = taskRepository.getFromEnd(0, 1000);
                exporter.export(tasks, outputFile);
            }).start(threadPool);
        }
    };

    // ~

    public ConsumerCommand<File> printTaskCommand() { return printTaskCommand; }

    private final ConsumerCommand<File> printTaskCommand = new ConsumerCommandBase<>() {

        { executable.bind(selectedTask.isNotNull()); }

        @Override
        protected void doExecute(File outputFile) {
            MeasurementTaskPrinter printer = new MeasurementTaskPrinter(providerRegistry, sharedPreferences.getTheme());
            Promise.runAsync(() -> printer.print(selectedTask.get(), outputFile)).start(threadPool);
        }
    };

    // ~

    public ConsumerCommand<MeasurementTask> runTaskCommand() { return runTaskCommand; }

    private final ConsumerCommand<MeasurementTask> runTaskCommand = new ConsumerCommandBase<>() {

        @Override
        protected void doExecute(MeasurementTask task) {
            runTask(task);
        }
    };

    private void runTask(MeasurementTask task) {
        if (task == null || taskHasStatus(task, Status.RUNNING, Status.FINISHED)) { return; }

        if (runningTaskRegistry.isHostInUse(task.getHost())) {
            DefaultEventBus.getInstance().publish(
                    Notification.warning(t(LINETEST_ERROR_HOST_ALREADY_IN_USE, task.getHost()))
            );
            return;
        }

        task.setDateTime(LocalDateTime.now());
        task.setDuration(-1);
        task.resetConnectionStatus();
        task.setResult(null);

        runningTaskRegistry.put(task);
        replaceTask(task);

        LOG.info("Starting task: " + task);

        Promise.supplyAsync(() -> {
            // a short pause to show running indicator to the user
            // to avoid ugly flickering if test is failed immediately after start
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

            MeasurementTask finishedTask = runLineTest(task);
            taskRepository.update(finishedTask);
            return finishedTask;
        }).then(finishedTask -> {
            runningTaskRegistry.remove(task);
            fireProgressEvent(task, false, Integer.MAX_VALUE, 0);
            replaceTask(finishedTask);

            if (!finishedTask.isFailed()) {
                LOG.info("Task [" + task.getId() + "] is finished");
            } else {
                LOG.info("Task [" + task.getId() + "] is finished with error");
            }
        }).start(threadPool);
    }

    private MeasurementTask runLineTest(MeasurementTask task) {
        LinetestRequest request = Objects.requireNonNull(task.getRequest());
        LinetestProvider provider = Objects.requireNonNull(providerRegistry.get(request.getProvider()));

        MeasurementTask finishedTask = new MeasurementTask(task);

        try (LinetestSession session = provider.createSession(task.getConnectionParams())) {
            session.connect();

            finishedTask.setConnectionSucceeded();
            fireProgressEvent(task, true, 0, session.getAverageTestDuration().toSeconds());

            Measurement measurement = session.runTest(request.getLine());
            finishedTask.setResult(measurement);
            finishedTask.setDuration((int) ChronoUnit.SECONDS.between(task.getDateTime(), LocalDateTime.now()));
        } catch (Exception e) {
            finishedTask.setConnectionFailed();
            LOG.severe(ExceptionUtils.getStackTrace(e));
        }

        return finishedTask;
    }

    private void fireProgressEvent(MeasurementTask task, boolean running, long processed, long total) {
        DefaultEventBus.getInstance().publish(new TaskProgressEvent(String.valueOf(task.getId()), running, processed, total));
    }
}
