package com.adam.fileprocessor;

import com.adam.fileprocessor.entities.PastGames;
import com.adam.fileprocessor.entities.Player;
import com.adam.fileprocessor.exceptions.FormatNotValidException;
import com.adam.fileprocessor.exceptions.PastGamesException;
import com.adam.fileprocessor.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Parser implements Callable {

    Logger logger = LoggerFactory.getLogger(Parser.class);

    private final String filepath;
    private final String filename;
    private final LocalDateTime startTime;
    private final BusinessService businessService;
    private final PlayerRepository playerRepository;
    private final int firstline;
    private final int lastline;

    public Parser(String filepath, LocalDateTime startTime, BusinessService businessService, PlayerRepository playerRepository, int firstline, int lastline) {
        Pattern stringPattern = Pattern.compile(".*(FILE_.*\\.csv)");

        Matcher m = stringPattern.matcher(filepath);

        if (m.find()) {
            this.filename = m.group(1);
        }
        else {
            this.filename = "";
        }

        this.filepath = filepath;
        this.startTime = startTime;
        this.businessService = businessService;
        this.playerRepository = playerRepository;
        this.firstline = firstline;
        this.lastline = lastline;
    }

    @Override
    public Map<String, Set<String>> call() {

        Set<String> games = new HashSet<>();
        Set<String> failed = new HashSet<>();
        Map<String, Set<String>> result = new HashMap<>();
        Map<String, String> ready = new HashMap<>();

        result.put("Games", games);
        result.put("Failed", failed);

        InputStream io = null;
        try {
            io = new FileInputStream(filepath);
        } catch (FileNotFoundException e) {
            logger.error("File not found: " + filepath, e);
            return result;
        }

        BufferedReader csvReader = new BufferedReader(new InputStreamReader(io, StandardCharsets.UTF_8));

        Integer INDEX_PAST_GAMES = null;
        Integer INDEX_GAME = null;
        Integer INDEX_PLAYER = null;

        String regPastGames = "(PastGames)(.*)";
        String regGame = "(Game)(.*)";
        String regPlayer = "(Player)(.*)";
        Pattern patternPastGames = Pattern.compile(regPastGames);
        Pattern patternGame = Pattern.compile(regGame);
        Pattern patternPlayer = Pattern.compile(regPlayer);

        String line = null;
        try {
            line = csvReader.readLine();
        } catch (IOException e) {
            logger.error("No header found: " + filepath, e);
            return result;
        }
        List<String> col = CsvUtils.parseCsvRecord(line, ';');

        int index = 0;
        for(String column : col) {
            Matcher matcherPastGames = patternPastGames.matcher(column);
            Matcher matcherGame = patternGame.matcher(column);
            Matcher matcherPlayer = patternPlayer.matcher(column);

            if(matcherPastGames.find() && INDEX_PAST_GAMES==null) INDEX_PAST_GAMES = index;
            if(matcherGame.find() && INDEX_GAME==null) INDEX_GAME = index;
            if(matcherPlayer.find()) INDEX_PLAYER = index;

            index++;
        }

        if(INDEX_PAST_GAMES == null) {
            logger.error("File read error no INDEX_PAST_GAMES: " + filepath);
            return result;
        }
        if(INDEX_GAME == null) {
            logger.error("File read error no INDEX_GAME: " + filepath);
            return result;
        }
        if(INDEX_PLAYER == null) {
            logger.debug("File read error no INDEX_PLAYER: " + filepath);
        }

        List<Player> players = playerRepository.findAll();
        List<String> playesrList = players.stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        logger.info("File: {} | Rows[{}-{}] - START", filepath, firstline, lastline);
        int currentLine = 1;
        while (true) {
            try {
                if ((line = csvReader.readLine()) == null) break;
            } catch (IOException e) {
                logger.error("File read error: " + filepath, e);
                return result;
            }
            currentLine++;
            if (currentLine < firstline) {
                continue;
            }
            if (currentLine > lastline) {
                break;
            }

            List<String> row = CsvUtils.parseCsvRecord(line, ';');

            if (row.size() == 0) {
                continue;
            }

            games.add(row.get(INDEX_GAME));
            ready.put(row.get(INDEX_GAME), row.get(INDEX_PAST_GAMES));

            List<PastGames> pastGames = null;
            try {
                pastGames = CsvUtils.parsePastGames(
                        row.get(INDEX_PAST_GAMES),
                        Long.parseLong(row.get(INDEX_GAME)),
                        this.startTime.toLocalDate(),
                        filename,
                        filepath
                        );
            } catch (InterruptedException e) {
                logger.error("Thread stopped" +
                        "\nRow["+currentLine+"]: " + row +
                        "\nFile: " + filepath, e);
                failed.add(row.get(INDEX_GAME));
                continue;
            } catch (FormatNotValidException e) {
                logger.error("Row["+currentLine+"]: " + row +
                        "\nFile: " + filepath, e);
                failed.add(row.get(INDEX_GAME));
                continue;
            }  catch (IndexOutOfBoundsException e) {
                logger.error("Index (" + INDEX_PAST_GAMES + ")" +
                        "\nRow["+currentLine+"]: " + row +
                        "\nFile: " + filepath, e);
                failed.add(row.get(INDEX_GAME));
                continue;
            } catch (PastGamesException e) {
                logger.error("Row["+currentLine+"]: " + row +
                        "\nFile: " + filepath, e);
                failed.add(row.get(INDEX_GAME));
                continue;
            }

            try {
                businessService.getDuration(
                        row.get(INDEX_GAME),
                        row.get(INDEX_PAST_GAMES),
                        pastGames,
                        filename,
                        this.startTime.truncatedTo(ChronoUnit.SECONDS),
                        playesrList
                );
            } catch (Exception e) {
                logger.error("Business error:\n" +
                        "request = "+row.get(INDEX_GAME) + "\n" +
                        "filename = "+filename+ "\n\n",
                        e
                );
            }


        }
        logger.info("File: {} | Rows[{}-{}] - END", filepath, firstline, lastline);

        games=ready.keySet();
        result.get("Games").addAll(games);
        result.get("Failed").addAll(failed);
        return result;
    }


}
