package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.PageQueryDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrdersVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    public Result<PageResult<Orders>> search(PageQueryDTO pageQueryDTO) {
        Page<Orders> ordersPage = orderService.page(pageQueryDTO);
        return Result.success(new PageResult<>(ordersPage.getTotal(), ordersPage.getRecords()));
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> getStatistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.getStatistics();
        return Result.success(orderStatisticsVO);
    }

    @PutMapping("/cancel")
    public Result<String> cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result<String> completeOrder(@PathVariable Long id) {
        orderService.complete(id);
        return Result.success();
    }

    @PutMapping("/rejection")
    public Result<String> rejectOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {
        orderService.reject(ordersRejectionDTO);
        return Result.success();
    }

    @PutMapping("/confirm")
    public Result<String> confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    @GetMapping("/details/{id}")
    public Result<OrdersVO> getDetails(@PathVariable Long id) {
        OrdersVO ordersVO = orderService.getOrderDetailsById(id);
        return Result.success(ordersVO);
    }

    @PutMapping("/delivery/{id}")
    public Result<String> deliverOrder(@PathVariable Long id) {
        orderService.deliver(id);
        return Result.success();
    }

}
