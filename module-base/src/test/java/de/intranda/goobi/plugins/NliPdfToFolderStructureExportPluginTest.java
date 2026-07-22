package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.sub.goobi.config.ConfigPlugins;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigPlugins.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*" })
public class NliPdfToFolderStructureExportPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File tempFolder;
    private static String resourcesFolder;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse

        System.setProperty("log4j.configurationFile", log4jFile);
    }

    @Before
    public void setUp() throws Exception {
        tempFolder = folder.newFolder("tmp");

        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        PowerMock.mockStatic(ConfigPlugins.class);
        EasyMock.expect(ConfigPlugins.getPluginConfig(EasyMock.anyString())).andReturn(getConfig()).anyTimes();
        PowerMock.replay(ConfigPlugins.class);
    }

    @Test
    public void testConstructor() {
        NliPdfToFolderStructureExportPlugin plugin = new NliPdfToFolderStructureExportPlugin();
        assertNotNull(plugin);
    }

    @Test
    public void testBuildFileNameIncludesIssueNumber() {
        DateTimeFormatter fWrite = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate pubDate = LocalDate.of(2026, 4, 7);

        String fileName = NliPdfToFolderStructureExportPlugin.buildFileName(fWrite, pubDate, 1, "28510");

        assertEquals("20260407_01-N28510.pdf", fileName);
    }

    @Test
    public void testBuildFileNamePadsRunningNumberToTwoDigits() {
        DateTimeFormatter fWrite = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate pubDate = LocalDate.of(2026, 4, 7);

        String fileName = NliPdfToFolderStructureExportPlugin.buildFileName(fWrite, pubDate, 12, "28510");

        assertEquals("20260407_12-N28510.pdf", fileName);
    }

    private XMLConfiguration getConfig() {
        String file = "plugin_intranda_export_nli_pdf_to_folder_structure.xml";
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        try {
            config.load(resourcesFolder + file);
        } catch (ConfigurationException e) {
        }
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        return config;
    }

}
