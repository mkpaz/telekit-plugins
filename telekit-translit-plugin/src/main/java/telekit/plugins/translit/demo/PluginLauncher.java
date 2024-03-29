package telekit.plugins.translit.demo;

import telekit.base.desktop.Component;
import telekit.base.i18n.BundleLoader;
import telekit.controls.demo.BaseLauncher;
import telekit.plugins.translit.i18n.TranslitMessages;
import telekit.plugins.translit.tool.TranslitView;

import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static telekit.base.Env.getPropertyOrEnv;

public class PluginLauncher extends BaseLauncher {

    public static void main(String[] args) { launch(args); }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<? extends Component> getComponent() {
        String prop = getPropertyOrEnv("telekit.launcher.tool", "TELEKIT_LAUNCHER_TOOL");
        if (isNotEmpty(prop)) {
            try {
                return (Class<? extends Component>) Class.forName(prop);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TranslitView.class;
    }

    @Override
    protected Collection<BundleLoader> getBundleLoaders() { return List.of(TranslitMessages.getLoader()); }
}