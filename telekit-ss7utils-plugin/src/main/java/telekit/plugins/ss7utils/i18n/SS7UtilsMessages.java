package telekit.plugins.ss7utils.i18n;

import telekit.base.i18n.BaseMessages;
import telekit.base.i18n.BundleLoader;
import telekit.controls.i18n.ControlsMessages;

public interface SS7UtilsMessages extends BaseMessages, ControlsMessages {

    String SS7UTILS_SS7 = "ss7utils.SS7";
    String SS7UTILS_CIC_TABLE = "ss7utils.CICTable";
    String SS7UTILS_SPC_CONVERTER = "ss7utils.SPCConverter";
    String SS7UTILS_SIGNALLING_POINT_CODE = "ss7utils.SignallingPointCode";
    String SS7UTILS_FIRST_CIC = "ss7utils.FirstCIC";
    String SS7UTILS_LAST_CIC = "ss7utils.LastCIC";
    String SS7UTILS_MSG_INVALID_POINT_CODE = "ss7utils.msg.invalid-point-code";
    String SS7UTILS_SPECIFIED_AS = "ss7utils.specified-as";

    static BundleLoader getLoader() { return BundleLoader.of(SS7UtilsMessages.class); }
}
