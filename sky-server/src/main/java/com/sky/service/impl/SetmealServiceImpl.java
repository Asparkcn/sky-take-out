package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.PageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal>
        implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;


    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();

        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        setmeal.setCreateTime(now);
        setmeal.setUpdateTime(now);
        setmeal.setCreateUser(userId);
        setmeal.setUpdateUser(userId);
        setmealMapper.insert(setmeal);

        List<SetmealDish> setmealDishList = BeanUtil.copyToList(setmealDTO.getSetmealDishes(), SetmealDish.class);
        setmealDishList.forEach(item -> item.setSetmealId(setmeal.getId()));

        setmealDishMapper.insert(setmealDishList);
    }

    public List<SetmealVO> list(SetmealDTO setmealDTO) {
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        List<Setmeal> setmealList = setmealMapper.list(setmeal);
        return BeanUtil.copyToList(setmealList, SetmealVO.class);
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    @Override
    public Page<SetmealVO> page(PageQueryDTO pageQueryDTO) {
        Page<SetmealVO> setmealVOPage = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        return setmealMapper.pageWithCategoryName(setmealVOPage, pageQueryDTO);
    }

    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = BaseContext.getCurrentId();

        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        setmeal.setUpdateUser(userId);
        setmeal.setUpdateTime(now);
        setmealMapper.updateById(setmeal);

        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        setmealDishMapper.delete(setmealDishLambdaQueryWrapper);

        List<SetmealDish> setmealDishList = BeanUtil.copyToList(setmealDTO.getSetmealDishes(), SetmealDish.class);
        setmealDishList.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishMapper.insert(setmealDishList);
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        SetmealVO setmealVO = BeanUtil.copyProperties(setmeal, SetmealVO.class);
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(setmealDishLambdaQueryWrapper);
        setmealVO.setSetmealDishes(setmealDishList);
        return setmealVO;
    }

    @Override
    public void startOrStop(Long id, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(setmealDishLambdaQueryWrapper);
        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            List<Long> dishIds = setmealDishList.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
            List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
            dishes.forEach(dish -> {
                if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                    throw new RuntimeException("套餐中菜品 " + dish.getName() + " 已经停售，请在起首套餐前先将该菜品起售或从套餐中移除该菜品");
                }
            });
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .updateUser(userId)
                .updateTime(now)
                .build();
        setmealMapper.updateById(setmeal);
    }
}
