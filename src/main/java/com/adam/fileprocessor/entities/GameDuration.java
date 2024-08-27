package com.adam.fileprocessor.entities;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("game_duration")
@ToString
public class GameDuration {
    private Long game;
    private String file;
    private String duration;
    private LocalDateTime creationDate;

    private String pastGames;
    private String durationTot;
    private String durationOther;

}
