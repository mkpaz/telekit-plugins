package org.telekit.plugins.ss7utils.mtp;

import javafx.scene.Node;
import org.telekit.base.plugin.Tool;
import org.telekit.base.plugin.ToolGroup;
import org.telekit.plugins.ss7utils.i18n.SS7UtilsMessages;

import static org.telekit.base.i18n.I18n.t;
import static org.telekit.plugins.ss7utils.SS7UtilsPlugin.SS7_TOOL_GROUP;

public class SPCConverterTool implements Tool<SPCConverterView> {

    @Override
    public String getName() { return t(SS7UtilsMessages.SS7UTILS_SPC_CONVERTER); }

    @Override
    public ToolGroup getGroup() { return SS7_TOOL_GROUP; }

    @Override
    public Class<SPCConverterView> getComponent() { return SPCConverterView.class; }

    @Override
    public Node getIcon() { return null; }
}
