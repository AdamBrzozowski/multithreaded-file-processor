package com.adam.fileprocessor;

import com.adam.fileprocessor.entities.GameDuration;
import com.adam.fileprocessor.exceptions.PaperNotFoundException;
import com.adam.fileprocessor.repository.PlayerRepository;
import com.adam.fileprocessor.repository.ResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
@ActiveProfiles({"TestNoWatcher", "test"})
@Slf4j
class FileProcessorApplicationTests {

	@Autowired
	private BusinessService businessService;

	@Autowired
	private ResultRepository resultRepository;

	@SpyBean
	private PlayerRepository playerRepository;

	private final String PATH = "src/test/resources/files";

	@Test
	void test() {
		doReturn(playerRepository.findAll()).when(playerRepository).findAll();

		List<String> files = new ArrayList<>();

		List<String> filenames = new ArrayList<>();

		String filename1 = "FILE_1";
//		String filename2 = "FILE_2";
//		String filename3 = "FILE_3";

		filenames.add(filename1);
//		filenames.add(filename2);
//		filenames.add(filename3);

		for(String fileName : filenames) {
			String filenameCSV = fileName + ".csv";
			resultRepository.deleteFile(filenameCSV);

			File fileCsv = new File(PATH + File.separator + filenameCSV);

			if (!fileCsv.isFile()) {
				String filenameEXCEL = fileName + ".xlsx";

				try {
					CsvUtils.convertExcelToCSV(PATH + File.separator + filenameEXCEL, PATH + File.separator + filenameCSV);
				} catch (IOException e) {
					log.error("Converion error: " + filenameEXCEL, e);
					continue;
				} catch (PaperNotFoundException e) {
					log.error("Error file [" + filenameEXCEL + "] paper not found", e);
					continue;
				}
			}
			files.add(PATH + File.separator + filenameCSV);
		}

		for (String filename : files) {
			calculate(filename);
		}

		int gamesError = 0;
		int pastGamesError = 0;
		List<List<GameDuration>> gameFailed = new ArrayList<>();
		List<GameDuration> pastGamesFiled = new ArrayList<>();

		for(String fileName : filenames) {
			String filenameCSV = fileName + ".csv";
			String filenameTest = fileName + "_test_junit";

			List<GameDuration> durationOk = resultRepository.selectFile(filenameTest);
			List<GameDuration> durationCheck = resultRepository.selectFile(filenameCSV);

			for (GameDuration gameDurationCheck : durationCheck) {

				List<GameDuration> durationCorrect = durationOk.stream()
						.filter(d -> d.getGame().equals(gameDurationCheck.getGame()))
						.collect(Collectors.toList());

//				if(durationCorrect.size()!=1) {
//					log.error("Error more than 1 element: " + gameDurationCheck.getGame());
//				}

				// TODO: add expected results to db
//				if(!gameDurationCheck.getDuration().equals(durationCorrect.get(0).getDuration()) ||
//						(
//							!(gameDurationCheck.getDurationOther()!=null? gameDurationCheck.getDurationOther():"")
//									.equals(
//											durationCorrect.get(0).getDurationOther()!=null?durationCorrect.get(0).getDurationOther():""
//									)
//						)
//				) {
//					List<GameDuration> errors = new ArrayList<>();
//					errors.add(gameDurationCheck);
//					errors.add(durationCorrect.get(0));
//					gameFailed.add(errors);
//					gamesError++;
//				}
//
//				if(!gameDurationCheck.getPastGames().equals(durationCorrect.get(0).getPastGames())) {
//					pastGamesError++;
//					pastGamesFiled.add(gameDurationCheck);
//				}

			}

		}


//		if(gamesError!=0) {
//			System.out.println("List ERRORS: ");
//			System.out.println("GAME  |  EXPECTED  |  FOUND \n\n");
//			for(List<GameDuration> failed : gameFailed) {
//				GameDuration gameDurationCorrect = failed.get(1);
//				GameDuration gameDurationFailed = failed.get(0);
//				System.out.println("Players\n");
//				System.out.println(gameDurationFailed.getGame() + "  |  "
//						+ gameDurationCorrect.getDuration() + "  |  "
//						+ gameDurationFailed.getDuration() + "\n\n"
//				);
//				System.out.println("Other\n");
//				System.out.println(gameDurationFailed.getGame() + "  |  "
//						+ (gameDurationCorrect.getDurationOther()!=null? gameDurationCorrect.getDurationOther():"null") + "  |  "
//						+ (gameDurationFailed.getDurationOther()!=null? gameDurationFailed.getDurationOther():"null")
//				);
//				System.out.println("\n--------------------------\n");
//			}
//		}
//
//		if(pastGamesError!=0) {
//			System.out.println("\n_______________________________________________________________________________________________");
//			System.out.println("_______________________________________________________________________________________________\n\n");
//			System.out.println("List GAMES ERRORS past games: ");
//			for(GameDuration pastGameFailed : pastGamesFiled) {
//				System.out.println(pastGameFailed.getGame());
//			}
//			System.out.println("\n_______________________________________________________________________________________________\n\n");
//		}

		assertEquals(files.size(), filenames.size());
		assertEquals(0,gamesError);
		assertEquals(0,pastGamesError);

	}

	private void calculate(String filename) {
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
			log.info("Rows {}: {} - CPU: {} - Rows for CPU: {}", filename, lines-1, cpuCount, rowsEachThread);

		} catch (Exception e) {
			log.error("Error count rows: \n", e);
			return;
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(cpuCount);
		List<Future<Map<String, Set<String>>>> list = new ArrayList<Future<Map<String,Set<String>>>>();
		int start = 0;
		int offset = rowsEachThread;
		LocalDateTime startTime = LocalDateTime.now();

		for (int threadCount = 0; threadCount < cpuCount; threadCount++) {
			int end = (start + offset == 1) ? (start + offset) : (start + offset);
			if (end > lines) {
				end = lines;
			}
			Parser parser = new Parser(filename, startTime, businessService, playerRepository, start+1, end);
			log.info("Thread scheduled - Working on file {} - Lines[{}-{}]", filename, start+1, end);
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
				log.error("Errore thread: ", e);
				return;
			}
		}
		if (totGames.size() + totFailed.size() != lines - 1) {
			log.info("Bad rows - games: {}, rows: {}", totGames.size(), lines - 1);
		}
		if (totFailed.size() != 0) {
			log.info("Failed: {}", totFailed);
		}

		// Disable new tasks from being submitted
		threadPool.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!threadPool.awaitTermination(50, TimeUnit.MINUTES)) {
				// Cancel currently executing tasks
				log.info("File: {} | Shutting down threads", filename);
				threadPool.shutdownNow();
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			threadPool.shutdownNow();
		}

		int resSecs = (int) ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
		int hours = resSecs / 3600;
		int minutes = (resSecs % 3600) / 60;
		int seconds = resSecs % 60;
		String duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);

		log.info("Exec time: {}", duration);
	}

}
