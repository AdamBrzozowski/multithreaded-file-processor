package com.adam.fileprocessor.entities;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("past_games")
@ToString
public class PastGames {
    private Long game;
    private LocalDateTime timestamp;
    private String code;
    private String player;
    private String state;
    private LocalDate insertDate;
    private String file;

    public String toStringValues() {
        return "(" + (this.getTimestamp() != null ? this.getTimestamp().toString() : "null") + ", " +
                (this.getState() != null ? this.getState() : "null") + ", " +
                (this.getGame() != null ? this.getGame().toString() : "null") + ", " +
                (this.getPlayer() != null ? this.getPlayer() : "null") + ", " +
                (this.getCode() != null ? this.getCode() : "null") + ", " +
                (this.getInsertDate() != null ? this.getInsertDate().toString() : "null") + ", " +
                (this.getFile() != null ? this.getFile() : "null") +
                ")"
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(game, timestamp, player, state, insertDate, file);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        final PastGames other = (PastGames) obj;
        if (!Objects.equals(this.game, other.game)) {
            return false;
        }
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        if (!Objects.equals(this.player, other.player)) {
            return false;
        }
        if (!Objects.equals(this.state, other.state)) {
            return false;
        }
        if (!Objects.equals(this.insertDate, other.insertDate)) {
            return false;
        }
        if (!Objects.equals(this.file, other.file)) {
            return false;
        }

        return true;
    }
}
