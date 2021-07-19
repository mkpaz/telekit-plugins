package org.telekit.plugins.translit;

import org.telekit.base.di.DependencyModule;
import org.telekit.base.i18n.BundleLoader;
import org.telekit.base.plugin.Includes;
import org.telekit.base.plugin.Metadata;
import org.telekit.base.plugin.Plugin;
import org.telekit.base.service.ArtifactRepository;
import org.telekit.base.service.EncryptionService;
import org.telekit.base.util.ClasspathResource;
import org.telekit.plugins.translit.i18n.TranslitMessages;
import org.telekit.plugins.translit.tool.TranslitTool;

import java.io.InputStreamReader;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Includes(TranslitTool.class)
public class TranslitPlugin implements Plugin {

    public static final ClasspathResource PLUGIN_MODULE_PATH = ClasspathResource.of("/org/telekit/plugins/translit", TranslitPlugin.class);
    public static final String PLUGIN_PROPERTIES_FILE = "plugin.properties";

    private final Metadata metadata;

    public TranslitPlugin() throws Exception {
        metadata = new Metadata();

        Properties properties = new Properties();
        String path = PLUGIN_MODULE_PATH.concat(PLUGIN_PROPERTIES_FILE).toString();
        InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(path)), UTF_8);
        properties.load(reader);

        metadata.setName(properties.getProperty(METADATA_NAME));
        metadata.setAuthor(properties.getProperty(METADATA_AUTHOR));
        metadata.setVersion(properties.getProperty(METADATA_VERSION));
        metadata.setDescription(properties.getProperty(METADATA_DESCRIPTION));
        metadata.setHomePage(properties.getProperty(METADATA_HOMEPAGE));
        metadata.setPlatformVersion(properties.getProperty(METADATA_PLATFORM_VERSION));
    }

    @Override
    public Metadata getMetadata() { return metadata; }

    @Override
    public Collection<? extends DependencyModule> getModules() { return Collections.emptyList(); }

    @Override
    public BundleLoader getBundleLoader() { return TranslitMessages.getLoader(); }

    @Override
    public Collection<String> getStylesheets() { return Collections.emptyList(); }

    @Override
    public ArtifactRepository getRepository() { return null; }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public void updateEncryptedData(EncryptionService encryptionService, EncryptionService encryptionService1) {}

    @Override
    public boolean providesDocs() { return false; }

    @Override
    public void openDocs(Locale locale) {}
}
