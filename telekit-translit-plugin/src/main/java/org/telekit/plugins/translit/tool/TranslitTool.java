package org.telekit.plugins.translit.tool;

import javafx.scene.Node;
import org.telekit.base.plugin.Tool;
import org.telekit.base.plugin.ToolGroup;
import org.telekit.plugins.translit.i18n.TranslitMessages;

import static org.telekit.base.i18n.I18n.t;

public class TranslitTool implements Tool<TranslitView> {

    @Override
    public String getName() { return t(TranslitMessages.TRANSLIT_TRANSLITERATOR); }

    @Override
    public ToolGroup getGroup() { return null; }

    @Override
    public Class<TranslitView> getComponent() { return TranslitView.class; }

    @Override
    public Node getIcon() { return null; }
}
