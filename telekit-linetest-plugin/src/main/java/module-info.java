import org.telekit.base.plugin.Plugin;

module telekit.plugins.linetest {

    provides Plugin with org.telekit.plugins.linetest.LinetestPlugin;

    requires telekit.base;
    requires telekit.controls;

    requires org.apache.commons.lang3;
    requires org.apache.commons.net;
    requires sshj;
    requires expectit.core;
    requires org.apache.commons.collections4;
    requires commons.dbutils;
    requires org.flywaydb.core;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;

    exports org.telekit.plugins.linetest;
    exports org.telekit.plugins.linetest.database;

    exports org.telekit.plugins.linetest.domain;
    exports org.telekit.plugins.linetest.provider;
    exports org.telekit.plugins.linetest.tool;

    exports org.telekit.plugins.linetest.demo to
            javafx.graphics, javafx.base, telekit.base;
    opens org.telekit.plugins.linetest.demo to
            javafx.graphics, javafx.base, telekit.base;

    // grant unconditional access to SQL migrations and i18n resources
    exports org.telekit.plugins.linetest.database.migration;
    opens org.telekit.plugins.linetest.database.migration;
    exports org.telekit.plugins.linetest.i18n;
    opens org.telekit.plugins.linetest.i18n;

    // grant access to plugin assets, so other modules could use it
    opens org.telekit.plugins.linetest.assets;
}