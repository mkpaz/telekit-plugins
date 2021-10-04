import telekit.base.plugin.Plugin;
import telekit.plugins.translit.TranslitPlugin;

module telekit.plugins.translit {

    provides Plugin with TranslitPlugin;

    requires java.base;
    requires telekit.base;
    requires telekit.controls;
    requires org.apache.commons.lang3;

    exports telekit.plugins.translit;
    exports telekit.plugins.translit.tool;

    exports telekit.plugins.translit.demo to
            javafx.graphics, javafx.base, telekit.base;
    opens telekit.plugins.translit.demo to
            javafx.graphics, javafx.base, telekit.base;

    exports telekit.plugins.translit.i18n;
    opens telekit.plugins.translit.i18n;
}