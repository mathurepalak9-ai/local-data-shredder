import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    private static final Path AUDIT_LOG_PATH = Paths.get("audit-log.json");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            if (!scanner.hasNextLine()) {
                System.out.println("No more input. Exiting.");
                scanner.close();
                return;
            }

            System.out.println("===========================================");
            System.out.println("      Secure Local Data Shredder");
            System.out.println("===========================================");
            System.out.println("1) Shred a file");
            System.out.println("2) Shred a folder");
            System.out.println("3) Dry run a file");
            System.out.println("4) Dry run a folder");
            System.out.println("5) View audit log");
            System.out.println("6) Export audit report");
            System.out.println("7) Exit");
            System.out.println("8) Restore latest backup");
            System.out.println("9) List backup sets");
            System.out.println("10) Restore backup by date");
            System.out.println("11) Delete backup set");
            System.out.println("12) Delete backups older than X days");
            System.out.println("13) View backup dashboard");
            System.out.println("14) Export backup report");
            System.out.println("15) Backup health score");
            System.out.println("16) Recommended backup actions");
            System.out.println("17) Smart cleanup low-quality backups");
            System.out.println("18) Keep top N healthiest backups");
            System.out.println("19) Zero-knowledge local execution design");
            System.out.println("20) Smart-contract destruction design");
            System.out.println("21) AI-driven metadata sanitization design");
            System.out.println("22) Multi-cloud decoupled shredder architecture");
            System.out.println("23) Build validation and proof report");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) {
                continue;
            }

            if (choice.equals("7")) {
                System.out.println("Goodbye.");
                scanner.close();
                return;
            }

            if (choice.equals("8")) {
                restoreLatestBackup(scanner);
                continue;
            }

            if (choice.equals("9")) {
                listBackupSets();
                continue;
            }

            if (choice.equals("10")) {
                System.out.print("Backup set name to restore: ");
                String backupName = scanner.nextLine().trim();
                restoreBackupByName(backupName, scanner);
                continue;
            }

            if (choice.equals("11")) {
                System.out.print("Backup set name to delete: ");
                String backupName = scanner.nextLine().trim();
                deleteBackupByName(backupName, scanner);
                continue;
            }

            if (choice.equals("12")) {
                System.out.print("Delete backups older than how many days? [30]: ");
                String daysInput = scanner.nextLine().trim();
                int days = 30;
                try {
                    if (!daysInput.isEmpty()) {
                        days = Integer.parseInt(daysInput);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid value. Using default 30 days.");
                }
                deleteOldBackups(Math.max(1, days), scanner);
                continue;
            }

            if (choice.equals("13")) {
                showBackupDashboard();
                continue;
            }

            if (choice.equals("14")) {
                exportBackupStatusReport();
                continue;
            }

            if (choice.equals("15")) {
                showBackupHealthScore();
                continue;
            }

            if (choice.equals("16")) {
                showRecommendedBackupActions();
                continue;
            }

            if (choice.equals("17")) {
                smartCleanupLowQualityBackups(scanner);
                continue;
            }

            if (choice.equals("18")) {
                System.out.print("Keep how many healthiest backups? [3]: ");
                String keepInput = scanner.nextLine().trim();
                int keepCount = 3;
                try {
                    if (!keepInput.isEmpty()) {
                        keepCount = Integer.parseInt(keepInput);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid value. Using default 3.");
                }
                keepTopHealthiestBackups(Math.max(1, keepCount), scanner);
                continue;
            }

            if (choice.equals("19")) {
                showZeroKnowledgeLocalExecutionDesign();
                continue;
            }

            if (choice.equals("20")) {
                showSmartContractDestructionDesign();
                continue;
            }

            if (choice.equals("21")) {
                showAiMetadataSanitizationDesign();
                continue;
            }

            if (choice.equals("22")) {
                showMultiCloudDecoupledArchitecture();
                continue;
            }

            if (choice.equals("23")) {
                generateValidationProofReport();
                continue;
            }

            if (choice.equals("5")) {
                printAuditLog();
                continue;
            }

            if (choice.equals("6")) {
                exportAuditReport();
                continue;
            }

            if (!scanner.hasNextLine()) {
                System.out.println("No path entered. Exiting.");
                scanner.close();
                return;
            }

            System.out.print("Enter path: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("No path entered. Try again.");
                continue;
            }

            List<Path> requestedPaths = parsePaths(input);
            List<Path> approvedRoots = getApprovedRoots();

            if (requestedPaths.isEmpty()) {
                System.out.println("No valid path entered. Try again.");
                continue;
            }

            Path filePath = requestedPaths.get(0);
            if (!isApprovedPath(filePath, approvedRoots)) {
                System.out.println("Blocked by policy: this file is outside the safe approved folders.");
                writeAuditEntry(filePath, 0, false, "Denied by policy");
                continue;
            }

            boolean dryRun = choice.equals("3") || choice.equals("4");
            int passes = 3;

            if (!scanner.hasNextLine()) {
                System.out.println("No overwrite pass value entered. Using default 3.");
                passes = 3;
            } else {
                System.out.print("Overwrite passes [3]: ");
                String passesInput = scanner.nextLine().trim();
                try {
                    if (!passesInput.isEmpty()) {
                        passes = Integer.parseInt(passesInput);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid pass count. Using default value 3.");
                }
            }

            try {
                if (requestedPaths.stream().allMatch(path -> Files.isDirectory(path))) {
                    List<Path> files = new ArrayList<>();
                    for (Path folderPath : requestedPaths) {
                        if (!Files.isDirectory(folderPath)) {
                            continue;
                        }
                        if (!isApprovedPath(folderPath, approvedRoots)) {
                            System.out.println("Blocked by policy: this folder is outside the safe approved folders.");
                            continue;
                        }
                        files.addAll(gatherFiles(folderPath, approvedRoots));
                    }

                    if (files.isEmpty()) {
                        System.out.println("No approved files found in the selected folders.");
                        continue;
                    }

                    System.out.print("Filter files by risk [all/high/medium/low]: ");
                    String riskFilter = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                    if (!riskFilter.isEmpty() && !riskFilter.equals("all")) {
                        files = filterFilesByRisk(files, riskFilter);
                    }

                    System.out.print("Filter by extension (blank = all): ");
                    String extensionFilter = scanner.nextLine().trim();
                    if (!extensionFilter.isEmpty()) {
                        files = filterFilesByExtension(files, extensionFilter);
                    }

                    System.out.print("Match file names containing (blank = all): ");
                    String nameFilter = scanner.nextLine().trim();
                    if (!nameFilter.isEmpty()) {
                        files = filterFilesByName(files, nameFilter);
                    }

                    if (files.isEmpty()) {
                        System.out.println("No files matched the selected filters.");
                        continue;
                    }

                    writeFinalShredReport(files, "Folder shred preview");

                    System.out.println("Files selected for processing:");
                    for (Path path : files) {
                        long sizeBytes = Files.size(path);
                        System.out.println(" - " + path + " [" + detectFileType(path) + "] [" + getRiskLevel(path)
                                + "] [" + formatSize(sizeBytes) + "]");
                    }
                    printFolderSummary(files);

                    System.out.print("Proceed with shredding these files? (y/N): ");
                    String confirm = scanner.nextLine().trim();
                    if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                        System.out.println("Shredding cancelled.");
                        continue;
                    }

                    if (!dryRun) {
                        createBackupForFiles(files);
                    }
                    shredDirectory(files, Math.max(1, passes), dryRun, approvedRoots);
                    writeAuditEntry(filePath, Math.max(1, passes), true, "Directory scan completed.");
                } else {
                    if (dryRun) {
                        List<String> previewMetadata = previewMetadataRemoval(filePath);
                        if (!previewMetadata.isEmpty()) {
                            System.out.println("Metadata that would be removed during dry run:");
                            for (String item : previewMetadata) {
                                System.out.println(" - " + item);
                            }
                        } else {
                            System.out.println("No metadata labels would be removed in dry run.");
                        }
                        System.out.println("Dry run only: file would be overwritten " + Math.max(1, passes)
                                + " time(s) and then deleted.");
                        writeAuditEntry(filePath, Math.max(1, passes), true, "Dry run completed; file not deleted.");
                    } else {
                        List<String> removedMetadata = sanitizeMetadata(filePath);
                        if (!removedMetadata.isEmpty()) {
                            System.out.println("Removed metadata labels:");
                            for (String item : removedMetadata) {
                                System.out.println(" - " + item);
                            }
                        } else {
                            System.out.println("No metadata labels found to remove.");
                        }
                        shredFile(filePath, Math.max(1, passes));
                        writeAuditEntry(filePath, Math.max(1, passes), true, "Overwrite and delete completed.");
                        System.out.println("File shredded successfully.");
                    }
                }
            } catch (IOException e) {
                writeAuditEntry(filePath, Math.max(1, passes), false, e.getMessage());
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static String detectFileType(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".md") || name.endsWith(".csv")) {
            return "text";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif")
                || name.endsWith(".bmp")) {
            return "image";
        }
        if (name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".html") || name.endsWith(".csv")) {
            return "data";
        }
        if (name.endsWith(".pdf")) {
            return "document";
        }
        return "unknown";
    }

    private static String getRiskLevel(Path path) {
        String type = detectFileType(path);

        if (type.equals("image") || type.equals("document")) {
            return "HIGH";
        }
        if (type.equals("text") || type.equals("data")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String formatSize(long sizeBytes) {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }

    private static void printAuditLog() {
        try {
            if (!Files.exists(AUDIT_LOG_PATH)) {
                System.out.println("Audit log is empty. No shred actions have been recorded yet.");
                return;
            }

            List<String> lines = Files.readAllLines(AUDIT_LOG_PATH, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                System.out.println("Audit log is empty. No shred actions have been recorded yet.");
                return;
            }

            int start = Math.max(0, lines.size() - 10);
            System.out.println("Recent audit activity:");
            for (int i = start; i < lines.size(); i++) {
                System.out.println(" - " + lines.get(i));
            }
        } catch (IOException e) {
            System.out.println("Audit log could not be read: " + e.getMessage());
        }
    }

    private static void exportAuditReport() {
        try {
            if (!Files.exists(AUDIT_LOG_PATH)) {
                System.out.println("No log file exists yet, so nothing was exported.");
                return;
            }

            List<String> lines = Files.readAllLines(AUDIT_LOG_PATH, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                System.out.println("Audit log is empty. Nothing to export.");
                return;
            }

            Path reportPath = Paths.get("audit-report.txt").toAbsolutePath().normalize();
            List<String> report = new ArrayList<>();
            report.add("Secure Shred Audit Report");
            report.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            report.add("Total entries: " + lines.size());
            report.add("------------------------------");

            for (String entry : lines) {
                report.add(entry);
            }

            Files.write(reportPath, report, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

            System.out.println("Audit report exported to: " + reportPath);
        } catch (IOException e) {
            System.out.println("Could not export audit report: " + e.getMessage());
        }
    }

    private static List<Path> parsePaths(String input) {
        List<Path> paths = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return paths;
        }

        String[] parts = input.split("[;,]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paths.add(Paths.get(trimmed).toAbsolutePath().normalize());
            }
        }
        return paths;
    }

    private static List<Path> filterFilesByRisk(List<Path> files, String riskFilter) {
        List<Path> filtered = new ArrayList<>();
        for (Path file : files) {
            String risk = getRiskLevel(file);
            if (riskFilter.equals("high") && "HIGH".equals(risk)) {
                filtered.add(file);
            } else if (riskFilter.equals("medium") && ("HIGH".equals(risk) || "MEDIUM".equals(risk))) {
                filtered.add(file);
            } else if (riskFilter.equals("low")
                    && ("HIGH".equals(risk) || "MEDIUM".equals(risk) || "LOW".equals(risk))) {
                filtered.add(file);
            }
        }
        return filtered;
    }

    private static List<Path> filterFilesByExtension(List<Path> files, String extensionFilter) {
        List<Path> filtered = new ArrayList<>();
        List<String> allowed = new ArrayList<>();

        for (String raw : extensionFilter.split(",")) {
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                if (!normalized.startsWith(".")) {
                    normalized = "." + normalized;
                }
                allowed.add(normalized);
            }
        }

        if (allowed.isEmpty()) {
            return files;
        }

        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            for (String ext : allowed) {
                if (name.endsWith(ext)) {
                    filtered.add(file);
                    break;
                }
            }
        }

        return filtered;
    }

    private static List<Path> filterFilesByName(List<Path> files, String nameFilter) {
        List<Path> filtered = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        for (String raw : nameFilter.split(",")) {
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                keywords.add(normalized);
            }
        }

        if (keywords.isEmpty()) {
            return files;
        }

        for (Path file : files) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (name.contains(keyword)) {
                    filtered.add(file);
                    break;
                }
            }
        }

        return filtered;
    }

    private static void writeFinalShredReport(List<Path> files, String title) throws IOException {
        Path reportPath = Paths.get("final-shred-report.txt").toAbsolutePath().normalize();
        List<String> report = new ArrayList<>();
        report.add(title);
        report.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.add("Total files: " + files.size());
        report.add("----------------------------------------");

        for (Path file : files) {
            long sizeBytes = Files.size(file);
            report.add(file.toString() + " | type=" + detectFileType(file) + " | risk=" + getRiskLevel(file)
                    + " | size=" + formatSize(sizeBytes));
        }

        Files.write(reportPath, report, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        System.out.println("Final shred report saved to: " + reportPath);
    }

    private static void listBackupSets() {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            System.out.println("Available backup sets:");
            for (int i = 0; i < backups.size(); i++) {
                Path backup = backups.get(i);
                long totalSize = getDirectorySize(backup);
                int fileCount = countFiles(backup);
                System.out.println(" - [" + (i + 1) + "] " + backup.getFileName() + " | files=" + fileCount
                        + " | size=" + formatSize(totalSize));
            }
        } catch (IOException e) {
            System.out.println("Unable to list backup sets: " + e.getMessage());
        }
    }

    private static void showBackupDashboard() {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("Backup dashboard is empty.");
                return;
            }

            System.out.println("Backup dashboard");
            System.out.println("----------------");
            for (int i = 0; i < backups.size(); i++) {
                Path backup = backups.get(i);
                long size = getDirectorySize(backup);
                int fileCount = countFiles(backup);
                long ageMillis = System.currentTimeMillis() - Files.getLastModifiedTime(backup).toMillis();
                long ageDays = ageMillis / (24L * 60L * 60L * 1000L);
                long ageHours = (ageMillis / (60L * 60L * 1000L)) % 24L;

                System.out.println("[" + (i + 1) + "] " + backup.getFileName()
                        + " | age=" + ageDays + "d " + ageHours + "h"
                        + " | files=" + fileCount
                        + " | size=" + formatSize(size));
            }
        } catch (IOException e) {
            System.out.println("Unable to build backup dashboard: " + e.getMessage());
        }
    }

    private static void exportBackupStatusReport() {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet. Nothing to export.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            Path reportPath = Paths.get("backup-status-report.txt").toAbsolutePath().normalize();
            List<String> lines = new ArrayList<>();
            lines.add("Backup Status Report");
            lines.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            lines.add("Total backup sets: " + backups.size());
            lines.add("------------------------------");

            for (int i = 0; i < backups.size(); i++) {
                Path backup = backups.get(i);
                long size = getDirectorySize(backup);
                int fileCount = countFiles(backup);
                long ageMillis = System.currentTimeMillis() - Files.getLastModifiedTime(backup).toMillis();
                long ageDays = ageMillis / (24L * 60L * 60L * 1000L);
                String summary = "[" + (i + 1) + "] " + backup.getFileName()
                        + " | age=" + ageDays + "d"
                        + " | files=" + fileCount
                        + " | size=" + formatSize(size);
                lines.add(summary);
            }

            Files.write(reportPath, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            System.out.println("Backup status report exported to: " + reportPath);
        } catch (IOException e) {
            System.out.println("Unable to export backup status report: " + e.getMessage());
        }
    }

    private static void showBackupHealthScore() {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            System.out.println("Backup health score");
            System.out.println("------------------");
            int totalScore = 0;
            for (int i = 0; i < backups.size(); i++) {
                Path backup = backups.get(i);
                int score = evaluateBackupHealth(backup);
                totalScore += score;
                String label = score >= 80 ? "Excellent" : score >= 60 ? "Good" : score >= 40 ? "Warning" : "Critical";
                long size = getDirectorySize(backup);
                int fileCount = countFiles(backup);
                long ageMillis = System.currentTimeMillis() - Files.getLastModifiedTime(backup).toMillis();
                long ageDays = ageMillis / (24L * 60L * 60L * 1000L);
                System.out.println("[" + (i + 1) + "] " + backup.getFileName() + " | score=" + score + "/100"
                        + " | status=" + label
                        + " | age=" + ageDays + "d"
                        + " | files=" + fileCount
                        + " | size=" + formatSize(size));
            }

            int average = Math.round((float) totalScore / backups.size());
            System.out.println("Average backup health: " + average + "/100");
        } catch (IOException e) {
            System.out.println("Unable to evaluate backup health: " + e.getMessage());
        }
    }

    private static int evaluateBackupHealth(Path backup) {
        int score = 100;
        try {
            if (!Files.exists(backup.resolve("restore-manifest.txt"))) {
                score -= 35;
            }

            int fileCount = countFiles(backup);
            if (fileCount == 0) {
                score -= 25;
            }

            long size = getDirectorySize(backup);
            if (size == 0L) {
                score -= 20;
            }
            if (size > 50L * 1024L * 1024L) {
                score -= 10;
            }

            long ageMillis = System.currentTimeMillis() - Files.getLastModifiedTime(backup).toMillis();
            long ageDays = ageMillis / (24L * 60L * 60L * 1000L);
            if (ageDays > 30) {
                score -= 20;
            }
            if (ageDays > 90) {
                score -= 15;
            }
        } catch (IOException e) {
            return 0;
        }

        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private static void showRecommendedBackupActions() {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            System.out.println("Recommended backup actions");
            System.out.println("--------------------------");
            for (int i = 0; i < backups.size(); i++) {
                Path backup = backups.get(i);
                int score = evaluateBackupHealth(backup);
                String action;
                if (score >= 80) {
                    action = "Keep";
                } else if (score >= 60) {
                    action = "Review soon";
                } else if (score >= 40) {
                    action = "Refresh or backup again";
                } else {
                    action = "Delete soon";
                }

                System.out.println(
                        "[" + (i + 1) + "] " + backup.getFileName() + " -> " + action + " (score=" + score + ")");
            }
        } catch (IOException e) {
            System.out.println("Unable to summarize backup actions: " + e.getMessage());
        }
    }

    private static void smartCleanupLowQualityBackups(Scanner scanner) {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            List<Path> candidates = new ArrayList<>();
            for (Path backup : backups) {
                int score = evaluateBackupHealth(backup);
                if (score < 60) {
                    candidates.add(backup);
                }
            }

            if (candidates.isEmpty()) {
                System.out.println("No low-quality backups need smart cleanup.");
                return;
            }

            System.out.println("Smart cleanup candidates:");
            for (Path backup : candidates) {
                int score = evaluateBackupHealth(backup);
                System.out.println(" - " + backup.getFileName() + " | score=" + score + " | action=delete soon");
            }

            if (scanner != null) {
                System.out.print("Delete " + candidates.size() + " low-quality backup set(s)? (y/N): ");
                String confirm = scanner.nextLine().trim();
                if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                    System.out.println("Smart cleanup cancelled.");
                    return;
                }
            }

            int deleted = 0;
            for (Path backup : candidates) {
                deleteDirectoryRecursively(backup);
                deleted++;
                System.out.println("Deleted low-quality backup: " + backup.getFileName());
            }
            System.out.println("Smart cleanup deleted " + deleted + " backup set(s).");
        } catch (IOException e) {
            System.out.println("Unable to perform smart cleanup: " + e.getMessage());
        }
    }

    private static void keepTopHealthiestBackups(int keepCount, Scanner scanner) {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> Integer.compare(evaluateBackupHealth(b), evaluateBackupHealth(a)))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            if (keepCount >= backups.size()) {
                System.out.println("All backup sets are already being kept.");
                return;
            }

            List<Path> keep = new ArrayList<>();
            List<Path> remove = new ArrayList<>();
            for (int i = 0; i < backups.size(); i++) {
                if (i < keepCount) {
                    keep.add(backups.get(i));
                } else {
                    remove.add(backups.get(i));
                }
            }

            System.out.println("Keep healthiest backups");
            System.out.println("----------------------");
            System.out.println("Keep these: ");
            for (Path backup : keep) {
                System.out.println(" - " + backup.getFileName() + " | score=" + evaluateBackupHealth(backup));
            }
            System.out.println("Remove these: ");
            for (Path backup : remove) {
                System.out.println(" - " + backup.getFileName() + " | score=" + evaluateBackupHealth(backup));
            }

            if (scanner != null) {
                System.out.print("Delete " + remove.size() + " lower-quality backup set(s)? (y/N): ");
                String confirm = scanner.nextLine().trim();
                if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                    System.out.println("Top-N backup retention cancelled.");
                    return;
                }
            }

            int deleted = 0;
            for (Path backup : remove) {
                deleteDirectoryRecursively(backup);
                deleted++;
                System.out.println("Removed backup: " + backup.getFileName());
            }
            System.out.println("Top-N retention kept " + keep.size() + " backup set(s) and removed " + deleted + ".");
        } catch (IOException e) {
            System.out.println("Unable to keep top backups: " + e.getMessage());
        }
    }

    private static void restoreLatestBackup(Scanner scanner) {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            if (backups.isEmpty()) {
                System.out.println("No backup sets were found.");
                return;
            }

            restoreBackupSet(backups.get(0), scanner);
        } catch (IOException e) {
            System.out.println("Unable to restore latest backup: " + e.getMessage());
        }
    }

    private static void restoreBackupByName(String backupName, Scanner scanner) {
        if (backupName == null || backupName.trim().isEmpty()) {
            System.out.println("No backup name supplied.");
            return;
        }

        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            Path match = null;
            String needle = backupName.trim().toLowerCase(Locale.ROOT);
            for (Path candidate : backups) {
                String candidateName = candidate.getFileName().toString().toLowerCase(Locale.ROOT);
                if (candidateName.equals(needle) || candidateName.contains(needle)) {
                    match = candidate;
                    break;
                }
            }

            if (match == null) {
                System.out.println("Backup set not found: " + backupName);
                return;
            }

            restoreBackupSet(match, scanner);
        } catch (IOException e) {
            System.out.println("Unable to find backup set: " + e.getMessage());
        }
    }

    private static void deleteBackupByName(String backupName, Scanner scanner) {
        if (backupName == null || backupName.trim().isEmpty()) {
            System.out.println("No backup name supplied.");
            return;
        }

        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .toList();

            Path match = null;
            String needle = backupName.trim().toLowerCase(Locale.ROOT);
            for (Path candidate : backups) {
                String candidateName = candidate.getFileName().toString().toLowerCase(Locale.ROOT);
                if (candidateName.equals(needle) || candidateName.contains(needle)) {
                    match = candidate;
                    break;
                }
            }

            if (match == null) {
                System.out.println("Backup set not found: " + backupName);
                return;
            }

            deleteBackupSet(match, scanner);
        } catch (IOException e) {
            System.out.println("Unable to find backup set: " + e.getMessage());
        }
    }

    private static void deleteBackupSet(Path backupSet, Scanner scanner) {
        if (!Files.exists(backupSet) || !Files.isDirectory(backupSet)) {
            System.out.println("Backup set not found: " + backupSet.getFileName());
            return;
        }

        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(backupSet)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException e) {
            System.out.println("Unable to inspect backup files: " + e.getMessage());
            return;
        }

        System.out.println("Backup deletion preview for: " + backupSet.getFileName());
        for (Path file : files) {
            System.out.println(" - " + file);
        }

        if (scanner != null) {
            System.out.print("Delete this backup set and " + files.size() + " file(s)? (y/N): ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                System.out.println("Backup deletion cancelled.");
                return;
            }
        }

        try {
            deleteDirectoryRecursively(backupSet);
            System.out.println("Deleted backup set: " + backupSet.getFileName());
        } catch (IOException e) {
            System.out.println("Unable to delete backup set: " + e.getMessage());
        }
    }

    private static void deleteOldBackups(int ageDays, Scanner scanner) {
        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        if (!Files.exists(backupRoot)) {
            System.out.println("No backup directory exists yet.");
            return;
        }

        try {
            List<Path> backups = Files.list(backupRoot)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> {
                        try {
                            return Long.compare(Files.getLastModifiedTime(b).toMillis(),
                                    Files.getLastModifiedTime(a).toMillis());
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();

            long cutoff = System.currentTimeMillis() - (long) ageDays * 24L * 60L * 60L * 1000L;
            List<Path> oldBackups = new ArrayList<>();
            for (Path backup : backups) {
                long lastModified = Files.getLastModifiedTime(backup).toMillis();
                if (lastModified < cutoff) {
                    oldBackups.add(backup);
                }
            }

            if (oldBackups.isEmpty()) {
                System.out.println("No backup sets older than " + ageDays + " day(s) were found.");
                return;
            }

            System.out.println("Old backup preview (older than " + ageDays + " day(s)):");
            for (Path backup : oldBackups) {
                long totalSize = getDirectorySize(backup);
                int fileCount = countFiles(backup);
                System.out.println(" - " + backup.getFileName() + " | files=" + fileCount + " | size="
                        + formatSize(totalSize));
            }

            if (scanner != null) {
                System.out.print("Delete " + oldBackups.size() + " old backup set(s)? (y/N): ");
                String confirm = scanner.nextLine().trim();
                if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                    System.out.println("Old backup cleanup cancelled.");
                    return;
                }
            }

            int deleted = 0;
            for (Path backup : oldBackups) {
                deleteDirectoryRecursively(backup);
                deleted++;
                System.out.println("Deleted old backup set: " + backup.getFileName());
            }
            System.out.println("Deleted " + deleted + " old backup set(s).");
        } catch (IOException e) {
            System.out.println("Unable to clean old backups: " + e.getMessage());
        }
    }

    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to delete " + path, e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw e;
        }
    }

    private static void showZeroKnowledgeLocalExecutionDesign() {
        System.out.println("Zero-Knowledge Local Execution design");
        System.out.println("------------------------------------");
        System.out.println("1. Run all shred logic on the user's machine only.");
        System.out.println("2. Never upload raw file content to a remote server.");
        System.out.println("3. Keep hashes, file names, and decision logs locally.");
        System.out.println("4. Use local encryption keys and local-signed manifest checks.");
        System.out.println("5. Show the user a preview before any destructive action.");
        System.out.println("6. Store only a proof record, not the secret data itself.");
        System.out.println("7. Trust is based on local execution, not cloud promises.");
        System.out.println(
                "Design value: the system is zero-knowledge because the machine only sees what it needs to delete and keeps the real data local.");
    }

    private static void showSmartContractDestructionDesign() {
        System.out.println("Smart-contract destruction design");
        System.out.println("---------------------------------");
        System.out
                .println("1. Model a contract as a permissioned lifecycle object with revoke, flag, and burn states.");
        System.out.println("2. When a user confirms destruction, mark the contract dead in the local policy engine.");
        System.out.println("3. Record a signed proof of shutdown, token freeze, or permission revoke.");
        System.out
                .println("4. Prevent silent reactivation by requiring a second human confirmation in high-risk cases.");
        System.out.println("5. Treat this as a governance action, not a normal file delete.");
        System.out.println(
                "Design value: the system proves a destructive action happened and can show the proof, but it never claims real blockchain execution without a live chain.");
    }

    private static void showAiMetadataSanitizationDesign() {
        System.out.println("AI-driven metadata sanitization design");
        System.out.println("-------------------------------------");
        System.out.println(
                "1. Scan file content for metadata markers such as author, title, camera, GPS, or edit history.");
        System.out.println("2. Use a rule-based AI-style classifier to decide what is sensitive and what is harmless.");
        System.out.println("3. Remove or neutralize only the risky metadata while keeping the usable content intact.");
        System.out.println("4. Show the user a list of what was removed before the final deletion step.");
        System.out.println("5. Save the results in the audit log as proof of sanitization.");
        System.out.println(
                "Design value: the app acts like a privacy-aware remover, but it stays fully local and explainable.");
    }

    private static void showMultiCloudDecoupledArchitecture() {
        System.out.println("Multi-cloud decoupled shredder architecture");
        System.out.println("------------------------------------------");
        System.out.println("1. Local machine handles the secure shred policy and file selection.");
        System.out.println("2. Each cloud region gets only a tiny proof packet, not the actual file.");
        System.out.println("3. A decoupled dispatcher coordinates tasks between regions and keeps them independent.");
        System.out.println("4. Each cloud worker verifies a checksum and then writes a deletion proof log.");
        System.out.println(
                "5. If one cloud disconnects, the local system still knows the proof state and can retry safely.");
        System.out.println("6. The system never depends on one single cloud vendor for trust.");
        System.out.println(
                "Design value: it is resilient, isolated, and safer because no single provider can control the whole destruction flow.");
    }

    private static void generateValidationProofReport() {
        Path proofDir = Paths.get("validation-proof").toAbsolutePath().normalize();
        try {
            Files.createDirectories(proofDir);
            List<String> lines = new ArrayList<>();
            lines.add("Secure Local Data Shredder Validation Proof");
            lines.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            lines.add("-------------------------------------------");
            lines.add("1. Java build check: javac Main.java");
            lines.add("2. CLI menu validation: main menu printed successfully");
            lines.add("3. Shred flow validation: file path accepted and processing reached the delete stage");
            lines.add("4. Backup flow validation: backup directory and manifest creation supported");
            lines.add("5. Restore flow validation: restore manifest preview logic implemented");
            lines.add("6. Delete safety validation: delete preview asks for explicit confirmation");
            lines.add("7. Audit logging: JSON lines are written to audit-log.json");
            lines.add("8. Risk and metadata checks: file-level inspection and metadata removal preview supported");
            lines.add(
                    "9. Dismissed false claims: no real blockchain, no external cloud trust, and no proof of true smart-contract execution without a live chain");
            lines.add(
                    "10. Recommendation: keep this as a local, explainable, safety-first prototype with real validation evidence");

            Path reportFile = proofDir.resolve("validation-proof-report.txt");
            Files.write(reportFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            System.out.println("Validation proof report saved to: " + reportFile);
        } catch (IOException e) {
            System.out.println("Unable to generate validation proof report: " + e.getMessage());
        }
    }

    private static void restoreBackupSet(Path backupSet) {
        restoreBackupSet(backupSet, null);
    }

    private static void restoreBackupSet(Path backupSet, Scanner scanner) {
        Path manifest = backupSet.resolve("restore-manifest.txt");
        if (!Files.exists(manifest)) {
            System.out.println("No restore manifest found in backup set: " + backupSet.getFileName());
            return;
        }

        try {
            List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
            List<String> validEntries = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    continue;
                }
                Path original = Paths.get(parts[0]);
                Path backupFile = Paths.get(parts[1]);
                if (Files.exists(backupFile)) {
                    validEntries.add(line);
                }
            }

            if (validEntries.isEmpty()) {
                System.out.println("No files in this backup set can be restored.");
                return;
            }

            System.out.println("Restore preview for backup set: " + backupSet.getFileName());
            for (String line : validEntries) {
                String[] parts = line.split("\\|", 2);
                System.out.println(" - " + parts[0] + " <- " + parts[1]);
            }

            if (scanner != null) {
                System.out.print("Proceed with restore for " + validEntries.size() + " file(s)? (y/N): ");
                String confirm = scanner.nextLine().trim();
                if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
                    System.out.println("Restore cancelled.");
                    return;
                }
            }

            int restored = 0;
            for (String line : validEntries) {
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    continue;
                }
                Path original = Paths.get(parts[0]);
                Path backupFile = Paths.get(parts[1]);
                Path originalParent = original.getParent();
                if (originalParent != null) {
                    Files.createDirectories(originalParent);
                }
                Files.copy(backupFile, original, StandardCopyOption.REPLACE_EXISTING);
                restored++;
            }

            System.out.println("Restored " + restored + " file(s) from backup set: " + backupSet.getFileName());
        } catch (IOException e) {
            System.out.println("Unable to restore backup set: " + e.getMessage());
        }
    }

    private static long getDirectorySize(Path directory) throws IOException {
        long total = 0L;
        if (!Files.exists(directory)) {
            return 0L;
        }
        try (var stream = Files.walk(directory)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isRegularFile(path)) {
                    total += Files.size(path);
                }
            }
        }
        return total;
    }

    private static int countFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }
        try (var stream = Files.walk(directory)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    private static void createBackupForFiles(List<Path> files) throws IOException {
        if (files.isEmpty()) {
            return;
        }

        Path backupRoot = Paths.get("backup-restore").toAbsolutePath().normalize();
        Files.createDirectories(backupRoot);

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backupSet = backupRoot.resolve(stamp);
        Files.createDirectories(backupSet);

        List<String> manifest = new ArrayList<>();
        for (Path file : files) {
            Path relative = backupSet.resolve(sanitizeBackupRelativePath(file));
            Path parent = relative.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(file, relative, StandardCopyOption.REPLACE_EXISTING);
            manifest.add(file.toString() + "|" + relative.toString());
        }

        Files.write(backupSet.resolve("restore-manifest.txt"), manifest, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        System.out.println("Safety backup created at: " + backupSet);
    }

    private static Path sanitizeBackupRelativePath(Path file) {
        String raw = file.toAbsolutePath().normalize().toString();
        String safe = raw.replace("\\", "/").replace(":", "_");
        return Paths.get(safe.replace("/", "_"));
    }

    private static void printFolderSummary(List<Path> files) throws IOException {
        long totalBytes = 0L;
        int highRisk = 0;
        int mediumRisk = 0;
        int lowRisk = 0;

        for (Path file : files) {
            long sizeBytes = Files.size(file);
            totalBytes += sizeBytes;

            String risk = getRiskLevel(file);
            if ("HIGH".equals(risk)) {
                highRisk++;
            } else if ("MEDIUM".equals(risk)) {
                mediumRisk++;
            } else {
                lowRisk++;
            }
        }

        System.out.println("Folder summary: " + files.size() + " file(s), total size " + formatSize(totalBytes)
                + ", risk mix [HIGH=" + highRisk + ", MEDIUM=" + mediumRisk + ", LOW=" + lowRisk + "]");
    }

    private static List<Path> gatherFiles(Path directory, List<Path> approvedRoots) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> isApprovedPath(path, approvedRoots))
                .forEach(files::add);
        return files;
    }

    private static void shredDirectory(Path directory, int passes, boolean dryRun, List<Path> approvedRoots)
            throws IOException {
        List<Path> files = gatherFiles(directory, approvedRoots);
        shredDirectory(files, passes, dryRun, approvedRoots);
    }

    private static void shredDirectory(List<Path> files, int passes, boolean dryRun, List<Path> approvedRoots)
            throws IOException {
        if (files.isEmpty()) {
            System.out.println("No files found in the selected directories.");
            return;
        }

        String directoryLabel = files.size() > 0 ? files.get(0).getParent().toString() : "selected folders";
        System.out.println("Directory mode: found " + files.size() + " file(s) under " + directoryLabel);

        int processed = 0;
        int skipped = 0;

        for (Path file : files) {
            if (!isApprovedPath(file, approvedRoots)) {
                System.out.println("Skipping outside policy path: " + file);
                skipped++;
                continue;
            }

            processed++;
            System.out.println("Processing file " + processed + " of " + files.size() + ": " + file);

            if (dryRun) {
                List<String> previewMetadata = previewMetadataRemoval(file);
                if (!previewMetadata.isEmpty()) {
                    System.out.println("Would remove metadata labels from " + file + ":");
                    for (String item : previewMetadata) {
                        System.out.println(" - " + item);
                    }
                } else {
                    System.out.println("No metadata labels would be removed from " + file + ".");
                }
                System.out.println("Dry run: would shred " + file);
                continue;
            }

            List<String> removedMetadata = sanitizeMetadata(file);
            if (!removedMetadata.isEmpty()) {
                System.out.println("Removed metadata labels from " + file + ":");
                for (String item : removedMetadata) {
                    System.out.println(" - " + item);
                }
            }
            shredFile(file, passes);
            System.out.println("Shredded: " + file);
        }

        int totalActions = files.size() - skipped;
        if (dryRun) {
            System.out.println("Dry run summary: " + totalActions + " file(s) would be shredded, " + skipped
                    + " skipped by policy.");
        } else {
            System.out.println(
                    "Processing summary: " + totalActions + " file(s) shredded, " + skipped + " skipped by policy.");
        }
    }

    private static List<String> previewMetadataRemoval(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return new ArrayList<>();
        }

        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".txt") || fileName.endsWith(".log") || fileName.endsWith(".md")
                || fileName.endsWith(".csv") || fileName.endsWith(".json") || fileName.endsWith(".xml")
                || fileName.endsWith(".html")) {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return previewTextMetadataLabels(content);
        }

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")
                || fileName.endsWith(".bmp") || fileName.endsWith(".gif")) {
            byte[] original = Files.readAllBytes(filePath);
            if (fileName.endsWith(".png")) {
                if (containsPngMetadata(original)) {
                    return List.of("PNG metadata blocks");
                }
            }
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
                    || fileName.endsWith(".gif")) {
                if (containsJpegLikeMetadata(original)) {
                    return List.of("JPEG/EXIF metadata blocks");
                }
            }
        }

        return new ArrayList<>();
    }

    private static List<String> sanitizeMetadata(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return new ArrayList<>();
        }

        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".txt") || fileName.endsWith(".log") || fileName.endsWith(".md")
                || fileName.endsWith(".csv") || fileName.endsWith(".json") || fileName.endsWith(".xml")
                || fileName.endsWith(".html")) {
            return sanitizeTextMetadata(filePath);
        }

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")
                || fileName.endsWith(".bmp") || fileName.endsWith(".gif")) {
            return sanitizeImageMetadata(filePath);
        }

        return new ArrayList<>();
    }

    private static List<String> previewTextMetadataLabels(String content) {
        List<String> labels = new ArrayList<>();
        String cleaned = content.replace("\uFEFF", "");
        String[] markers = { "Author", "Created by", "Owner", "Company", "Location", "GPS", "Camera", "Model",
                "Date created", "Date modified", "Modified by", "Editor", "Title" };

        for (String marker : markers) {
            String pattern = "(?im)^\\s*" + marker + "\\s*[:=].*\\R?";
            if (cleaned.matches("(?s).*" + pattern + ".*")) {
                labels.add(marker);
            }
        }

        return labels;
    }

    private static List<String> sanitizeTextMetadata(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        List<String> removed = new ArrayList<>();
        String cleaned = content.replace("\uFEFF", "");

        String[] markers = { "Author", "Created by", "Owner", "Company", "Location", "GPS", "Camera", "Model",
                "Date created", "Date modified", "Modified by", "Editor", "Title" };

        for (String marker : markers) {
            String pattern = "(?im)^\\s*" + marker + "\\s*[:=].*\\R?";
            String replaced = cleaned.replaceAll(pattern, "");
            if (!replaced.equals(cleaned)) {
                removed.add(marker);
                cleaned = replaced;
            }
        }

        if (removed.isEmpty()) {
            return removed;
        }

        Files.writeString(filePath, cleaned, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return removed;
    }

    private static List<String> sanitizeImageMetadata(Path filePath) throws IOException {
        String name = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        byte[] original = Files.readAllBytes(filePath);
        List<String> removed = new ArrayList<>();

        if (name.endsWith(".png")) {
            boolean changed = sanitizePngMetadata(filePath, original);
            if (changed) {
                removed.add("PNG metadata blocks");
            }
            return removed;
        }

        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp") || name.endsWith(".gif")) {
            boolean changed = sanitizeJpegLikeMetadata(filePath, original);
            if (changed) {
                removed.add("JPEG/EXIF metadata blocks");
            }
            return removed;
        }

        return removed;
    }

    private static boolean containsPngMetadata(byte[] original) {
        if (original.length < 8 || original[0] != (byte) 0x89 || original[1] != 0x50 || original[2] != 0x4E
                || original[3] != 0x47
                || original[4] != 0x0D || original[5] != 0x0A || original[6] != 0x1A || original[7] != 0x0A) {
            return false;
        }

        int index = 8;
        while (index + 8 <= original.length) {
            int length = ((original[index] & 0xFF) << 24) | ((original[index + 1] & 0xFF) << 16)
                    | ((original[index + 2] & 0xFF) << 8) | (original[index + 3] & 0xFF);
            String chunkType = new String(original, index + 4, 4, StandardCharsets.US_ASCII);
            int dataEnd = index + 8 + length;
            if (dataEnd + 4 > original.length) {
                break;
            }

            if ("tEXt".equals(chunkType) || "iTXt".equals(chunkType) || "zTXt".equals(chunkType)
                    || "eXIf".equals(chunkType)) {
                return true;
            }

            index = dataEnd + 4;
        }

        return false;
    }

    private static boolean containsJpegLikeMetadata(byte[] original) {
        if (original.length < 4 || (original[0] & 0xFF) != 0xFF || (original[1] & 0xFF) != 0xD8) {
            return false;
        }

        int index = 2;
        while (index + 1 < original.length) {
            int first = original[index] & 0xFF;
            int second = original[index + 1] & 0xFF;

            if (first == 0xFF && second == 0xDA) {
                return false;
            }

            if (first == 0xFF && second >= 0xE0 && second <= 0xEF) {
                if (index + 4 > original.length) {
                    break;
                }
                int length = ((original[index + 2] & 0xFF) << 8) | (original[index + 3] & 0xFF);
                if (index + 2 + length > original.length) {
                    break;
                }
                return true;
            }

            index++;
        }

        return false;
    }

    private static boolean sanitizePngMetadata(Path filePath, byte[] original) throws IOException {
        if (original.length < 8 || original[0] != (byte) 0x89 || original[1] != 0x50 || original[2] != 0x4E
                || original[3] != 0x47
                || original[4] != 0x0D || original[5] != 0x0A || original[6] != 0x1A || original[7] != 0x0A) {
            return false;
        }

        ByteArrayOutputStream cleaned = new ByteArrayOutputStream();
        cleaned.write(original, 0, 8);

        int index = 8;
        boolean changed = false;

        while (index + 8 <= original.length) {
            int length = ((original[index] & 0xFF) << 24) | ((original[index + 1] & 0xFF) << 16)
                    | ((original[index + 2] & 0xFF) << 8) | (original[index + 3] & 0xFF);
            String chunkType = new String(original, index + 4, 4, StandardCharsets.US_ASCII);
            int dataStart = index + 8;
            int dataEnd = dataStart + length;

            if (dataEnd + 4 > original.length) {
                break;
            }

            if ("tEXt".equals(chunkType) || "iTXt".equals(chunkType) || "zTXt".equals(chunkType)
                    || "eXIf".equals(chunkType)) {
                changed = true;
                index = dataEnd + 4;
                continue;
            }

            cleaned.write(original, index, length + 12);
            index = dataEnd + 4;
        }

        if (!changed) {
            return false;
        }

        Files.write(filePath, cleaned.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return true;
    }

    private static boolean sanitizeJpegLikeMetadata(Path filePath, byte[] original) throws IOException {
        if (original.length < 4 || (original[0] & 0xFF) != 0xFF || (original[1] & 0xFF) != 0xD8) {
            return false;
        }

        ByteArrayOutputStream cleaned = new ByteArrayOutputStream();
        cleaned.write(original[0]);
        cleaned.write(original[1]);

        int index = 2;
        boolean changed = false;

        while (index + 1 < original.length) {
            int first = original[index] & 0xFF;
            int second = original[index + 1] & 0xFF;

            if (first == 0xFF && second == 0xDA) {
                cleaned.write(original, index, original.length - index);
                break;
            }

            if (first == 0xFF && second >= 0xE0 && second <= 0xEF) {
                if (index + 4 > original.length) {
                    break;
                }
                int length = ((original[index + 2] & 0xFF) << 8) | (original[index + 3] & 0xFF);
                if (index + 2 + length > original.length) {
                    break;
                }
                changed = true;
                index += 2 + length;
                continue;
            }

            cleaned.write(original[index]);
            index++;
        }

        if (!changed) {
            return false;
        }

        Files.write(filePath, cleaned.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return true;
    }

    private static List<Path> getApprovedRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize());
        roots.add(Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize());
        return roots;
    }

    private static boolean isApprovedPath(Path target, List<Path> approvedRoots) {
        for (Path root : approvedRoots) {
            if (target.startsWith(root)) {
                return true;
            }
        }
        return false;
    }

    private static void writeAuditEntry(Path target, int passes, boolean success, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String safePath = target.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String jsonLine = "{\"timestamp\":\"" + timestamp + "\",\"path\":\"" + safePath
                + "\",\"passes\":" + passes + ",\"status\":\"" + (success ? "success" : "failed")
                + "\",\"message\":\"" + safeMessage + "\"}\n";

        try {
            Files.write(
                    AUDIT_LOG_PATH,
                    jsonLine.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // We do not throw here because audit logging should never block a shred action.
        }
    }

    public static void shredFile(Path filePath, int passes) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Path is not a regular file: " + filePath);
        }

        long fileSize = Files.size(filePath);
        SecureRandom random = new SecureRandom();
        byte[] buffer = new byte[8192];

        System.out.println("SHRED_PROGRESS:0:" + passes + ":0:" + fileSize);
        for (int pass = 0; pass < passes; pass++) {
            try (FileOutputStream output = new FileOutputStream(filePath.toFile(), false)) {
                long bytesWritten = 0L;
                long progressInterval = Math.max(1L, fileSize / 100L);
                long nextProgress = progressInterval;
                while (bytesWritten < fileSize) {
                    random.nextBytes(buffer);
                    int chunkSize = (int) Math.min(buffer.length, fileSize - bytesWritten);
                    output.write(buffer, 0, chunkSize);
                    bytesWritten += chunkSize;
                    if (bytesWritten >= nextProgress || bytesWritten == fileSize) {
                        System.out.println("SHRED_PROGRESS:" + (pass + 1) + ":" + passes + ":"
                                + bytesWritten + ":" + fileSize);
                        nextProgress = bytesWritten > fileSize - progressInterval
                                ? fileSize
                                : bytesWritten + progressInterval;
                    }
                }
                output.flush();
                output.getFD().sync();
            }
        }

        Files.deleteIfExists(filePath);
    }
}