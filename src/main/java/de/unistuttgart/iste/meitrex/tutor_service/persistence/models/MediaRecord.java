package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class MediaRecord {
    private UUID id;
    private List<Content> contents;
}
