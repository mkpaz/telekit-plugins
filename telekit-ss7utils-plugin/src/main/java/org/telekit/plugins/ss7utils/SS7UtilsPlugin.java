package org.telekit.plugins.ss7utils;

import org.telekit.base.di.DependencyModule;
import org.telekit.base.i18n.BundleLoader;
import org.telekit.base.plugin.Includes;
import org.telekit.base.plugin.Metadata;
import org.telekit.base.plugin.Plugin;
import org.telekit.base.plugin.ToolGroup;
import org.telekit.base.service.ArtifactRepository;
import org.telekit.base.service.crypto.EncryptionService;
import org.telekit.base.util.ClasspathResource;
import org.telekit.plugins.ss7utils.i18n.SS7UtilsMessages;
import org.telekit.plugins.ss7utils.isup.CICTableTool;
import org.telekit.plugins.ss7utils.mtp.SPCConverterTool;

import java.io.InputStreamReader;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Includes({CICTableTool.class, SPCConverterTool.class})
public class SS7UtilsPlugin implements Plugin {

    public static final ToolGroup SS7_TOOL_GROUP = new SS7ToolGroup();
    public static final ClasspathResource PLUGIN_MODULE_PATH = ClasspathResource.of("/org/telekit/plugins/ss7utils", SS7UtilsPlugin.class);
    public static final String PLUGIN_PROPERTIES_FILE = "plugin.properties";

    private final Metadata metadata;

    public SS7UtilsPlugin() throws Exception {
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
    public BundleLoader getBundleLoader() { return SS7UtilsMessages.getLoader(); }

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
