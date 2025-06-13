package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity(name = "CategorizedQuestion")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorizedQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String question;

    private Category category;

}
