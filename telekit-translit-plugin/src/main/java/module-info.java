import org.telekit.base.plugin.Plugin;
import org.telekit.plugins.translit.TranslitPlugin;

module telekit.plugins.translit {

    provides Plugin with TranslitPlugin;

    requires java.base;
    requires telekit.base;
    requires telekit.controls;
    requires org.apache.commons.lang3;

    exports org.telekit.plugins.translit;
    exports org.telekit.plugins.translit.tool;

    exports org.telekit.plugins.translit.demo to
            javafx.graphics, javafx.base, telekit.base;
    opens org.telekit.plugins.translit.demo to
            javafx.graphics, javafx.base, telekit.base;

    exports org.telekit.plugins.translit.i18n;
    opens org.telekit.plugins.translit.i18n;
}