package com.adam.fileprocessor;

import com.adam.fileprocessor.entities.GameDuration;
import com.adam.fileprocessor.entities.PastGames;
import com.adam.fileprocessor.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class BusinessService {

    @Value("#{${holidays}}")
    private List<String> holidays;

    private final String STATE_START = "Start";
    private final String STATE_ENDED = "Ended";

    List<String> datesHoliday = Arrays.asList(
            "01/01",
            "01/06",
            "04/25",
            "05/01",
            "06/02",
            "08/15",
            "11/01",
            "12/08",
            "12/25",
            "12/26"
    );

    private final ResultRepository resultRepository;

    public String getDuration(String game, String csv, List<PastGames> pastGames, String filename, LocalDateTime creation, List<String> players) {

        Long code = Long.valueOf(game);

        GameDuration res = GameDuration.builder()
                .game(code)
                .creationDate(creation)
                .file(filename)
                .pastGames(csv)
                .build();


        Collections.sort(pastGames, (o1, o2) ->
        {
            if(ChronoUnit.SECONDS.between(o2.getTimestamp(), o1.getTimestamp()) != 0) {
                return (int) ChronoUnit.SECONDS.between(o2.getTimestamp(), o1.getTimestamp());
            }

            return -1;
        });

        int min = getDuration(pastGames, players);

        res.setDuration(String.format("%02d", min));

        log.debug("req: {} - life {}", game, res.getDuration());

        Set<String> other = pastGames.stream()
                .map(PastGames::getPlayer)
                .filter(player -> !players.contains(player))
                .collect(Collectors.toSet());

        int durationTot = min;

        res.setDurationOther("");
        for (String player : other) {
            min = getDuration(pastGames, Collections.singletonList(player));
            if(min > 0) {
                String life = String.format("%02d", min);
                durationTot += min;
                String residual = res.getDurationOther();

                residual = residual != null ? residual + ", " + player + " - " + life : player + " - " + life;

                res.setDurationOther(residual);
            }

        }

        res.setDurationTot(String.valueOf(durationTot));


        resultRepository.save(res);

        return res.getDuration();

    }

    private int getDuration(List<PastGames> pastGames, List<String> players) {
        // Implement custom Logic
        int duratino = 0;
        LocalDateTime dateStart = pastGames.get(0).getTimestamp();
        LocalDateTime dateEnd = pastGames.get(0).getTimestamp();

        for (PastGames rowPastGames : pastGames) {
            String playerNow = rowPastGames.getPlayer();
            LocalDateTime datePlayer = rowPastGames.getTimestamp();
            String statePlayer = rowPastGames.getState();

            if (players.contains(playerNow)) {
                if (statePlayer.equals(STATE_START)) {
                    dateStart = datePlayer;
                }

                if (statePlayer.equals(STATE_ENDED)) {
                    dateEnd = datePlayer;
                    duratino += getPartialDuration(dateStart, dateEnd);
                }

            }
        }
        return duratino / 60;
    }

    private int getPartialDuration(LocalDateTime dateStart, LocalDateTime dateEnd) {
        if(dateStart.isEqual(dateEnd)) {
            return 0;
        }

        DateTimeFormatter formatterHolidays = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterHolidayFixed = DateTimeFormatter.ofPattern("MM/dd");
        List<LocalDate> dateHolidays = new ArrayList<>();

        for (String date : holidays) {
            dateHolidays.add(LocalDate.parse(date, formatterHolidays));
        }

        int resSecond = 0;
        int businessDays = 0;
        LocalDate start = LocalDate.from(dateStart);
        LocalDate end = LocalDate.from(dateEnd);

        LocalTime timeStart = LocalTime.of(7, 0);
        LocalDateTime baginningDay = LocalDateTime.of(start, timeStart);

        LocalTime timeEnd = LocalTime.of(20, 0);
        LocalDateTime endDay = LocalDateTime.of(end, timeEnd);

        if (dateStart.getHour() >= 20) {
            dateStart = baginningDay.plusDays(1);
            baginningDay = dateStart;
        } else if (dateStart.getHour() < 7) {
            dateStart = baginningDay;
        }

        if (dateEnd.getHour() >= 20) {
            dateEnd = endDay;
        } else if (dateEnd.getHour() < 7) {
            dateEnd = endDay.minusDays(1);
            endDay = dateEnd;
        }

        while (start.isBefore(end.plusDays(1))) {
            DayOfWeek day = start.getDayOfWeek();

            String dateFormatted = start.format(formatterHolidayFixed);
            if (!dateHolidays.contains(start)
                    && !datesHoliday.contains(dateFormatted)
                    && day != DayOfWeek.SATURDAY
                    && day != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            start = start.plusDays(1);
        }

        if (businessDays > 0) {
            resSecond = businessDays * 13 * 60 * 60;

            DayOfWeek dayOfWeekStart = dateStart.getDayOfWeek();
            String startFormatted = dateStart.format(formatterHolidayFixed);
            if (!dateHolidays.contains(dateStart)
                    && !datesHoliday.contains(startFormatted)
                    && dayOfWeekStart != DayOfWeek.SATURDAY
                    && dayOfWeekStart != DayOfWeek.SUNDAY) {

                resSecond -= ChronoUnit.SECONDS.between(baginningDay, dateStart);
            }

            DayOfWeek dayOfWeekEnd = dateEnd.getDayOfWeek();
            startFormatted = dateEnd.format(formatterHolidayFixed);
            if (!dateHolidays.contains(dateEnd)
                    && !datesHoliday.contains(startFormatted)
                    && dayOfWeekEnd != DayOfWeek.SATURDAY
                    && dayOfWeekEnd != DayOfWeek.SUNDAY) {

                resSecond -= ChronoUnit.SECONDS.between(dateEnd, endDay);
            }
        } else {
            resSecond = (int) ChronoUnit.SECONDS.between(dateStart, dateEnd);
        }

        int hours = resSecond / 3600;
        int minutes = (resSecond % 3600) / 60;
        int seconds = resSecond % 60;
        String result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        log.debug("days: {} - life {}", businessDays, result);

        return resSecond;
    }


}
