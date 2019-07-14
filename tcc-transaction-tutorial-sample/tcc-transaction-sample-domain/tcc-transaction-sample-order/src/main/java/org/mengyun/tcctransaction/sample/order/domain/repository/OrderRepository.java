package org.mengyun.tcctransaction.sample.order.domain.repository;


import org.mengyun.tcctransaction.sample.order.domain.entity.Order;
import org.mengyun.tcctransaction.sample.order.domain.entity.OrderLine;
import org.mengyun.tcctransaction.sample.order.infrastructure.dao.OrderDao;
import org.mengyun.tcctransaction.sample.order.infrastructure.dao.OrderLineDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/**
 * 订单持久层
 */
@Repository
public class OrderRepository {

    @Autowired
    OrderDao orderDao;

    @Autowired
    OrderLineDao orderLineDao;

    public void createOrder(Order order) {
        orderDao.insert(order);

        for (OrderLine orderLine : order.getOrderLines()) {
            orderLineDao.insert(orderLine);
        }
    }

    public void updateOrder(Order order) {
        // 版本号加1
        order.updateVersion();
        // 更新订单状态
        int effectCount = orderDao.update(order);

        if (effectCount < 1) {
            throw new OptimisticLockingFailureException("update order failed");
        }
    }

    public Order findByMerchantOrderNo(String merchantOrderNo) {
        return orderDao.findByMerchantOrderNo(merchantOrderNo);
    }
}
