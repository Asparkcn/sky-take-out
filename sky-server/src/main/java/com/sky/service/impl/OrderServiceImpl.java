package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrdersVO;
import com.sky.websocket.WebsocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders>
        implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebsocketServer websocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况的处理（收货地址为空、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.selectList(shoppingCartLambdaQueryWrapper);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入1条数据
        orderMapper.insert(order);

        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setId(null);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        //向明细表插入n条数据
        orderDetailMapper.insert(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.delete(shoppingCartLambdaQueryWrapper);

        //封装返回结果
        return OrderSubmitVO
                .builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal("0.01"), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        paySuccess(ordersPaymentDTO.getOrderNumber());

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        JSONObject notice = new JSONObject();
        notice.put("type", 1);
        notice.put("orderId", ordersDB.getId());
        notice.put("content", "订单号：" + outTradeNo + " ");
        websocketServer.sendToAllClient(notice.toJSONString());

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders
                .builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();


        orderMapper.updateById(orders);
    }

    @Override
    public Page<Orders> page(PageQueryDTO pageQueryDTO) {
        Page<Orders> ordersPage = new Page<>(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper
                .between(pageQueryDTO.getBeginTime() != null && pageQueryDTO.getEndTime() != null, Orders::getOrderTime, pageQueryDTO.getBeginTime(), pageQueryDTO.getEndTime())
                .like(pageQueryDTO.getNumber() != null, Orders::getNumber, pageQueryDTO.getNumber())
                .like(pageQueryDTO.getPhone() != null, Orders::getPhone, pageQueryDTO.getPhone())
                .eq(pageQueryDTO.getStatus() != null, Orders::getStatus, pageQueryDTO.getStatus());
        return orderMapper.selectPage(ordersPage, ordersLambdaQueryWrapper);
    }

    @Override
    public OrderStatisticsVO getStatistics() {
        QueryWrapper<Orders> ordersQueryWrapper = new QueryWrapper<>();
        ordersQueryWrapper
                .select("status", "count(*) as count")
                .groupBy("status");
        List<Map<String, Object>> mapList = orderMapper.selectMaps(ordersQueryWrapper);

        Map<Integer, Long> resultMap = mapList.stream()
                .collect(Collectors.toMap(
                        map -> (Integer) map.get("status"),
                        map -> (Long) map.get("count")
                ));

        return OrderStatisticsVO
                .builder()
                .cancelled(resultMap.getOrDefault(Orders.CANCELLED, 0L).intValue())
                .completed(resultMap.getOrDefault(Orders.COMPLETED, 0L).intValue())
                .confirmed(resultMap.getOrDefault(Orders.CONFIRMED, 0L).intValue())
                .deliveryInProgress(resultMap.getOrDefault(Orders.DELIVERY_IN_PROGRESS, 0L).intValue())
                .toBeConfirmed(resultMap.getOrDefault(Orders.TO_BE_CONFIRMED, 0L).intValue())
                .build();
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        LocalDateTime now = LocalDateTime.now();
        Orders orders = BeanUtil.copyProperties(ordersCancelDTO, Orders.class);
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(now);
        orderMapper.updateById(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.updateById(orders);
    }

    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = BeanUtil.copyProperties(ordersRejectionDTO, Orders.class);
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(orders.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = BeanUtil.copyProperties(ordersConfirmDTO, Orders.class);
        orders.setStatus(Orders.CONFIRMED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public OrdersVO getOrderDetailsById(Long id) {
        OrdersVO ordersVO = new OrdersVO();
        Orders orders = orderMapper.selectById(id);
        BeanUtils.copyProperties(orders, ordersVO);
        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailLambdaQueryWrapper);
        ordersVO.setOrderDetailList(orderDetailList);
        return ordersVO;
    }

    @Override
    public void deliver(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.updateById(orders);
    }

    @Override
    @Transactional
    public void repeatById(Long id) {
        Long userId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();

        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        OrdersVO orderDetails = this.getOrderDetailsById(id);
        orderDetails.getOrderDetailList().forEach(orderDetail -> {
            ShoppingCart shoppingCart = BeanUtil.copyProperties(orderDetail, ShoppingCart.class);
            shoppingCart.setId(null);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(now);
            shoppingCartList.add(shoppingCart);
        });

        shoppingCartMapper.insert(shoppingCartList);
    }

    @Override
    public void reminder(Long id) {
        JSONObject notice = new JSONObject();
        Orders orders = orderMapper.selectById(id);
        notice.put("type", 2);
        notice.put("orderId", id);
        notice.put("content", String.format("订单号：%s ", orders.getNumber()));
        websocketServer.sendToAllClient(notice.toJSONString());
    }

    @Override
    public Page<OrdersVO> pageWithOrderDetail(PageQueryDTO pageQueryDTO) {
        Page<OrdersVO> ordersVOPage = new Page<>();
        List<OrdersVO> ordersVOList = new ArrayList<>();

        Page<Orders> ordersPage = this.page(pageQueryDTO);

        ordersVOPage.setTotal(ordersPage.getTotal());
        ordersPage.getRecords().forEach(orders -> {
            OrdersVO ordersVO = this.getOrderDetailsById(orders.getId());
            ordersVOList.add(ordersVO);
        });
        ordersVOPage.setRecords(ordersVOList);
        return ordersVOPage;
    }

}
