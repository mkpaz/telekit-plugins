package telekit.plugins.ss7utils.isup;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import org.jetbrains.annotations.Nullable;
import telekit.base.desktop.Component;
import telekit.base.di.Initializable;
import telekit.base.util.CollectionUtils;
import telekit.controls.util.Containers;
import telekit.controls.util.Controls;
import telekit.plugins.ss7utils.i18n.SS7UtilsMessages;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static telekit.base.i18n.I18n.t;
import static telekit.controls.util.Containers.*;
import static telekit.plugins.ss7utils.isup.ISUPUtils.MAX_CIC;

@Singleton
public class CICTableView extends HBox implements Initializable, Component {

    public ListView<Integer> listStreams;
    public ListView<String> listTimeslots;
    public TextField cicSearch;
    public Label firstCic;
    public Label lastCic;

    @Inject
    public CICTableView() {
        createView();
    }

    private void createView() {
        VBox streamsBox = new VBox();
        streamsBox.setSpacing(5);

        listStreams = Controls.create(ListView::new, "monospace");
        listStreams.setPrefWidth(120);
        streamsBox.getChildren().addAll(new Label("E1"), listStreams);
        VBox.setVgrow(listStreams, Priority.ALWAYS);

        VBox timeslotsBox = new VBox();
        timeslotsBox.setSpacing(5);

        listTimeslots = Controls.create(ListView::new, "monospace");
        listTimeslots.setPrefWidth(120);
        timeslotsBox.getChildren().addAll(new Label("Timeslot - CIC"), listTimeslots);
        VBox.setVgrow(listTimeslots, Priority.ALWAYS);

        cicSearch = new TextField();

        firstCic = new Label();
        lastCic = new Label();

        GridPane infoPane = Containers.gridPane(10, 5, Insets.EMPTY, "info");
        infoPane.setMaxHeight(Region.USE_PREF_SIZE);

        infoPane.add(new Label(t(SS7UtilsMessages.SEARCH)), 0, 0, GridPane.REMAINING, 1);
        infoPane.add(cicSearch, 0, 1, GridPane.REMAINING, 1);

        infoPane.add(verticalGap(10), 0, 2, GridPane.REMAINING, 1);

        infoPane.add(new Label(" " + t(SS7UtilsMessages.SS7UTILS_FIRST_CIC) + " :"), 0, 3, 1, 1);
        infoPane.add(firstCic, 1, 3, 1, 1);

        infoPane.add(new Label(" " + t(SS7UtilsMessages.SS7UTILS_LAST_CIC) + " :"), 0, 4, 1, 1);
        infoPane.add(lastCic, 1, 4, 1, 1);

        infoPane.getRowConstraints().addAll(
                rowConstraints(Priority.NEVER),
                rowConstraints(Priority.NEVER),
                rowConstraints(Priority.NEVER),
                rowConstraints(Priority.NEVER)
        );

        setAlignment(Pos.CENTER);
        getChildren().addAll(
                horizontalSpacer(),
                streamsBox,
                infoPane,
                timeslotsBox,
                horizontalSpacer()
        );
        setSpacing(10);
        setPadding(new Insets(10));
        setId("cic-table");
    }

    @Override
    public void initialize() {
        cicSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isNotBlank(newVal)) {
                findCICPositionAndScrollToIt(newVal);
            } else {
                listStreams.getSelectionModel().selectFirst();
                listTimeslots.getSelectionModel().selectFirst();
                listTimeslots.scrollTo(0);
            }
        });

        listStreams.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) { return; }
            updateCICInfo(newVal - 1); // E1 numbers start from 0, but list values start from 1
            listTimeslots.scrollTo(0);
        });
        listStreams.setItems(FXCollections.observableArrayList(CollectionUtils.generate(1, MAX_CIC / 32)));
        listStreams.getSelectionModel().selectFirst();
    }

    private void updateCICInfo(Integer e1num) {
        ObservableList<String> timeslots = FXCollections.observableArrayList();
        List<Integer> cicIDs = ISUPUtils.getCICRange(e1num);

        for (int index = 0; index < cicIDs.size(); index++) {
            timeslots.add(leftPad(String.valueOf(index + 1), 2) + " - " + cicIDs.get(index));
        }

        listTimeslots.setItems(timeslots);
        firstCic.setText(String.valueOf(CollectionUtils.getFirstElement(cicIDs)));
        lastCic.setText(String.valueOf(CollectionUtils.getLastElement(cicIDs)));
    }

    private void findCICPositionAndScrollToIt(String str) {
        try {
            int cic = Integer.parseInt(str);

            int e1num = ISUPUtils.findE1ByCIC(cic);
            if (e1num >= 0) {
                listStreams.getSelectionModel().select(e1num);
                listStreams.scrollTo(e1num - 1);
            }

            int timeslot = ISUPUtils.findTimeslotByCIC(cic);
            if (timeslot > 0) {
                listTimeslots.getSelectionModel().select(timeslot - 1);
                listTimeslots.scrollTo(timeslot - 1);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public Region getRoot() { return this; }

    @Override
    public void reset() {}

    @Override
    public @Nullable Node getPrimaryFocusNode() { return cicSearch; }
}
