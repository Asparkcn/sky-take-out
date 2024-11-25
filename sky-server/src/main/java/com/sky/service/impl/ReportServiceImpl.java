package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        Map<LocalDate, BigDecimal> turnoverHashMap = new HashMap<>();

        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            turnoverHashMap.put(date, BigDecimal.ZERO);
        }

        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper.between(
                Orders::getOrderTime,
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX)
        );
        List<Orders> ordersList = orderMapper.selectList(ordersLambdaQueryWrapper);

        Map<LocalDate, BigDecimal> turnoverTreeMap = new TreeMap<>(turnoverHashMap);
        ordersList.forEach(orders -> {
            if (orders.getStatus().equals(Orders.COMPLETED)) {
                LocalDate orderTime = orders.getOrderTime().toLocalDate();
                turnoverTreeMap.merge(orderTime, orders.getAmount(), BigDecimal::add);
            }
        });

        List<LocalDate> dateList = new ArrayList<>();
        List<BigDecimal> turnoverList = new ArrayList<>();

        turnoverTreeMap.forEach((key, value) -> {
            dateList.add(key);
            turnoverList.add(value);
        });

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate i = begin; !i.isAfter(end); i = i.plusDays(1)) {
            dateList.add(i);
        }

        List<Integer> newUserList = userMapper.getNewUserStatistics(begin, end);
        List<Integer> totalUserList = userMapper.getTotalUserStatistics(begin, end);
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        Map<LocalDate, DailyStats> dailyStatsHashMap = new HashMap<>();

        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dailyStatsHashMap.put(date, new DailyStats());
        }

        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper.between(
                Orders::getOrderTime,
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX)
        );

        List<Orders> orderList = orderMapper.selectList(ordersLambdaQueryWrapper);

        orderList.forEach(orders -> {
            LocalDate orderDate = orders.getOrderTime().toLocalDate();
            DailyStats stats = dailyStatsHashMap.get(orderDate);
            if (stats != null) {
                stats.totalOrders++;
                if (orders.getStatus().equals(Orders.COMPLETED)) {
                    stats.validOrders++;
                }
            }
        });

        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;

        TreeMap<LocalDate, DailyStats> dailyStatsTreeMap = new TreeMap<>(dailyStatsHashMap);

        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            DailyStats stats = dailyStatsTreeMap.get(date);
            dateList.add(date);
            orderCountList.add(stats.totalOrders);
            validOrderCountList.add(stats.validOrders);
            totalOrderCount += stats.totalOrders;
            validOrderCount += stats.validOrders;
        }

        BigDecimal completionRate = totalOrderCount > 0
                ? BigDecimal.valueOf(validOrderCount)
                .divide(BigDecimal.valueOf(totalOrderCount), 3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(completionRate.doubleValue())
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        List<String> nameList = new ArrayList<>();
        List<BigDecimal> numberList = new ArrayList<>();

        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper
                .select("name", "sum(number) as number")
                .groupBy("name")
                .orderByDesc("number")
                .last("limit 10");
        List<Map<String, Object>> mapList = orderDetailMapper.selectMaps(orderDetailQueryWrapper);
        mapList.forEach(map -> {
            nameList.add((String) map.get("name"));
            numberList.add((BigDecimal) map.get("number"));
        });

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    @Override
    public void export(HttpServletResponse response) {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx")) {
            if (inputStream != null) {
                try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(inputStream)) {
                    LocalDate end = LocalDate.now().minusDays(1);
                    LocalDate begin = end.minusMonths(1);

                    XSSFSheet sheet = xssfWorkbook.getSheetAt(0);
                    XSSFRow row = sheet.getRow(1);
                    row.getCell(1).setCellValue(String.format("时间：%s 至 %s", begin, end));

                    TurnoverReportVO turnoverStatistics = this.getTurnoverStatistics(begin, end);
                    UserReportVO userStatistics = this.getUserStatistics(begin, end);
                    OrderReportVO ordersStatistics = this.getOrdersStatistics(begin, end);

                    row = sheet.getRow(3);
                    double turnover = Arrays.stream(
                            StringUtils.split(turnoverStatistics.getTurnoverList(), ","))
                            .mapToDouble(Double::parseDouble)
                            .sum();

                    row.getCell(2).setCellValue(turnover);
                    row.getCell(4).setCellValue(ordersStatistics.getOrderCompletionRate());
                    row.getCell(6).setCellValue(
                            Arrays.stream(StringUtils.split(userStatistics.getNewUserList(), ","))
                                    .mapToInt(Integer::parseInt)
                                    .sum()
                    );

                    row = sheet.getRow(4);
                    row.getCell(2).setCellValue(ordersStatistics.getValidOrderCount());
                    row.getCell(4).setCellValue(
                            ordersStatistics.getTotalOrderCount() > 0
                                    ? BigDecimal
                                        .valueOf(turnover / ordersStatistics.getTotalOrderCount())
                                        .setScale(4, RoundingMode.HALF_UP)
                                        .doubleValue()
                                    : 0.0
                    );


                    List<Double> turnoverList = splitToList(turnoverStatistics.getTurnoverList(), Double::parseDouble);
                    List<Integer> validOrderCountList = splitToList(ordersStatistics.getValidOrderCountList(), Integer::parseInt);
                    List<Integer> orderCountList = splitToList(ordersStatistics.getOrderCountList(), Integer::parseInt);
                    List<Integer> newUserList = splitToList(userStatistics.getNewUserList(), Integer::parseInt);

                    int i = 0;
                    for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
                        row = sheet.getRow(7 + i);
                        row.getCell(1).setCellValue(date.toString());
                        row.getCell(2).setCellValue(turnoverList.get(i));
                        row.getCell(3).setCellValue(validOrderCountList.get(i));
                        row.getCell(4).setCellValue(
                                orderCountList.get(i) > 0
                                        ? BigDecimal
                                            .valueOf(Double.valueOf(validOrderCountList.get(i)) / orderCountList.get(i))
                                            .setScale(4, RoundingMode.HALF_UP)
                                            .doubleValue()
                                        : 0.0
                        );
                        row.getCell(5).setCellValue(
                                validOrderCountList.get(i) > 0
                                        ? BigDecimal
                                            .valueOf(turnoverList.get(i) / validOrderCountList.get(i))
                                            .setScale(4, RoundingMode.HALF_UP)
                                            .doubleValue()
                                        : 0.0
                        );
                        row.getCell(6).setCellValue(newUserList.get(i));
                        i++;
                    }

                    xssfWorkbook.write(response.getOutputStream());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private static <T> List<T> splitToList(String str, Function<String, T> mapper) {
        if (StringUtils.isEmpty(str)) {
            return new ArrayList<>();
        }
        return Arrays.stream(StringUtils.split(str, ","))
                .map(mapper)
                .collect(Collectors.toList());
    }

    @Data
    private static class DailyStats {
        private int totalOrders;
        private int validOrders;
    }
}
