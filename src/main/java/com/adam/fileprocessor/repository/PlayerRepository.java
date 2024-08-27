package com.adam.fileprocessor.repository;

import com.adam.fileprocessor.entities.Player;
import org.springframework.data.repository.RepositoryDefinition;

import java.util.List;

@RepositoryDefinition(domainClass = Player.class, idClass = String.class)
public interface PlayerRepository {

    List<Player> findAll();

}
