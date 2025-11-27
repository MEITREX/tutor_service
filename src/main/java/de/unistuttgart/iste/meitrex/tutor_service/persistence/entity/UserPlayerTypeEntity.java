package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a user's Hexad player type scores.
 * Stores the primary player type and all six player type scores.
 */
@Entity
@Table(name = "user_player_type")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPlayerTypeEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_player_type", nullable = false)
    private HexadPlayerType primaryPlayerType;

    @Column(name = "achiever_score")
    private Double achieverScore;

    @Column(name = "player_score")
    private Double playerScore;

    @Column(name = "socialiser_score")
    private Double socialiserScore;

    @Column(name = "free_spirit_score")
    private Double freeSpiritScore;

    @Column(name = "philanthropist_score")
    private Double philanthropistScore;

    @Column(name = "disruptor_score")
    private Double disruptorScore;

    /**
     * Converts the individual percentage fields into a Map for easier access.
     * @return Map of HexadPlayerType to percentage values
     */
    public Map<HexadPlayerType, Double> getPlayerTypePercentagesAsMap() {
        Map<HexadPlayerType, Double> playerTypePercentages = new HashMap<>();
        playerTypePercentages.put(HexadPlayerType.ACHIEVER, achieverScore);
        playerTypePercentages.put(HexadPlayerType.PLAYER, playerScore);
        playerTypePercentages.put(HexadPlayerType.SOCIALISER, socialiserScore);
        playerTypePercentages.put(HexadPlayerType.FREE_SPIRIT, freeSpiritScore);
        playerTypePercentages.put(HexadPlayerType.PHILANTHROPIST, philanthropistScore);
        playerTypePercentages.put(HexadPlayerType.DISRUPTOR, disruptorScore);
        return playerTypePercentages;
    }

    /**
     * Sets all percentage fields from a Map.
     * @param playerTypePercentages Map of HexadPlayerType to percentage values
     */
    public void setPlayerTypePercentagesFromMap(Map<HexadPlayerType, Double> playerTypePercentages) {
        this.achieverScore = playerTypePercentages.get(HexadPlayerType.ACHIEVER);
        this.playerScore = playerTypePercentages.get(HexadPlayerType.PLAYER);
        this.socialiserScore = playerTypePercentages.get(HexadPlayerType.SOCIALISER);
        this.freeSpiritScore = playerTypePercentages.get(HexadPlayerType.FREE_SPIRIT);
        this.philanthropistScore = playerTypePercentages.get(HexadPlayerType.PHILANTHROPIST);
        this.disruptorScore = playerTypePercentages.get(HexadPlayerType.DISRUPTOR);
    }
}
