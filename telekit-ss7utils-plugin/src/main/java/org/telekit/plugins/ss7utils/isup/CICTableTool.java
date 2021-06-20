package org.telekit.plugins.ss7utils.isup;

import javafx.scene.Node;
import org.telekit.base.plugin.Tool;
import org.telekit.base.plugin.ToolGroup;
import org.telekit.plugins.ss7utils.i18n.SS7UtilsMessages;

import static org.telekit.base.i18n.I18n.t;
import static org.telekit.plugins.ss7utils.SS7UtilsPlugin.SS7_TOOL_GROUP;

public class CICTableTool implements Tool<CICTableView> {

    @Override
    public String getName() { return t(SS7UtilsMessages.SS7UTILS_CIC_TABLE); }

    @Override
    public ToolGroup getGroup() { return SS7_TOOL_GROUP; }

    @Override
    public Class<CICTableView> getComponent() { return CICTableView.class; }

    @Override
    public Node getIcon() { return null; }
}
