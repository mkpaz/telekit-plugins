package telekit.plugins.linetest.tool;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import telekit.base.desktop.Focusable;
import telekit.controls.util.NodeUtils;
import telekit.controls.util.TableUtils;
import telekit.controls.widgets.FilterTable;
import telekit.controls.widgets.OverlayDialog;
import telekit.plugins.linetest.domain.Equipment;
import telekit.plugins.linetest.domain.PhoneBookEntry;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static javafx.geometry.Pos.CENTER_LEFT;
import static telekit.base.i18n.I18n.t;
import static telekit.controls.util.Containers.vbox;
import static telekit.controls.util.Controls.button;
import static telekit.plugins.linetest.i18n.LinetestMessages.*;

class PhoneBookDialog extends OverlayDialog implements Focusable {

    private static final int ROW_LIMIT = 25;

    PhoneBookEntryTable table;

    private final PhoneBookService phoneBookService;
    private final ProviderRegistry providers;

    private Consumer<PhoneBookEntry> onCommitCallback;

    public PhoneBookDialog(PhoneBookService phoneBookService, ProviderRegistry providers) {
        this.phoneBookService = phoneBookService;
        this.providers = providers;

        createContent();
    }

    private void createContent() {
        table = new PhoneBookEntryTable(pattern -> phoneBookService.find(pattern, ROW_LIMIT));

        TableColumn<PhoneBookEntry, String> eqColumn = TableUtils.createColumn(t(LINETEST_EQUIPMENT_MODEL), "provider");
        eqColumn.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String providerId, boolean empty) {
                super.updateItem(providerId, empty);

                if (empty) {
                    setText(null);
                    return;
                }

                Equipment eq = providers.findEquipmentById(providerId);
                setText(eq != null ? eq.getVendor() + " " + eq.getModel() : t(LINETEST_UNKNOWN_EQUIPMENT));
            }
        });

        table.setColumns(
                TableUtils.createColumn(t(PHONE_NUMBER), "phoneNumber"),
                eqColumn,
                TableUtils.createColumn(t(LINETEST_LINE_ID), "line"),
                TableUtils.createColumn(t(DESCRIPTION), "description")
        );
        table.setPredicate((filter, row) -> true);
        table.getDataTable().setOnMouseClicked((MouseEvent e) -> {
            if (NodeUtils.isDoubleClick(e)) { commit(); }
        });

        VBox content = vbox(10, CENTER_LEFT, new Insets(0, 10, 10, 10));
        content.getChildren().setAll(table);

        Button commitBtn = button(t(ACTION_CHOOSE), null, "form-action");
        commitBtn.setDefaultButton(true);
        commitBtn.setOnAction(e -> commit());
        commitBtn.disableProperty().bind(
                table.getDataTable().getSelectionModel().selectedItemProperty().isNull()
        );

        footerBox.getChildren().add(1, commitBtn);

        setPrefWidth(600);
        setPrefHeight(400);
        setTitle(t(LINETEST_PHONE_BOOK));
        setContent(content);
    }

    public void setOnCommit(Consumer<PhoneBookEntry> handler) {
        this.onCommitCallback = handler;
    }

    private void commit() {
        PhoneBookEntry selectedItem = table.getSelectedItem();
        if (onCommitCallback != null && selectedItem != null) {
            onCommitCallback.accept(selectedItem);
        }
    }

    @Override
    public @Nullable Node getPrimaryFocusNode() {
        return table.getFilterTextField();
    }

    ///////////////////////////////////////////////////////////////////////////

    static class PhoneBookEntryTable extends FilterTable<PhoneBookEntry> {

        private final Function<String, List<PhoneBookEntry>> finder;

        public PhoneBookEntryTable(Function<String, List<PhoneBookEntry>> finder) {
            this.finder = finder;
        }

        @Override
        protected void filter() {
            String pattern = filterText.getText();

            if (StringUtils.length(pattern) < 3) {
                clearData();
                return;
            }

            setData(finder.apply(pattern));
        }

        private void clearData() {
            if (!dataTable.getItems().isEmpty()) {
                setData(Collections.emptyList());
            }
        }
    }
}
