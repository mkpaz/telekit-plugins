import org.telekit.base.plugin.Plugin;

module telekit.plugins.ss7utils {

    provides Plugin with org.telekit.plugins.ss7utils.SS7UtilsPlugin;

    requires java.base;
    requires telekit.base;
    requires telekit.controls;

    requires org.apache.commons.lang3;

    exports org.telekit.plugins.ss7utils;
    exports org.telekit.plugins.ss7utils.isup;
    exports org.telekit.plugins.ss7utils.mtp;

    exports org.telekit.plugins.ss7utils.demo to
            javafx.graphics, javafx.base, telekit.base;
    opens org.telekit.plugins.ss7utils.demo to
            javafx.graphics, javafx.base, telekit.base;

    exports org.telekit.plugins.ss7utils.i18n;
    opens org.telekit.plugins.ss7utils.i18n;
}