package com.swna.javafx.common.ui.table;

/**
 * 
 * TABLEVIEW 변경내역만 저장하는 함수
 *    
 */
@FunctionalInterface
public interface DirtyConsumer<T> {
    void accept(T entity);
}
