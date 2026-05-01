package com.swna.javafx.domain.admin.product;

import java.util.List;

import lombok.Data;

@Data
public class PageResult<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // getter / setter
}
