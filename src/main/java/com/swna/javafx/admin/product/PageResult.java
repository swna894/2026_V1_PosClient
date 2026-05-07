package com.swna.javafx.admin.product;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    // =========================
    // Static Factory Methods
    // =========================
    
    /**
     * 빈 페이지 결과 반환
     */
    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setContent(Collections.emptyList());
        result.setPage(0);
        result.setSize(0);
        result.setTotalElements(0);
        result.setTotalPages(0);
        return result;
    }
    
    /**
     * 빈 페이지 결과 반환 (페이지 정보 포함)
     */
    public static <T> PageResult<T> empty(int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setContent(Collections.emptyList());
        result.setPage(page);
        result.setSize(size);
        result.setTotalElements(0);
        result.setTotalPages(0);
        return result;
    }
    
    /**
     * 단일 페이지 결과 생성
     */
    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        PageResult<T> result = new PageResult<>();
        result.setContent(content != null ? content : Collections.emptyList());
        result.setPage(page);
        result.setSize(size);
        result.setTotalElements(totalElements);
        result.setTotalPages((int) Math.ceil((double) totalElements / size));
        return result;
    }
    
    /**
     * 전체 데이터를 단일 페이지로 생성
     */
    public static <T> PageResult<T> ofAll(List<T> content) {
        if (content == null || content.isEmpty()) {
            return empty();
        }
        return of(content, 0, content.size(), content.size());
    }
    
    /**
     * Builder 패턴을 통한 생성
     */
    public static <T> PageResultBuilder<T> builder() {
        return new PageResultBuilder<>();
    }
    
    // =========================
    // Builder Class
    // =========================
    
    public static class PageResultBuilder<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        
        public PageResultBuilder<T> content(List<T> content) {
            this.content = content;
            return this;
        }
        
        public PageResultBuilder<T> page(int page) {
            this.page = page;
            return this;
        }
        
        public PageResultBuilder<T> size(int size) {
            this.size = size;
            return this;
        }
        
        public PageResultBuilder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }
        
        public PageResultBuilder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }
        
        public PageResult<T> build() {
            PageResult<T> result = new PageResult<>();
            result.setContent(content != null ? content : Collections.emptyList());
            result.setPage(page);
            result.setSize(size);
            result.setTotalElements(totalElements);
            result.setTotalPages(totalPages > 0 ? totalPages : 
                (int) Math.ceil((double) totalElements / size));
            return result;
        }
    } 
}