package com.telenav.osv.data.sequence.database.entity;

import java.util.List;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import androidx.room.Embedded;
import androidx.room.Relation;

/**
 * @author horatiuf
 */
public class SequenceWithRewardEntity {

    @Embedded
    private SequenceEntity sequenceEntity;

    @Relation(parentColumn = "id", entityColumn = "sequence_id", entity = ScoreEntity.class)
    private List<ScoreEntity> scoreEntities;

    public SequenceWithRewardEntity(SequenceEntity sequenceEntity) {
        this.sequenceEntity = sequenceEntity;
    }

    public SequenceEntity getSequenceEntity() {
        return sequenceEntity;
    }

    public List<ScoreEntity> getScoreEntities() {
        return scoreEntities;
    }

    public void setScoreEntities(List<ScoreEntity> scoreEntities) {
        this.scoreEntities = scoreEntities;
    }
}
