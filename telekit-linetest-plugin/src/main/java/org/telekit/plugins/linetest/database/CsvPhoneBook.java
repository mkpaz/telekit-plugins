package org.telekit.plugins.linetest.database;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.telekit.base.Env;
import org.telekit.base.domain.security.Credentials;
import org.telekit.base.domain.security.UsernamePasswordCredentials;
import org.telekit.base.util.FileSystemUtils;
import org.telekit.plugins.linetest.LinetestPlugin;
import org.telekit.plugins.linetest.domain.LinetestRequest;
import org.telekit.plugins.linetest.domain.PhoneBookEntry;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.*;

public class CsvPhoneBook implements PhoneBook {

    private static final Logger LOG = Logger.getLogger(CsvPhoneBook.class.getName());
    private static final Path FILE_PATH = getFilePath();

    private List<String> lines;
    private BasicFileAttributes lastFileAttributes;

    public CsvPhoneBook() {}

    private static Path getFilePath() {
        String prop = Env.getPropertyOrEnv(
                "telekit.linetest.phonebook.csv",
                "TELEKIT_LINETEST_PHONEBOOK_CSV"
        );
        return isNotBlank(prop) ?
                Paths.get(prop) :
                Env.getPluginConfigDir(LinetestPlugin.class).resolve("phone-book.csv");
    }

    @Override
    public List<PhoneBookEntry> find(String pattern, int limit) {
        readFile();

        if (lines == null || lines.isEmpty()) { return Collections.emptyList(); }

        List<PhoneBookEntry> result = new ArrayList<>();
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            if (!line.contains(pattern) || line.startsWith("#")) { continue; }

            PhoneBookEntry entry = parseLine(line, lineIdx);
            if (entry != null) {
                result.add(entry);
            } else {
                LOG.warning(String.format("Unable to parse line [%d]: '%s'", lineIdx, line));
            }

            if (result.size() == limit) { break; }
        }

        return result;
    }

    private @Nullable PhoneBookEntry parseLine(String line, int lineIdx) {
        try {
            String[] columns = line.split("[,;]");

            // each line must contain 6 mandatory columns
            if (columns.length < 6) {
                LOG.warning(String.format("Line [%d] has invalid number of columns", lineIdx));
                return null;
            }

            List<String> invalidColumns = new ArrayList<>();

            String phoneNumber = getMandatoryColumn(columns, 0, invalidColumns);
            String providerId = getMandatoryColumn(columns, 1, invalidColumns);
            String lineId = getMandatoryColumn(columns, 2, invalidColumns);
            String urlString = getMandatoryColumn(columns, 3, invalidColumns);
            String username = getMandatoryColumn(columns, 4, invalidColumns);
            String password = getMandatoryColumn(columns, 5, invalidColumns);

            URI url = createUrl(urlString);
            if (url == null) { invalidColumns.add("C3='" + urlString + "'"); }

            if (!invalidColumns.isEmpty()) {
                LOG.warning(String.format("Line [%d] contains invalid values: %s", lineIdx, invalidColumns));
                return null;
            }

            String description = getOptionalColumn(columns, 6);

            Credentials credentials = UsernamePasswordCredentials.of(username, password);
            LinetestRequest request = LinetestRequest.of(url, credentials, providerId, lineId);

            return new PhoneBookEntry(phoneNumber, request, description);
        } catch (Exception e) {
            return null;
        }
    }

    private String getMandatoryColumn(String[] columns, int colIdx, List<String> invalidColumns) {
        String column = columns[colIdx];
        if (isBlank(column)) { invalidColumns.add(String.format("C%d='%s'", colIdx, column)); }
        return trim(column);
    }

    @SuppressWarnings("SameParameterValue")
    private @Nullable String getOptionalColumn(String[] columns, int colIdx) {
        return columns.length > colIdx ? trim(columns[colIdx]) : null;
    }

    private @Nullable URI createUrl(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private void readFile() {
        // database file may not exist, which means it either wasn't created or was deleted
        // in the latter case we should free the memory
        if (!FileSystemUtils.fileExists(FILE_PATH)) {
            lines = null;
            lastFileAttributes = null;
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(FILE_PATH, BasicFileAttributes.class);
            if (lastFileAttributes != null && Objects.equals(attributes.lastModifiedTime(), lastFileAttributes.lastModifiedTime())) {
                return;
            }
            lines = Files.readAllLines(FILE_PATH, StandardCharsets.UTF_8);
            lastFileAttributes = attributes;
        } catch (IOException e) {
            LOG.warning(ExceptionUtils.getStackTrace(e));
        }
    }
}
