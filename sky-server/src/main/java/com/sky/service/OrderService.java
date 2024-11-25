package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.vo.*;

public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    Page<Orders> page(PageQueryDTO pageQueryDTO);

    OrderStatisticsVO getStatistics();

    void cancel(OrdersCancelDTO ordersCancelDTO);

    void complete(Long id);

    void reject(OrdersRejectionDTO ordersRejectionDTO);

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    OrdersVO getOrderDetailsById(Long id);

    void deliver(Long id);

    void repeatById(Long id);

    void reminder(Long id);

    Page<OrdersVO> pageWithOrderDetail(PageQueryDTO pageQueryDTO);
}
