package com.adam.fileprocessor.entities;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("games")
@ToString
public class Game {
    private String player;
    private Long game;
    private LocalDateTime creationDate;
    private LocalDateTime treansferDate;
}
