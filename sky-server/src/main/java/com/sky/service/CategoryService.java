package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.CategoryDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> list(Integer type);
    Page<Category> page(PageQueryDTO pageQueryDTO);

    void save(CategoryDTO categoryDTO);

    void update(Long id, Integer status);
}
