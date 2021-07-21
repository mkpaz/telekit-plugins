package org.telekit.plugins.linetest.i18n;

import org.telekit.base.i18n.BaseMessages;
import org.telekit.base.i18n.BundleLoader;
import org.telekit.controls.i18n.ControlsMessages;

public interface LinetestMessages extends BaseMessages, ControlsMessages {

    // headers
    String LINETEST_CONNECTION_PARAMS = "linetest.ConnectionParams";
    String LINETEST_CONNECTION_FAILED = "linetest.ConnectionFailed";
    String LINETEST_CREATE_NEW_TEST = "linetest.CreateNewTest";
    String LINETEST_EQUIPMENT_MODEL = "linetest.EquipmentModel";
    String LINETEST_LINE_ID = "linetest.LineID";
    String LINETEST_LINE_STATUS = "linetest.LineStatus";
    String LINETEST_LINETEST = "linetest.Linetest";
    String LINETEST_MEASUREMENT_RESULT = "linetest.MeasurementResult";
    String LINETEST_PHONE_BOOK = "linetest.PhoneBook";
    String LINETEST_RAW_OUTPUT = "linetest.RawOutput";
    String LINETEST_RING_GROUND = "linetest.RingGround";
    String LINETEST_RUN_TEST = "linetest.RunTest";
    String LINETEST_RUNNING = "linetest.Running";
    String LINETEST_TASKS = "linetest.Tasks";
    String LINETEST_TEST_PARAMS = "linetest.TestParams";
    String LINETEST_TEST_FAILED = "linetest.TestFailed";
    String LINETEST_TIP_GROUND = "linetest.TipGround";
    String LINETEST_TIP_RING = "linetest.TipRing";
    String LINETEST_TRY_AGAIN = "linetest.TryAgain";
    String LINETEST_UNKNOWN_EQUIPMENT = "linetest.UnknownEquipment";

    // messages
    String LINETEST_ERROR_CONNECTION_FAILED = "linetest.error.connection-failed";
    String LINETEST_ERROR_DATA_TRANSFER_FAILED = "linetest.error.data-transfer-failed";
    String LINETEST_ERROR_HOST_ALREADY_IN_USE = "linetest.error.host-is-already-in-use";

    // other
    String LINETEST_SECONDS = "linetest.seconds";

    static BundleLoader getLoader() { return BundleLoader.of(LinetestMessages.class); }
}
