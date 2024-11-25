package com.sky.controller.admin;

import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("/admin/shop")
public class ShopController {

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @PutMapping("/{status}")
    public Result<String> setStatus(@PathVariable Integer status) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set("status", status.toString());
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> getStatus() {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String statusStr = valueOperations.get("status");
        if (statusStr == null) {
            statusStr = StatusConstant.DISABLE.toString();
        }
        Integer status = Integer.valueOf(statusStr);
        return Result.success(status);
    }
}
