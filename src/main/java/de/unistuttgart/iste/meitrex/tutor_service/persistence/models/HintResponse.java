package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HintResponse {
    private String hint;

    public HintResponse(String hint) {
        this.hint = hint;
    }
}
