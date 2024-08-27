package com.adam.fileprocessor;

import com.adam.fileprocessor.exceptions.PaperNotFoundException;
import com.adam.fileprocessor.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class FileMonitor implements Runnable {

    Logger logger = LoggerFactory.getLogger(FileMonitor.class);

    private final Environment env;
    private final BusinessService businessService;
    private final PlayerRepository playerRepository;


    public FileMonitor(Environment env, BusinessService businessService, PlayerRepository playerRepository) {
        this.env = env;
        this.businessService = businessService;
        this.playerRepository = playerRepository;
    }


    @Override
    public void run() {
        logger.info("File watch start");

        WatchService watchService;

        Path path = Paths.get(Objects.requireNonNull(env.getProperty("folder.path")));
        Pattern filenameOk = Pattern.compile(Objects.requireNonNull(env.getProperty("filename.regex")));

        try {
            watchService = FileSystems.getDefault().newWatchService();

            path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE
            );
        } catch (IOException e) {
            logger.error("Error during watcher inizialization: {}",e.getLocalizedMessage());
            return;
        }

        WatchKey key;


            while (true) {
                try {
                    if ((key = watchService.take()) == null) break;
                } catch (InterruptedException e) {
                    logger.error(e.getLocalizedMessage());
                    return;
                }

                key.pollEvents().stream()
                        .filter(event -> event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE) && filenameOk.matcher(event.context().toString()).matches())
                        .forEach(event -> {

                            String filenameCSV = event.context().toString().split("\\.")[0] + ".csv";

                            File file = new File(path + File.separator + filenameCSV);
                            if (!file.isFile()) {
                                String filenameEXCEL = event.context().toString().split("\\.")[0] + ".xlsx";

                                try {
                                    CsvUtils.convertExcelToCSV(path + File.separator + filenameEXCEL, path + File.separator + filenameCSV);
                                } catch (IOException e) {
                                    logger.error("Error file conversion: " + filenameEXCEL, e);
                                    return;
                                } catch (PaperNotFoundException e) {
                                    logger.error("Error file format [" + filenameEXCEL + "]", e);
                                    return;
                                }
                            }

                            // START THREAD PREPARATION ------------------------------------------------------------------
                            String filename = path + File.separator + filenameCSV;
                            LineNumberReader lineNumberReader = null;
                            int cpuCount;
                            int lines;
                            int rowsEachThread;
                            try {
                                lineNumberReader = new LineNumberReader(new FileReader(filename));

                                lineNumberReader.skip(Long.MAX_VALUE);
                                lines = lineNumberReader.getLineNumber();
                                lineNumberReader.close();

                                cpuCount = Runtime.getRuntime().availableProcessors();
                                rowsEachThread = (int) Math.ceil((double) lines / cpuCount);
                                logger.info("Rows {}: {} - CPU: {} - Rows for each CPU: {}", filename, lines-1, cpuCount, rowsEachThread);

                            } catch (Exception e) {
                                logger.error("Error thread preparation: \n", e);
                                return;
                            }

                            ExecutorService threadPool = Executors.newFixedThreadPool(cpuCount);
                            List<Future<Map<String,Set<String>>>> list = new ArrayList<Future<Map<String,Set<String>>>>();
                            int start = 0;
                            int offset = rowsEachThread;
                            LocalDateTime startTime = LocalDateTime.now();

                            for (int threadCount = 0; threadCount < cpuCount; threadCount++) {
                                int end = (start + offset == 1) ? (start + offset) : (start + offset);
                                if (end > lines) {
                                    end = lines;
                                }
                                Parser parser = new Parser(filename, startTime, businessService, playerRepository, start+1, end);
                                logger.info("Thread scheduled - Working on file {} - Lines[{}-{}]", filenameCSV, start+1, end);
                                start = start + offset;
                                Future<Map<String,Set<String>>> submit = null;
                                    submit = threadPool.submit(parser);
                                list.add(submit);
                            }
                            Set<String> totGames = new HashSet<>();
                            Set<String> totFailed = new HashSet<>();
                            for (Future<Map<String,Set<String>>> futureSet : list) {
                                try {
                                    Map<String,Set<String>> res = futureSet.get();
                                    totGames.addAll(res.get("Games"));
                                    totFailed.addAll(res.get("Failed"));
                                } catch (Exception e) {
                                    logger.error("Error thread: ", e);
                                    return;
                                }
                            }
                            if (totGames.size() + totFailed.size() != lines - 1) {
                                logger.info("Empty rows found - games: {}, rows: {}", totGames.size(), lines - 1);
                            }
                            if (totFailed.size() != 0) {
                                logger.info("Filed: {}", totFailed);
                            }

                            // Disable new tasks from being submitted
                            threadPool.shutdown();
                            try {
                                // Wait a while for existing tasks to terminate
                                if (!threadPool.awaitTermination(50, TimeUnit.MINUTES)) {
                                    // Cancel currently executing tasks
                                    logger.info("File: {} | Shutting down threads", filename);
                                    threadPool.shutdownNow();
                                }
                            } catch (InterruptedException ie) {
                                // (Re-)Cancel if current thread also interrupted
                                threadPool.shutdownNow();
                            }

                            int totSeconds = (int)ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
                            int hours = totSeconds / 3600;
                            int minutes = (totSeconds % 3600) / 60;
                            int seconds = totSeconds % 60;
                            String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

                            logger.info("Time: {}", time);

                        });
                key.reset();
            }

    }
}