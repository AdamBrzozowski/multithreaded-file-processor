package com.adam.fileprocessor;

import com.adam.fileprocessor.entities.PastGames;
import com.adam.fileprocessor.exceptions.PaperNotFoundException;
import com.adam.fileprocessor.exceptions.FormatNotValidException;
import com.adam.fileprocessor.exceptions.PastGamesException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
@Slf4j
public class CsvUtils {
    public static void convertExcelToCSV(String filenameEXCEL, String filenameCSV) throws IOException, PaperNotFoundException {
        FileInputStream fileInStream = null;
        DataFormatter formatter = new DataFormatter();

        fileInStream = new FileInputStream(filenameEXCEL);

        XSSFWorkbook workBook = new XSSFWorkbook(fileInStream);

        XSSFSheet selSheet = null;

        selSheet = workBook.getSheet("Paper");
        if(selSheet == null) {
            try {
                selSheet = workBook.getSheetAt(0);
            } catch (IllegalArgumentException e) {
                throw new PaperNotFoundException("No paper found");
            }
        }

        // Loop through all the rows
        Iterator rowIterator = selSheet.iterator();
        StringBuffer stringBuffer = new StringBuffer();
        while (rowIterator.hasNext()) {
            Row row = (Row) rowIterator.next();
            // Loop through all rows and add ";"
            Iterator cellIterator = row.cellIterator();
            int column = 0;
            while (cellIterator.hasNext()) {
                Cell cell = (Cell) cellIterator.next();
                // Skipped cells?
                while(column != cell.getColumnIndex()) {
                    stringBuffer.append(";");
                    column++;
                }

                stringBuffer.append(formatter.formatCellValue(cell));
                stringBuffer.append(";");
                column++;

            }
            stringBuffer.append("\n");
        }
        BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(filenameCSV)));

        //write contents of StringBuffer to a file
        bwr.write(stringBuffer.toString());

        //flush the stream
        bwr.flush();

        //close the stream
        bwr.close();
        workBook.close();

    }

    public static List<String> parseCsvRecord(String record, char csvSeparator) {

        // Prepare.
        boolean quoted = false;
        StringBuilder fieldBuilder = new StringBuilder();
        List<String> fields = new ArrayList<>();

        // Process fields.
        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);
            fieldBuilder.append(c);

            if (c == '"') {
                quoted = !quoted; // Detect nested quotes.
            }

            if ((!quoted && c == csvSeparator) // The separator ..
                    || i + 1 == record.length()) // .. or, the end of record.
            {
                String field = fieldBuilder.toString() // Obtain the field, ..
                        .replaceAll(csvSeparator + "$", "") // .. trim ending separator, ..
                        .replaceAll("^\"|\"$", "") // .. trim surrounding quotes, ..
                        .replace("\"\"", "\""); // .. and un-escape quotes.
                fields.add(field.trim()); // Add field to List.
                fieldBuilder = new StringBuilder(); // Reset.
            }
        }

        return fields;
    }

    public static List<PastGames> parsePastGames(
            String csv,
            long game,
            LocalDate startTime,
            String filename,
            String filepath
    ) throws InterruptedException, FormatNotValidException, PastGamesException {
        List<PastGames> pastGames = new ArrayList<>();

        String regRow = "(?=(([0-9]{2}\\/){2}[0-9]{4} ([0-9]{2}\\:){2}[0-9]{2})( \\w* ))";

        String[] rows = csv.split(regRow);

        // Parse Data
        String regData = "(([0-9]{2}\\/){2}[0-9]{4} ([0-9]{2}\\:){2}[0-9]{2})";
        Pattern patternData = Pattern.compile(regData);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        String regCode = "(([0-9]{2}\\/){2}[0-9]{4} ([0-9]{2}\\:){2}[0-9]{2})(.+?)(-* *AUTO *)*(?=((?i)Player))";
        Pattern patternCode = Pattern.compile(regCode);

        String regSpaces = "(.+?)(  )";
        Pattern patternSces = Pattern.compile(regSpaces);

        String regPlayer = "((?i)Player)([\\s]*:[\\w \\s\\-\\/]*\\.)";

        String regPartialPlayes = "((?i)Player)([\\s]*:[\\s]*)";
        Pattern patternPlayer = Pattern.compile(regPlayer);
        Pattern patternPartialPlayer = Pattern.compile(regPartialPlayes);

        String regState = "(State)([\\s]*[\\w]*)";
        String regStatePartial = "(State)";
        Pattern patternState = Pattern.compile(regState);
        Pattern patternStatePartial = Pattern.compile(regStatePartial);

        for (String row : rows) {
            Matcher matcherData = patternData.matcher(row);
            Matcher matcherCode = patternCode.matcher(row);
            Matcher matcherPlayer = patternPlayer.matcher(row);
            Matcher matcherState = patternState.matcher(row);

            if (matcherPlayer.find() && matcherState.find()) {
                PastGames rowPastGames = new PastGames();
                rowPastGames.setGame(game);
                if (matcherData.find() && matcherCode.find()) {
                    rowPastGames.setTimestamp(LocalDateTime.parse(matcherData.group(), formatter));

                    rowPastGames.setInsertDate(startTime);

                    String code = matcherCode.group(4).trim();
                    Matcher matcherSpazi = patternSces.matcher(code);
                    if(matcherSpazi.find()) {
                        code = matcherSpazi.group(1);
                    }
                    rowPastGames.setCode(code);

                    String stringPlayer = matcherPlayer.group();
                    Matcher matcherPartialPlayer = patternPartialPlayer.matcher(stringPlayer);
                    matcherPartialPlayer.find();
                    int indexEndPlayer = stringPlayer.length() - 1;
                    int indexStartPlayer = matcherPartialPlayer.end();
                    String player = stringPlayer.substring(indexStartPlayer, indexEndPlayer);

                    rowPastGames.setPlayer(player);

                    String stringState = matcherState.group();
                    Matcher matcherStatePartial = patternStatePartial.matcher(stringState);
                    matcherStatePartial.find();
                    int indexEndState = stringState.length();
                    int indexStartState = matcherStatePartial.end() + 1;

                    String state = stringState.substring(indexStartState, indexEndState);

                    rowPastGames.setState(state);

                } else {
                    throw new FormatNotValidException("Format not valid");
                }

                if(rowPastGames.getTimestamp() == null || rowPastGames.getPlayer() == null ||
                        rowPastGames.getGame() == null) {
                    throw new PastGamesException("Past games exception" + " - Values: " + rowPastGames.toStringValues());
                }

                rowPastGames.setFile(filename);
                pastGames.add(rowPastGames);

            } else {
                log.info("WARNING ignored row", filepath, game, row);
            }
        }

        return pastGames;
    }
}
