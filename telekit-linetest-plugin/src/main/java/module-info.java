import telekit.base.plugin.Plugin;

module telekit.plugins.linetest {

    provides Plugin with telekit.plugins.linetest.LinetestPlugin;

    requires telekit.base;
    requires telekit.controls;

    requires org.apache.commons.lang3;
    requires org.apache.commons.net;
    requires com.hierynomus.sshj;
    requires expectit.core;
    requires org.apache.commons.collections4;
    requires commons.dbutils;
    requires org.flywaydb.core;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;

    exports telekit.plugins.linetest;
    exports telekit.plugins.linetest.database;

    exports telekit.plugins.linetest.domain;
    exports telekit.plugins.linetest.provider;
    exports telekit.plugins.linetest.tool;

    exports telekit.plugins.linetest.demo to
            javafx.graphics, javafx.base, telekit.base;
    opens telekit.plugins.linetest.demo to
            javafx.graphics, javafx.base, telekit.base;

    // grant unconditional access to SQL migrations and i18n resources
    exports telekit.plugins.linetest.database.migration;
    opens telekit.plugins.linetest.database.migration;
    exports telekit.plugins.linetest.i18n;
    opens telekit.plugins.linetest.i18n;

    // grant access to plugin assets, so other modules could use it
    opens telekit.plugins.linetest.assets;
}