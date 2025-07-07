package org.codeforcompassion.animalwelfare.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.codeforcompassion.animalwelfare.model.ArticleDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GoogleSheetService {

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    private int duplicateCount = 0;

    private static final String APPLICATION_NAME = "Animal Welfare Tracker";

    public void appendArticle(ArticleDTO article) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();

        String monthSheetName = getMonthlySheetName(article.getPublishedDate() != null
                ? article.getPublishedDate().toString()
                : "");

        ensureSheetExists(service, monthSheetName);

        reorderMonthlySheets();

        // Step 1: Check for duplicates
        ValueRange existingRows = safeGetWithRetry(service, monthSheetName + "!C2:C"); // Column C = URL
        List<List<Object>> values = existingRows.getValues();
        if (values != null) {
            for (List<Object> row : values) {
                if (!row.isEmpty() && article.getUrl().equalsIgnoreCase(row.get(0).toString())) {
                    duplicateCount++;
                    log.info("Skipping duplicate article: {}", article.getTitle());
                    return;
                }
            }
        }

        // Step 2: Prepare row
        String trimmedSummary = article.getSummary();
        if (trimmedSummary != null && trimmedSummary.length() > 100) {
            trimmedSummary = trimmedSummary.substring(0, 100) + "...";
        }

        List<Object> row = Arrays.asList(
                article.getPublishedDate() != null ? article.getPublishedDate().toString() : "",
                article.getTitle(),
                article.getUrl(),
                article.getSentiment(),
                String.join(", ", article.getThemes()),
                article.getTone(),
                String.join(", ", article.getAuthorities()),
                article.getLocation(),
                article.isFestivalLinked() ? "YES" : "NO",
                article.getFestivalName() != null ? article.getFestivalName() : "",
                article.getSource() != null ? article.getSource() : "",
                trimmedSummary
        );

        ValueRange body = new ValueRange().setValues(Collections.singletonList(row));

        // Step 3: Append row with retry
        safeAppendWithRetry(service, monthSheetName + "!A1", body);
    }

    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        String base64Creds = System.getenv("GOOGLE_CREDS_BASE64");

        if (base64Creds == null || base64Creds.isBlank()) {
            throw new IllegalStateException("GOOGLE_CREDS_BASE64 environment variable not set");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(base64Creds);
        File tempFile = File.createTempFile("google-creds", ".json");
        Files.write(tempFile.toPath(), decodedBytes);

        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new FileInputStream(tempFile))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));

        ServiceAccountCredentials credentials = (ServiceAccountCredentials) googleCredentials;

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private String getMonthlySheetName(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return "Articles_UnknownDate";
        }
        return "Articles_" + dateString.substring(0, 7); // e.g., 2025-07
    }

    private void ensureSheetExists(Sheets service, String sheetName) throws IOException {
        List<String> existingSheets = service.spreadsheets()
                .get(spreadsheetId)
                .execute()
                .getSheets()
                .stream()
                .map(sheet -> sheet.getProperties().getTitle())
                .toList();

        if (!existingSheets.contains(sheetName)) {
            var addSheetRequest = new com.google.api.services.sheets.v4.model.Request()
                    .setAddSheet(new com.google.api.services.sheets.v4.model.AddSheetRequest()
                            .setProperties(new com.google.api.services.sheets.v4.model.SheetProperties()
                                    .setTitle(sheetName)));

            var batchUpdateRequest = new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                    .setRequests(List.of(addSheetRequest));

            var response = service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();

            Integer newSheetId = response.getReplies().get(0).getAddSheet().getProperties().getSheetId();

            List<Object> headers = List.of("Date", "Title", "URL", "Sentiment", "Themes", "Tone", "Authorities", "Location", "Festival", "Festival Name", "Source", "Summary");
            ValueRange headerRow = new ValueRange().setValues(List.of(headers));

            service.spreadsheets().values()
                    .update(spreadsheetId, sheetName + "!A1", headerRow)
                    .setValueInputOption("RAW")
                    .execute();

            com.google.api.services.sheets.v4.model.CellFormat format = new com.google.api.services.sheets.v4.model.CellFormat()
                    .setTextFormat(new com.google.api.services.sheets.v4.model.TextFormat().setBold(true))
                    .setBackgroundColor(new com.google.api.services.sheets.v4.model.Color()
                            .setRed(0.9f).setGreen(0.9f).setBlue(0.9f));

            com.google.api.services.sheets.v4.model.RepeatCellRequest repeatCellRequest = new com.google.api.services.sheets.v4.model.RepeatCellRequest()
                    .setRange(new com.google.api.services.sheets.v4.model.GridRange()
                            .setSheetId(newSheetId)
                            .setStartRowIndex(0)
                            .setEndRowIndex(1)) // Header row only
                    .setCell(new com.google.api.services.sheets.v4.model.CellData().setUserEnteredFormat(format))
                    .setFields("userEnteredFormat(backgroundColor,textFormat)");

            com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest formatRequest =
                    new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                            .setRequests(List.of(new com.google.api.services.sheets.v4.model.Request()
                                    .setRepeatCell(repeatCellRequest)));

            service.spreadsheets().batchUpdate(spreadsheetId, formatRequest).execute();
        }
    }

    private ValueRange safeGetWithRetry(Sheets service, String range) throws IOException {
        int retries = 5;
        int backoff = 1000;

        for (int i = 0; i < retries; i++) {
            try {
                return service.spreadsheets().values().get(spreadsheetId, range).execute();
            } catch (IOException e) {
                if (e.getMessage().contains("429")) {
                    log.warn("GET hit rate limit (attempt {}), retrying in {}ms", i + 1, backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ignored) {}
                    backoff *= 2;
                } else {
                    throw e;
                }
            }
        }

        throw new IOException("GET failed after retries due to rate limiting.");
    }

    private void safeAppendWithRetry(Sheets service, String range, ValueRange valueRange) throws IOException {
        int retries = 5;
        int backoff = 1000;

        for (int i = 0; i < retries; i++) {
            try {
                service.spreadsheets().values()
                        .append(spreadsheetId, range, valueRange)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
                return;
            } catch (IOException e) {
                if (e.getMessage().contains("429")) {
                    log.warn("APPEND hit rate limit (attempt {}), retrying in {}ms", i + 1, backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ignored) {}
                    backoff *= 2;
                } else {
                    throw e;
                }
            }
        }

        throw new IOException("APPEND failed after retries due to rate limiting.");
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void reorderMonthlySheets() throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();

        List<com.google.api.services.sheets.v4.model.Sheet> sheets = service.spreadsheets()
                .get(spreadsheetId)
                .execute()
                .getSheets();

        List<com.google.api.services.sheets.v4.model.Sheet> sorted = sheets.stream()
                .filter(s -> s.getProperties().getTitle().matches("^Articles_\\d{4}-\\d{2}$"))
                .sorted((s1, s2) -> {
                    String title1 = s1.getProperties().getTitle().split("_")[1];
                    String title2 = s2.getProperties().getTitle().split("_")[1];
                    return title2.compareTo(title1); // reverse chronological order
                })
                .toList();

        List<com.google.api.services.sheets.v4.model.Request> requests = new java.util.ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {
            var sheet = sorted.get(i);
            var updateRequest = new com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest()
                    .setProperties(new com.google.api.services.sheets.v4.model.SheetProperties()
                            .setSheetId(sheet.getProperties().getSheetId())
                            .setIndex(i + 1)) // place after Summary if needed
                    .setFields("index");

            requests.add(new com.google.api.services.sheets.v4.model.Request().setUpdateSheetProperties(updateRequest));
        }

        if (!requests.isEmpty()) {
            service.spreadsheets()
                    .batchUpdate(spreadsheetId,
                            new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                                    .setRequests(requests))
                    .execute();
            log.info("Reordered {} monthly sheets in reverse order successfully.", sorted.size());
        }
    }


}
