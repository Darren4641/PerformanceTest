package com.example.performance.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Getter
@Document(collection = "member")
public class Member {
    @Id
    private String id;
    private String name;
    private String email;
    private String title;
    private String content;
    private Boolean isSend;

    public void send() {
        this.isSend = true;
    }
}
