package com.adam.fileprocessor.repository;

import com.adam.fileprocessor.entities.GameDuration;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.RepositoryDefinition;

import java.time.LocalDateTime;
import java.util.List;


@RepositoryDefinition(domainClass = GameDuration.class, idClass = String.class)
public interface ResultRepository {

    default void save(GameDuration res) {
        save(
                res.getGame(),
                res.getDuration(),
                res.getFile(),
                res.getCreationDate(),
                res.getPastGames(),
                res.getDurationTot(),
                res.getDurationOther()
                );
    }

    @Query("INSERT INTO game_duration(game, duration, file, creation_date, past_games, duration_tot, duration_other) " +
            "VALUES(:game, :duration, :file, :creationDate, :pastGames, :durationTot, :durationOther) " +
            "ON CONFLICT (game, file, creation_date) DO UPDATE " +
            "SET duration = :duration, past_games = :pastGames, duration_tot = :durationTot, duration_other = :durationOther")
    @Modifying
    void save(Long game, String duration, String file, LocalDateTime creationDate, String pastGames, String durationTot, String durationOther);


    @Query("DELETE FROM game_duration")
    @Modifying
    void cleanForTest();

    @Query("DELETE FROM game_duration WHERE file = :filename")
    @Modifying
    void deleteFile(String filename);

    @Query("SELECT * FROM game_duration WHERE file = :filename")
    List<GameDuration> selectFile(String filename);

}
