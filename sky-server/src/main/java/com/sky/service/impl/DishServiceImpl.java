package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.mapper.*;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish>
        implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private CategoryMapper categoryMapper;


    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    @Override
    public Page<Dish> page(PageQueryDTO pageQueryDTO) {
        pageQueryDTO.setPage(max(1, pageQueryDTO.getPage()));
        pageQueryDTO.setPageSize(min(max(1, pageQueryDTO.getPageSize()), 20));

        Page<Dish> page = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        return baseMapper.pageWithCategory(page, pageQueryDTO);
    }

    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setCreateUser(BaseContext.getCurrentId());
        dish.setUpdateUser(BaseContext.getCurrentId());
        dish.setCreateTime(LocalDateTime.now());
        dish.setUpdateTime(LocalDateTime.now());
        dishMapper.insert(dish);

        List<DishFlavor> dishFlavors = new ArrayList<>();
        dishDTO.getFlavors().forEach(flavor -> {
            if (!flavor.getValue().equals("[]")) {
                DishFlavor dishFlavor = BeanUtil.copyProperties(flavor, DishFlavor.class);
                dishFlavor.setDishId(dish.getId());
                dishFlavors.add(dishFlavor);
            }
        });
        dishFlavorMapper.insertBatch(dishFlavors);
    }


    @Override
    public List<Dish> list(Integer categoryId) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(categoryId != null, Dish::getCategoryId, categoryId);
        return dishMapper.selectList(dishLambdaQueryWrapper);
    }

    @Override
    public DishVO getDishWithFlavorById(Long id) {
        Dish dish = dishMapper.selectById(id);
        Category category = categoryMapper.selectById(dish.getCategoryId());
        dish.setCategoryName(category.getName());

        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(dishFlavorLambdaQueryWrapper);
        dish.setFlavors(dishFlavors);
        return BeanUtil.copyProperties(dish, DishVO.class);
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishMapper.updateById(dish);

        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, dish.getId());
        dishFlavorMapper.delete(dishFlavorLambdaQueryWrapper);
        List<DishFlavor> dishFlavors = new ArrayList<>();
        dishDTO.getFlavors().forEach(flavor -> {
            if (!flavor.getValue().equals("[]")) {
                DishFlavor dishFlavor = BeanUtil.copyProperties(flavor, DishFlavor.class);
                if (dishFlavor.getDishId() == null) {
                    dishFlavor.setDishId(dish.getId());
                }
                dishFlavors.add(dishFlavor);
            }
        });
        if (!dishFlavors.isEmpty()) {
            dishFlavorMapper.insertBatch(dishFlavors);
        }
    }

    @Override
    @Transactional
    public void removeDishAndFlavorAndSetmealByIds(List<Long> ids) {
        dishMapper.deleteByIds(ids);
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorMapper.delete(dishFlavorLambdaQueryWrapper);
        setmealMapper.deleteByDishIds(ids);
    }

    @Override
    @Transactional
    public void update(Long id, Integer status) {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .updateTime(now)
                .updateUser(userId)
                .build();
        dishMapper.updateById(dish);
        if (StatusConstant.DISABLE.equals(status)) {
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            List<Long> setmealIds = setmealMapper.selectSetmealIdsByDishIds(ids);
            List<Setmeal> setmealList = new ArrayList<>();
            if (setmealIds != null && !setmealIds.isEmpty()) {
                setmealIds.forEach(setmealId -> {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .updateTime(now)
                            .updateUser(userId)
                            .build();
                    setmealList.add(setmeal);
                });
                setmealMapper.updateById(setmealList);
            }
        }
    }
}
