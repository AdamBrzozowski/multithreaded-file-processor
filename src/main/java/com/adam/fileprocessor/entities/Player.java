package com.adam.fileprocessor.entities;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("players")
@ToString
public class Player {
    private String name;
}
