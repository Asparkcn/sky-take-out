package com.sky.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    List<Integer> getNewUserStatistics(LocalDate begin, LocalDate end);

    List<Integer> getTotalUserStatistics(LocalDate begin, LocalDate end);
}
