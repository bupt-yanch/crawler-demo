package org.dssc.demo.data;

import lombok.Data;

@Data
public class Article {

    private int id;

    private String website;

    private String url;

    private String date;

    private String title;

    private String abstractText;

    private String kind;

    private long createdTime;
}
