package telekit.plugins.linetest;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import telekit.base.i18n.BaseMessages;
import telekit.base.i18n.I18n;
import telekit.plugins.linetest.i18n.LinetestMessages;

public class OrdinaryExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        I18n.getInstance().register(BaseMessages.getLoader());
        I18n.getInstance().register(LinetestMessages.getLoader());
        I18n.getInstance().reload();
    }
}