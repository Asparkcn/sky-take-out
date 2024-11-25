package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.PageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    void save(SetmealDTO setmealDTO);

    List<SetmealVO> list(SetmealDTO setmealDTO);

    List<DishItemVO> getDishItemById(Long id);

    Page<SetmealVO> page(PageQueryDTO pageQueryDTO);

    void update(SetmealDTO setmealDTO);

    SetmealVO getById(Long id);

    void startOrStop(Long id, Integer status);
}
