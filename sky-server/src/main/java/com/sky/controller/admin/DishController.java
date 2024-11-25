package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("adminDishController")
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @CacheEvict(cacheNames = "dishCache", allEntries = true)
    @PostMapping
    public Result<String> saveDish(@RequestBody DishDTO dishDTO) {
        dishService.save(dishDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<Dish>> pageQuery(PageQueryDTO pageQueryDTO) {
        Page<Dish> dishes = dishService.page(pageQueryDTO);
        return Result.success(new PageResult<>(dishes.getTotal(), dishes.getRecords()));
    }

    @GetMapping("/list")
    @Cacheable(cacheNames = "dishCache", key = "#categoryId")
    public Result<List<Dish>> listDishes(Integer categoryId) {
        List<Dish> dishes = dishService.list(categoryId);
        return Result.success(dishes);
    }

    @GetMapping("/{id}")
    public Result<DishVO> getDish(@PathVariable Long id) {
        DishVO dishVO = dishService.getDishWithFlavorById(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @CacheEvict(cacheNames = "dishCache", allEntries = true)
    public Result<String> updateDish(@RequestBody DishDTO dishDTO) {
        dishService.update(dishDTO);
        return Result.success();
    }

    @DeleteMapping
    @CacheEvict(cacheNames = "dishCache", allEntries = true)
    public Result<String> removeDishes(@RequestParam List<Long> ids) {
        dishService.removeDishAndFlavorAndSetmealByIds(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result<String> updateStatus(Long id, @PathVariable Integer status) {
        log.info("更新菜品状态：{}", status);
        dishService.update(id, status);
        return Result.success();
    }

}
