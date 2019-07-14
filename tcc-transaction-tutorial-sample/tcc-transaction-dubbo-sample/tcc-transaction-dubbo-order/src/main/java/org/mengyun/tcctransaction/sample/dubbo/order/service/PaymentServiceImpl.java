package org.mengyun.tcctransaction.sample.dubbo.order.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.api.UniqueIdentity;
import org.mengyun.tcctransaction.sample.dubbo.capital.api.CapitalTradeOrderService;
import org.mengyun.tcctransaction.sample.dubbo.capital.api.dto.CapitalTradeOrderDto;
import org.mengyun.tcctransaction.sample.dubbo.redpacket.api.RedPacketTradeOrderService;
import org.mengyun.tcctransaction.sample.dubbo.redpacket.api.dto.RedPacketTradeOrderDto;
import org.mengyun.tcctransaction.sample.order.domain.entity.Order;
import org.mengyun.tcctransaction.sample.order.domain.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.Calendar;

/**
 * 支付服务实现类
 */
@Service
public class PaymentServiceImpl {

    @Autowired
    CapitalTradeOrderService capitalTradeOrderService;

    @Autowired
    RedPacketTradeOrderService redPacketTradeOrderService;

    @Autowired
    OrderRepository orderRepository;

    @Compensable(confirmMethod = "confirmMakePayment", cancelMethod = "cancelMakePayment", asyncConfirm = false, delayCancelExceptions = {SocketTimeoutException.class, com.alibaba.dubbo.remoting.TimeoutException.class})
    public void makePayment(@UniqueIdentity String orderNo, Order order, BigDecimal redPacketPayAmount, BigDecimal capitalPayAmount) {
        System.out.println("调用下单预处理接口；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));

        //检查订单状态
        if (order.getStatus().equals("DRAFT")) {
            // 把订单改为支付中
            order.pay(redPacketPayAmount, capitalPayAmount);
            try {
                // 使用版本号解决幂等问题
                orderRepository.updateOrder(order);
            } catch (OptimisticLockingFailureException e) {
                //ignore the concurrently update order exception, ensure idempotency.
            }
        }

        // RPC调用资金服务，创建订单交易记录
        String result = capitalTradeOrderService.record(buildCapitalTradeOrderDto(order));
        // RPC调用红包服务，创建红包交易记录
        String result2 = redPacketTradeOrderService.record(buildRedPacketTradeOrderDto(order));
    }

    /**
     * 确认接口
     * 如果当makePayment接口，不发生异常，则当前接口，触发
     * @param orderNo
     * @param order
     * @param redPacketPayAmount
     * @param capitalPayAmount
     */
    public void confirmMakePayment(String orderNo, Order order, BigDecimal redPacketPayAmount, BigDecimal capitalPayAmount) {


        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("调用下单确认接口； 时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        // 查询本地订单，
        Order foundOrder = orderRepository.findByMerchantOrderNo(order.getMerchantOrderNo());

        // 查看订单是否存在，并且是支付中，然后确认提交     如果是已支付状态直接返回，
        if (foundOrder != null && foundOrder.getStatus().equals("PAYING")) {
            // 修改订单状态为确认
            order.confirm();
            // 更新订单状态，幂等操作
            orderRepository.updateOrder(order);
        }
    }

    /**
     * 取消接口
     * 如果makePayment接口发生异常，则当前接口触发
     * @param orderNo
     * @param order
     * @param redPacketPayAmount
     * @param capitalPayAmount
     */
    public void cancelMakePayment(String orderNo, Order order, BigDecimal redPacketPayAmount, BigDecimal capitalPayAmount) {

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("调用下单取消接口；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));

        Order foundOrder = orderRepository.findByMerchantOrderNo(order.getMerchantOrderNo());

        //检查订单不为空，且为支付中，修改订单状态为失败
        if (foundOrder != null && foundOrder.getStatus().equals("PAYING")) {
            order.cancelPayment();
            // 幂等操作
            orderRepository.updateOrder(order);
        }
    }


    private CapitalTradeOrderDto buildCapitalTradeOrderDto(Order order) {

        CapitalTradeOrderDto tradeOrderDto = new CapitalTradeOrderDto();
        tradeOrderDto.setAmount(order.getCapitalPayAmount());
        tradeOrderDto.setMerchantOrderNo(order.getMerchantOrderNo());
        tradeOrderDto.setSelfUserId(order.getPayerUserId());
        tradeOrderDto.setOppositeUserId(order.getPayeeUserId());
        tradeOrderDto.setOrderTitle(String.format("order no:%s", order.getMerchantOrderNo()));

        return tradeOrderDto;
    }

    private RedPacketTradeOrderDto buildRedPacketTradeOrderDto(Order order) {
        RedPacketTradeOrderDto tradeOrderDto = new RedPacketTradeOrderDto();
        tradeOrderDto.setAmount(order.getRedPacketPayAmount());
        tradeOrderDto.setMerchantOrderNo(order.getMerchantOrderNo());
        tradeOrderDto.setSelfUserId(order.getPayerUserId());
        tradeOrderDto.setOppositeUserId(order.getPayeeUserId());
        tradeOrderDto.setOrderTitle(String.format("order no:%s", order.getMerchantOrderNo()));

        return tradeOrderDto;
    }
}
