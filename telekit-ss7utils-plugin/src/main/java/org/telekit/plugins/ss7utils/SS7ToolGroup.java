package org.telekit.plugins.ss7utils;

import javafx.scene.Node;
import org.telekit.base.plugin.ToolGroup;
import org.telekit.plugins.ss7utils.i18n.SS7UtilsMessages;

import static org.telekit.base.i18n.I18n.t;

public class SS7ToolGroup implements ToolGroup {

    @Override
    public String getName() { return t(SS7UtilsMessages.SS7UTILS_SS7); }

    @Override
    public boolean isExpanded() { return false; }

    @Override
    public Node getIcon() { return null; }
}
