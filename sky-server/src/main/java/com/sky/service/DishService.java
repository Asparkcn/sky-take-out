package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    Page<Dish> page(PageQueryDTO pageQueryDTO);

    void save(DishDTO dishDTO);

    List<Dish> list(Integer categoryId);

    DishVO getDishWithFlavorById(Long id);

    void update(DishDTO dishDTO);

    void removeDishAndFlavorAndSetmealByIds(List<Long> ids);

    void update(Long id, Integer status);
}
