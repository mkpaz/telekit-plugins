package org.telekit.plugins.linetest.tool;

import javafx.scene.Node;
import org.telekit.base.plugin.Tool;
import org.telekit.base.plugin.ToolGroup;
import org.telekit.plugins.linetest.i18n.LinetestMessages;

import static org.telekit.base.i18n.I18n.t;

public class LinetestTool implements Tool<LinetestView> {

    @Override
    public String getName() { return t(LinetestMessages.LINETEST_LINETEST); }

    @Override
    public ToolGroup getGroup() { return null; }

    @Override
    public Class<LinetestView> getComponent() { return LinetestView.class; }

    @Override
    public Node getIcon() { return null; }
}
