import telekit.base.plugin.Plugin;

module telekit.plugins.ss7utils {

    provides Plugin with telekit.plugins.ss7utils.SS7UtilsPlugin;

    requires java.base;
    requires telekit.base;
    requires telekit.controls;

    requires org.apache.commons.lang3;

    exports telekit.plugins.ss7utils;
    exports telekit.plugins.ss7utils.isup;
    exports telekit.plugins.ss7utils.mtp;

    exports telekit.plugins.ss7utils.demo to
            javafx.graphics, javafx.base, telekit.base;
    opens telekit.plugins.ss7utils.demo to
            javafx.graphics, javafx.base, telekit.base;

    exports telekit.plugins.ss7utils.i18n;
    opens telekit.plugins.ss7utils.i18n;
}