package com.swna.javafx.common.ui.table;

import javafx.beans.property.*;

public class PagingHelper {

    private final IntegerProperty page = new SimpleIntegerProperty(0);
    private final IntegerProperty size = new SimpleIntegerProperty(10);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(0);

    public IntegerProperty pageProperty() {
        return page;
    }

    public int getPage() {
        return page.get();
    }

    public void next() {
        if (page.get() < totalPages.get() - 1) {
            page.set(page.get() + 1);
        }
    }

    public void prev() {
        if (page.get() > 0) {
            page.set(page.get() - 1);
        }
    }

    public void reset() {
        page.set(0);
    }

    public void setTotalPages(int totalPages) {
        this.totalPages.set(totalPages);
    }

    public String toQueryParam() {
        return "page=" + page.get() + "&size=" + size.get();
    }
}