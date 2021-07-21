package org.telekit.plugins.linetest.demo;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.telekit.base.desktop.Component;
import org.telekit.base.di.DependencyModule;
import org.telekit.base.di.Provides;
import org.telekit.base.i18n.BundleLoader;
import org.telekit.base.service.crypto.EncryptionService;
import org.telekit.controls.demo.BaseLauncher;
import org.telekit.plugins.linetest.LinetestDependencyModule;
import org.telekit.plugins.linetest.LinetestPlugin;
import org.telekit.plugins.linetest.database.JdbcStore;
import org.telekit.plugins.linetest.i18n.LinetestMessages;
import org.telekit.plugins.linetest.tool.LinetestView;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginLauncher extends BaseLauncher {

    private final JdbcStore store = JdbcStore.createDefault();

    public static void main(String[] args) { launch(args); }

    @Override
    protected Class<? extends Component> getComponent() {
        return LinetestView.class;
    }

    @Override
    protected Collection<BundleLoader> getBundleLoaders() {
        return List.of(LinetestMessages.getLoader());
    }

    @Override
    protected void initServices() {
        store.migrate();
    }

    @Override
    protected void initStage(Stage stage, Scene scene) {
        scene.getStylesheets().addAll(LinetestPlugin.STYLESHEETS);
    }

    @Override
    protected List<DependencyModule> getDependencyModules() {
        return List.of(
                new LinetestDependencyModule(store),
                new TestDependencyModule()
        );
    }

    ///////////////////////////////////////////////////////////////////////////

    public static class TestDependencyModule implements DependencyModule {

        @Provides
        @Singleton
        public ExecutorService executorService() {
            return Executors.newCachedThreadPool();
        }

        @Provides
        @Singleton
        @Named("masterEncryptionService")
        public EncryptionService masterEncryptionService() {
            return new DummyEncryptionService();
        }
    }
}