package org.telekit.plugins.translit.i18n;

import org.telekit.base.i18n.BaseMessages;
import org.telekit.base.i18n.BundleLoader;
import org.telekit.controls.i18n.ControlsMessages;

public interface TranslitMessages extends BaseMessages, ControlsMessages {

    String TRANSLIT_TRANSLITERATOR = "translit.Transliterator";
    String TRANSLIT_TRANSLITERATE = "translit.Transliterate";
    String TRANSLIT_TRANSLITERATED_TEXT = "translit.TransliteratedText";

    static BundleLoader getLoader() { return BundleLoader.of(TranslitMessages.class); }
}
