package com.speechmaru.record.dto.response;

import com.speechmaru.record.document.Record;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecordResponse {

    private final String id;
    private final List<String> text;
    private final List<Integer> speed;
    private final List<Integer> volume;
    private final List<String> keyword;
    private final List<Integer> textInfo;
    private final List<String> habitualWord;
    private final String createdAt;

    public RecordResponse(Record record) {
        this.id = record.getId();
        this.text = record.getText();
        this.speed = record.getSpeed();
        this.volume = record.getVolume();
        this.keyword = record.getKeyword();
        this.textInfo = record.getTextInfo();
        this.habitualWord = record.getHabitualWord();
        this.createdAt = record.getCreatedAt();
    }
}
