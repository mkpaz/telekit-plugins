package telekit.plugins.linetest;

import telekit.base.di.DependencyModule;
import telekit.base.i18n.BundleLoader;
import telekit.base.plugin.Includes;
import telekit.base.plugin.Metadata;
import telekit.base.plugin.Plugin;
import telekit.base.service.ArtifactRepository;
import telekit.base.service.crypto.EncryptionService;
import telekit.base.util.ClasspathResource;
import telekit.plugins.linetest.database.JdbcStore;
import telekit.plugins.linetest.i18n.LinetestMessages;
import telekit.plugins.linetest.tool.LinetestTool;

import java.io.InputStreamReader;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Includes({LinetestTool.class})
public class LinetestPlugin implements Plugin {

    public static final ClasspathResource PLUGIN_MODULE_PATH = ClasspathResource.of("/telekit/plugins/linetest", LinetestPlugin.class);
    public static final String PLUGIN_PROPERTIES_FILE = "plugin.properties";
    public static final Set<String> STYLESHEETS = Set.of(
            PLUGIN_MODULE_PATH.concat("assets/index.css").toString()
    );

    private final Metadata metadata;
    private final JdbcStore jdbcStore;

    public LinetestPlugin() throws Exception {
        metadata = new Metadata();

        String path = PLUGIN_MODULE_PATH.concat(PLUGIN_PROPERTIES_FILE).toString();
        InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(path)),
                UTF_8
        );

        Properties properties = new Properties();
        properties.load(reader);

        metadata.setName(properties.getProperty(METADATA_NAME));
        metadata.setAuthor(properties.getProperty(METADATA_AUTHOR));
        metadata.setVersion(properties.getProperty(METADATA_VERSION));
        metadata.setDescription(properties.getProperty(METADATA_DESCRIPTION));
        metadata.setHomePage(properties.getProperty(METADATA_HOMEPAGE));
        metadata.setPlatformVersion(properties.getProperty(METADATA_PLATFORM_VERSION));

        this.jdbcStore = JdbcStore.createDefault();
    }

    @Override
    public Metadata getMetadata() { return metadata; }

    @Override
    public Collection<? extends DependencyModule> getModules() {
        return List.of(new LinetestDependencyModule(jdbcStore));
    }

    @Override
    public BundleLoader getBundleLoader() { return LinetestMessages.getLoader(); }

    @Override
    public Collection<String> getStylesheets() { return STYLESHEETS; }

    @Override
    public ArtifactRepository getRepository() { return null; }

    @Override
    public void start() {
        jdbcStore.migrate();
    }

    @Override
    public void stop() {}

    @Override
    public void updateEncryptedData(EncryptionService oldEncryptor,
                                    EncryptionService newEncryptor) {}

    @Override
    public boolean providesDocs() { return false; }

    @Override
    public void openDocs(Locale locale) {}
}
