package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.OrderService;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ReportService reportService;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDate begin, LocalDate end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        TurnoverReportVO turnoverStatistics = reportService.getTurnoverStatistics(begin, end);
        OrderReportVO ordersStatistics = reportService.getOrdersStatistics(begin, end);
        UserReportVO userStatistics = reportService.getUserStatistics(begin, end);

        Double turnover = Arrays.stream(StringUtils.split(turnoverStatistics.getTurnoverList(), ","))
                .mapToDouble(Double::parseDouble)
                .sum();
        Integer validOrderCount = Integer.valueOf(ordersStatistics.getValidOrderCountList());
        Double orderCompletionRate = ordersStatistics.getOrderCompletionRate();
        Double unitPrice = validOrderCount != 0 ? turnover / validOrderCount : 0.0;
        Integer newUsers = Integer.valueOf(userStatistics.getNewUserList());

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {

        QueryWrapper<Orders> ordersQueryWrapper = new QueryWrapper<>();
        ordersQueryWrapper
                .select("status", "count(*) as count")
                .groupBy("status")
                .lambda()
                .between(
                        Orders::getOrderTime,
                        LocalDateTime.of(LocalDate.now(), LocalTime.MIN),
                        LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                );

        List<Map<String, Object>> mapList = orderMapper.selectMaps(ordersQueryWrapper);

        Map<Integer, Long> resultMap = mapList.stream()
                .collect(Collectors.toMap(
                        map -> (Integer) map.get("status"),
                        map -> (Long) map.get("count")
                ));
        List<Integer> statusList = Arrays.asList(Orders.TO_BE_CONFIRMED,
                Orders.DELIVERY_IN_PROGRESS,
                Orders.COMPLETED,
                Orders.CANCELLED
        );

        Long allOrders = mapList.stream()
                .mapToLong(map -> {
                    Integer status = (Integer) map.get("status");
                    if (statusList.contains(status)) {
                        return (Long) map.get("count");
                    } else {
                        return 0L;
                    }
                }).sum();

        return OrderOverViewVO.builder()
                .waitingOrders(resultMap.getOrDefault(Orders.TO_BE_CONFIRMED, 0L).intValue())
                .deliveredOrders(resultMap.getOrDefault(Orders.DELIVERY_IN_PROGRESS, 0L).intValue())
                .completedOrders(resultMap.getOrDefault(Orders.COMPLETED, 0L).intValue())
                .cancelledOrders(resultMap.getOrDefault(Orders.CANCELLED, 0L).intValue())
                .allOrders(allOrders.intValue())
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        QueryWrapper<Dish> dishQueryWrapper = new QueryWrapper<>();
        dishQueryWrapper
                .select("status", "count(*) as count")
                .groupBy("status");
        List<Map<String, Object>> mapList = dishMapper.selectMaps(dishQueryWrapper);

        Map<Integer, Long> resultMap = mapList.stream()
                .collect(Collectors.toMap(
                        map -> (Integer) map.get("status"),
                        map -> (Long) map.get("count")
                ));

        Integer sold = resultMap.getOrDefault(StatusConstant.ENABLE, 0L).intValue();
        Integer discontinued = resultMap.getOrDefault(StatusConstant.DISABLE, 0L).intValue();

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        QueryWrapper<Setmeal> setmealQueryWrapper = new QueryWrapper<>();
        setmealQueryWrapper
                .select("status", "count(*) as count")
                .groupBy("status");

        List<Map<String, Object>> mapList = setmealMapper.selectMaps(setmealQueryWrapper);
        Map<Integer, Long> resultMap = mapList.stream()
                .collect(Collectors.toMap(
                        map -> (Integer) map.get("status"),
                        map -> (Long) map.get("count")
                ));

        return SetmealOverViewVO.builder()
                .sold(resultMap.getOrDefault(StatusConstant.ENABLE, 0L).intValue())
                .discontinued(resultMap.getOrDefault(StatusConstant.DISABLE, 0L).intValue())
                .build();
    }
}
