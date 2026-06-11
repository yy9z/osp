package com.caspar.common;

import lombok.Data;
import java.util.List;

/**
 * 分页结果类
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Integer page, Integer size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
