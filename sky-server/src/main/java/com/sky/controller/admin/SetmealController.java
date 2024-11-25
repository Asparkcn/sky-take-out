package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.PageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("adminSetmealController")
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @GetMapping("/page")
    public Result<PageResult<SetmealVO>> pageQuery(PageQueryDTO pageQueryDTO) {
        Page<SetmealVO> setmealPageVO = setmealService.page(pageQueryDTO);
        return Result.success(new PageResult<>(setmealPageVO.getTotal(), setmealPageVO.getRecords()));
    }

    @PostMapping
    public Result<String> saveSetmeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @PutMapping
    public Result<String> updateSetmeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SetmealVO> getSetmealDetail(@PathVariable Long id) {
        log.info("查询套餐详情：{}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    @DeleteMapping
    public Result<String> removeSetmeal(@RequestParam List<Long> ids) {
        setmealService.removeBatchByIds(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result<String> startOrStop(Long id, @PathVariable Integer status) {
        setmealService.startOrStop(id, status);
        return Result.success();
    }
}
