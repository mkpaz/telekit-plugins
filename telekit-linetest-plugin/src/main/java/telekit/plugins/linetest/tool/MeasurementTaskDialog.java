package telekit.plugins.linetest.tool;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;
import telekit.base.desktop.Focusable;
import telekit.base.domain.Action;
import telekit.base.domain.exception.TelekitException;
import telekit.base.domain.security.UsernamePasswordCredentials;
import telekit.base.net.connection.BaseConnectionParams;
import telekit.base.net.connection.ConnectionParams;
import telekit.base.net.connection.Scheme;
import telekit.controls.custom.RevealablePasswordField;
import telekit.controls.util.*;
import telekit.controls.widgets.OverlayDialog;
import telekit.plugins.linetest.domain.LinetestRequest;
import telekit.plugins.linetest.domain.MeasurementTask;
import telekit.plugins.linetest.provider.LinetestProvider;

import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;

import static javafx.geometry.HPos.RIGHT;
import static javafx.geometry.Pos.CENTER_LEFT;
import static org.apache.commons.lang3.StringUtils.trim;
import static telekit.base.i18n.I18n.t;
import static telekit.controls.util.BindUtils.isNotBlank;
import static telekit.controls.util.Containers.*;
import static telekit.controls.util.Controls.button;
import static telekit.plugins.linetest.i18n.LinetestMessages.*;

class MeasurementTaskDialog extends OverlayDialog implements Focusable {

    ComboBox<LinetestProvider> providerChoice;
    TextField lineIdText;
    ComboBox<Scheme> schemeChoice;
    TextField hostText;
    Spinner<Integer> portSpinner;
    TextField usernameText;
    RevealablePasswordField passwordText;

    Button runBtn;
    Button saveBtn;

    private final ProviderRegistry providers;
    private final ObjectProperty<LinetestProvider> selectedProvider = new UnconditionalObjectProperty<>(this, "selectedProvider");

    private Action action;
    private MeasurementTask task;
    private BiConsumer<Action, MeasurementTask> onCommitCallback;

    public MeasurementTaskDialog(ProviderRegistry providers) {
        this.providers = providers;
        createContent();
    }

    public void setOnCommit(BiConsumer<Action, MeasurementTask> handler) {
        this.onCommitCallback = handler;
    }

    private void createContent() {
        VBox content = vbox(10, CENTER_LEFT, new Insets(10));
        content.getChildren().setAll(
                createTestParamsArea(),
                createConnectionParamsArea()
        );

        runBtn = button(t(ACTION_RUN), null, "form-action");
        runBtn.setDefaultButton(true);
        runBtn.setOnAction(e -> commit(true));

        saveBtn = button(t(ACTION_SAVE), null, "form-action");
        saveBtn.setOnAction(e -> commit(false));

        footerBox.getChildren().add(1, saveBtn);
        footerBox.getChildren().add(1, runBtn);
        setPrefWidth(500);
        setTitle(t(ACTION_ADD));

        initialize();
        setContent(content);
    }

    private TitledPane createTestParamsArea() {
        providerChoice = new ComboBox<>(FXCollections.observableArrayList(providers.getAll()));
        providerChoice.setButtonCell(new ProviderListCell());
        providerChoice.setCellFactory(property -> new ProviderListCell());
        providerChoice.setPrefWidth(300);
        HBox.setHgrow(providerChoice, Priority.ALWAYS);
        providerChoice.setMaxWidth(Double.MAX_VALUE);

        HBox providerBox = hbox(0, CENTER_LEFT, Insets.EMPTY);
        providerBox.getChildren().setAll(providerChoice);

        lineIdText = new TextField();

        // GRID

        GridPane grid = Containers.gridPane(20, 10, new Insets(10));

        grid.add(Controls.gridLabel(t(LINETEST_EQUIPMENT_MODEL), RIGHT, providerChoice), 0, 0);
        grid.add(providerBox, 1, 0);

        grid.add(Controls.gridLabel(t(LINETEST_LINE_ID), RIGHT, lineIdText), 0, 1);
        grid.add(lineIdText, 1, 1);

        grid.getColumnConstraints().setAll(HGROW_NEVER, HGROW_ALWAYS);

        TitledPane root = new TitledPane(t(LINETEST_TEST_PARAMS), grid);
        root.setCollapsible(false);

        return root;
    }

    private TitledPane createConnectionParamsArea() {
        schemeChoice = new ComboBox<>();
        schemeChoice.setPrefWidth(100);

        hostText = new TextField();
        HBox.setHgrow(hostText, Priority.ALWAYS);

        portSpinner = new Spinner<>(0, 65535, 0);
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);
        IntegerStringConverter.createFor(portSpinner);

        HBox connectionBox = hbox(0, CENTER_LEFT, Insets.EMPTY);
        connectionBox.getChildren().setAll(
                schemeChoice, hostText, portSpinner
        );

        usernameText = new TextField();
        usernameText.setMaxWidth(Double.MAX_VALUE);

        passwordText = Controls.passwordField();

        // GRID

        GridPane grid = Containers.gridPane(20, 10, new Insets(10));

        grid.add(Controls.gridLabel("URL", RIGHT, hostText), 0, 0);
        grid.add(connectionBox, 1, 0);

        grid.add(Controls.gridLabel(t(USERNAME), RIGHT, usernameText), 0, 1);
        grid.add(usernameText, 1, 1);

        grid.add(Controls.gridLabel(t(PASSWORD), RIGHT, passwordText), 0, 2);
        grid.add(passwordText.getParent(), 1, 2);

        grid.getColumnConstraints().setAll(HGROW_NEVER, HGROW_ALWAYS);

        TitledPane root = new TitledPane(t(LINETEST_CONNECTION_PARAMS), grid);
        root.setCollapsible(false);

        return root;
    }

    private void initialize() {
        final PausableChangeListener<Scheme> schemeListener = new PausableChangeListener<>(scheme -> {
            LinetestProvider provider = selectedProvider.get();
            if (scheme == null || provider == null) {
                portSpinner.getValueFactory().setValue(0);
                return;
            }

            int port = provider.supports(scheme) ?
                    provider.getDefaultConnectionParams(scheme).getPort() :
                    scheme.getWellKnownPort();
            portSpinner.getValueFactory().setValue(port);
        });
        schemeChoice.valueProperty().addListener(schemeListener);

        selectedProvider.bindBidirectional(providerChoice.valueProperty());
        selectedProvider.addListener((obs, old, value) -> {
            if (value == null || task == null) {
                schemeChoice.getItems().setAll(Collections.emptyList());
                updateConnectionParams(null);
                return;
            }

            ConnectionParams params = value.supports(task.getConnectionParams()) ?
                    task.getConnectionParams() :
                    value.getDefaultConnectionParams();
            schemeListener.pauseAndRun(() -> {
                schemeChoice.getItems().setAll(value.getSupportedConnections());
                updateConnectionParams(params);
            });
        });

        BooleanBinding allFieldsValid = BindUtils.and(
                providerChoice.valueProperty().isNotNull(),
                isNotBlank(lineIdText.textProperty()),
                isNotBlank(hostText.textProperty()),
                isNotBlank(usernameText.textProperty()),
                isNotBlank(passwordText.textProperty())
        );

        runBtn.disableProperty().bind(allFieldsValid.not());
        saveBtn.disableProperty().bind(allFieldsValid.not());
    }

    ///////////////////////////////////////////////////////////////////////////

    public void setData(Action origAction, @Nullable MeasurementTask origTask) {
        // the dialog shouldn't be initialized if there's no registered providers
        if (providers.isEmpty()) { throw new TelekitException("No registered linetest providers"); }

        action = Objects.requireNonNull(origAction);
        LinetestProvider provider = Objects.requireNonNull(providers.getDefaultProvider());

        switch (action) {
            case ADD -> {
                if (origTask == null) {
                    // creating new test manually
                    LinetestRequest request = new LinetestRequest(provider.getDefaultConnectionParams(), "", "");
                    task = new MeasurementTask(request);
                } else {
                    // creating new test from phone book
                    task = new MeasurementTask(origTask);
                    provider = providers.getOrDefault(task.getProvider(), provider);
                }
                setTitle(t(ACTION_ADD));
            }
            case DUPLICATE -> {
                task = Objects.requireNonNull(origTask).duplicate();
                provider = providers.getOrDefault(task.getProvider(), provider);
                setTitle(t(ACTION_ADD));
            }
            case EDIT -> {
                task = new MeasurementTask(Objects.requireNonNull(origTask));
                provider = providers.getOrDefault(task.getProvider(), provider);
                setTitle(t(ACTION_EDIT));
            }
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }

        lineIdText.setText(task.getLine());
        // this will trigger auto update on all others fields (UnconditionalObjectProperty)
        selectedProvider.set(provider);
    }

    private void updateConnectionParams(@Nullable ConnectionParams params) {
        if (params != null) {
            schemeChoice.setValue(params.getScheme());
            hostText.setText(params.getHost());
            portSpinner.getValueFactory().setValue(params.getPort());
            if (params.getCredentials() instanceof UsernamePasswordCredentials userPassword) {
                // valid credentials
                usernameText.setText(userPassword.getUsername());
                passwordText.setText(userPassword.getPasswordAsString());
            } else {
                // null credentials
                usernameText.setText(null);
                passwordText.setText(null);
            }
        } else {
            schemeChoice.setValue(null);
            hostText.setText(null);
            portSpinner.getValueFactory().setValue(0);
            usernameText.setText(null);
            passwordText.setText(null);
        }
    }

    private void commit(boolean autoRun) {
        task.getRequest().setProvider(providerChoice.getValue().getId());
        task.getRequest().setLine(trim(lineIdText.getText()));

        ConnectionParams connectionParams = new BaseConnectionParams(
                schemeChoice.getValue(),
                trim(hostText.getText()),
                portSpinner.getValue(),
                UsernamePasswordCredentials.of(
                        trim(usernameText.getText()),
                        trim(passwordText.getText())
                )
        );
        task.getRequest().setConnectionParams(connectionParams);
        task.setAutoRun(autoRun);

        if (onCommitCallback != null) {
            onCommitCallback.accept(action, task);
        }
    }

    @Override
    public @Nullable Node getPrimaryFocusNode() {
        return lineIdText;
    }

    ///////////////////////////////////////////////////////////////////////////

    static class ProviderListCell extends ListCell<LinetestProvider> {

        @Override
        protected void updateItem(LinetestProvider provider, boolean empty) {
            super.updateItem(provider, empty);

            if (provider != null && provider.getSupportedEquipment() != null) {
                setText(provider.getSupportedEquipment().printInOneLine());
            } else {
                setText(null);
            }
        }
    }
}
