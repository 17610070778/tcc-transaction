package org.mengyun.tcctransaction.sample.dubbo.order.service;

import org.apache.commons.lang3.tuple.Pair;
import org.mengyun.tcctransaction.CancellingException;
import org.mengyun.tcctransaction.ConfirmingException;
import org.mengyun.tcctransaction.sample.order.domain.entity.Order;
import org.mengyun.tcctransaction.sample.order.domain.entity.Shop;
import org.mengyun.tcctransaction.sample.order.domain.repository.ShopRepository;
import org.mengyun.tcctransaction.sample.order.domain.service.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 交易订单实现类
 */
@Service
public class PlaceOrderServiceImpl {

    /**
     * 操作商户表
     */
    @Autowired
    ShopRepository shopRepository;

    /**
     * 操作订单表
     */
    @Autowired
    OrderServiceImpl orderService;

    /**
     * 支付服务实现类，内部引用了来自dubbo发布的资金服务和红包服务
     */
    @Autowired
    PaymentServiceImpl paymentService;

    /**
     * 下单
     * @param payerUserId
     * @param shopId
     * @param productQuantities
     * @param redPacketPayAmount
     * @return
     */
    public String placeOrder(long payerUserId, long shopId, List<Pair<Long, Integer>> productQuantities, final BigDecimal redPacketPayAmount) {
        // 本地库查询商店
        Shop shop = shopRepository.findById(shopId);

        // 本库创建订单
        final Order order = orderService.createOrder(payerUserId, shop.getOwnerUserId(), productQuantities);

        Boolean result = false;

        try {

//            ExecutorService executorService = Executors.newFixedThreadPool(2);

//            Future future1 = executorService.submit(new Runnable() {
//                @Override
//                public void run() {
            // 对外发起支付
                    paymentService.makePayment(order.getMerchantOrderNo(), order, redPacketPayAmount, order.getTotalAmount().subtract(redPacketPayAmount));
//                }
//            });

//            Future future2 = executorService.submit(new Runnable() {
//                @Override
//                public void run() {
//                    paymentService.makePayment(order.getMerchantOrderNo(), order, redPacketPayAmount, order.getTotalAmount().subtract(redPacketPayAmount));
//                }
//            });
//
//            future1.get();
//            future2.get();

        } catch (ConfirmingException confirmingException) {
            //exception throws with the tcc transaction status is CONFIRMING,
            //when tcc transaction is confirming status,
            // the tcc transaction recovery will try to confirm the whole transaction to ensure eventually consistent.

            result = true;
        } catch (CancellingException cancellingException) {
            //exception throws with the tcc transaction status is CANCELLING,
            //when tcc transaction is under CANCELLING status,
            // the tcc transaction recovery will try to cancel the whole transaction to ensure eventually consistent.
        } catch (Throwable e) {
            //other exceptions throws at TRYING stage.
            //you can retry or cancel the operation.
            e.printStackTrace();
        }

        return order.getMerchantOrderNo();
    }
}
