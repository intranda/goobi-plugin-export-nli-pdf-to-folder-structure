package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IExportPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class NliPdfToFolderStructureExportPlugin implements IExportPlugin, IPlugin {

    private static final long serialVersionUID = -8466626389946881597L;
    @Getter
    private String title = "intranda_export_nli_pdf_to_folder_structure";
    @Getter
    private PluginType type = PluginType.Export;
    @Getter
    @Setter
    private Step step;

    @Getter
    private List<String> problems;

    @Override
    public void setExportFulltext(boolean arg0) {
    }

    @Override
    public void setExportImages(boolean arg0) {
    }

    @Override
    public boolean startExport(Process process) throws IOException, InterruptedException, DocStructHasNoTypeException, PreferencesException,
            WriteException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException, ReadException, SwapException, DAOException,
            TypeNotAllowedForParentException {
        String benutzerHome = process.getProjekt().getDmsImportImagesPath();
        return startExport(process, benutzerHome);
    }

    @Override
    public boolean startExport(Process process, String destination) throws IOException, InterruptedException, DocStructHasNoTypeException,
            PreferencesException, WriteException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException, ReadException,
            SwapException, DAOException, TypeNotAllowedForParentException {
        problems = new ArrayList<>();
        VariableReplacer replacer;

        // read information from config file
        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        String exportFolder = config.getString("exportFolder", "/opt/digiverso/export/");
        String metdataPublicationDate = config.getString("metdataPublicationDate", "$(meta.DateOfOrigin)");
        String metdataPublicationCode = config.getString("metdataPublicationCode", "$(meta.Type)");
        String metdataIssueNumber = config.getString("metdataIssueNumber", "$(meta.CurrentNo)");
        String dateReadPattern = config.getString("dateReadPattern", "yyyy-MM-dd");
        String dateWritePattern = config.getString("dateWritePattern", "ddMMyyyy");
        int issueNumberTargetLength = config.getInt("issueNumberTargetLength", 0);
        String issueNumberPadCharConfig = config.getString("issueNumberPadChar", "0");
        char issueNumberPadChar = issueNumberPadCharConfig.isEmpty() ? '0' : issueNumberPadCharConfig.charAt(0);

        // read mets file to test if it is readable
        try {
            Prefs prefs = process.getRegelsatz().getPreferences();
            Fileformat ff = null;
            ff = process.readMetadataFile();
            DigitalDocument dd = ff.getDigitalDocument();
            replacer = new VariableReplacer(dd, prefs, process, null);
        } catch (ReadException | PreferencesException | IOException | SwapException e) {
            log.error(e);
            problems.add("Cannot read metadata file.");
            return false;
        }

        // get publication date and check it
        String publicationDateString = replacer.replace(metdataPublicationDate);
        if (publicationDateString.equals(metdataPublicationDate)) {
            String error = "Export failed, metadata for publicaten date cannot be found (" + publicationDateString + ".";
            log.error(error);
            problems.add(error);
            return false;
        }

        // get publication code and check it
        String publicationCode = replacer.replace(metdataPublicationCode);
        if (publicationCode.equals(metdataPublicationCode)) {
            String error = "Export failed, metadata for publicaten code cannot be found (" + publicationCode + ".";
            log.error(error);
            problems.add(error);
            return false;
        }

        // get issue number and check it
        String issueNumber = replacer.replace(metdataIssueNumber);
        // If the number is not presen in metadata use default of 0
        if (issueNumber.equals(metdataIssueNumber)) {
            String info = "Export failed, metadata for issue number cannot be found (" + issueNumber + ".";
            issueNumber = "0";
            log.info(info);
        }
        // pad issue number to the configured length
        issueNumber = padIssueNumber(issueNumber, issueNumberTargetLength, issueNumberPadChar);

        // prepare date conversion
        DateTimeFormatter fRead = DateTimeFormatter.ofPattern(dateReadPattern);
        DateTimeFormatter fWrite = DateTimeFormatter.ofPattern(dateWritePattern);
        LocalDate pubDate = LocalDate.parse(publicationDateString, fRead);
        LocalDate curDate = LocalDate.now();

        //define folder structure and create folder if it is not there already
        StorageProviderInterface sp = StorageProvider.getInstance();
        Path folder = Paths.get(exportFolder, curDate.format(fWrite), publicationCode);
        sp.createDirectories(folder);

        // define file name and check if it exists already, otherwise find next free file name
        int currentNo = 1;
        Path file = Paths.get(folder.toString(), buildFileName(fWrite, pubDate, currentNo, issueNumber));
        while (sp.isFileExists(file)) {
            file = Paths.get(folder.toString(), buildFileName(fWrite, pubDate, ++currentNo, issueNumber));
        }

        // find PDF files in master folder and take the first one to copy to target folder
        List<Path> pdffiles = sp.listFiles(process.getImagesOrigDirectory(false), pdfFilter);
        if (!pdffiles.isEmpty()) {
            sp.copyFile(pdffiles.get(0), file);
        } else {
            String error = "Export failed, no PDF file found in folder " + process.getImagesOrigDirectory(false) + "to import.";
            log.error(error);
            problems.add(error);
            return false;
        }

        log.info("Export executed for process with ID " + process.getId());
        return true;
    }

    /**
     * Builds the export file name in the form {@code <publication date>_<running number>-N<issue number>.pdf},
     * e.g. {@code 20260407_01-N28510.pdf}.
     */
    static String buildFileName(DateTimeFormatter dateWriteFormatter, LocalDate publicationDate, int runningNumber, String issueNumber) {
        return publicationDate.format(dateWriteFormatter) + "_" + String.format("%02d", runningNumber) + "-N" + issueNumber + ".pdf";
    }

    /**
     * Pads the issue number with the given character up to the given target length. If the issue number is already
     * at least as long as the target length, it is returned unchanged.
     */
    static String padIssueNumber(String issueNumber, int targetLength, char padChar) {
        return StringUtils.leftPad(issueNumber, targetLength, padChar);
    }

    /**
     * File Filter to get pdf files from file system
     */
    private static final DirectoryStream.Filter<Path> pdfFilter = path -> {
        String name = path.getFileName().toString();
        return name.toLowerCase().endsWith(".pdf");
    };
}
