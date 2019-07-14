package org.mengyun.tcctransaction.sample.dubbo.capital.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.dubbo.context.DubboTransactionContextEditor;
import org.mengyun.tcctransaction.sample.capital.domain.entity.CapitalAccount;
import org.mengyun.tcctransaction.sample.capital.domain.entity.TradeOrder;
import org.mengyun.tcctransaction.sample.capital.domain.repository.CapitalAccountRepository;
import org.mengyun.tcctransaction.sample.capital.domain.repository.TradeOrderRepository;
import org.mengyun.tcctransaction.sample.dubbo.capital.api.CapitalTradeOrderService;
import org.mengyun.tcctransaction.sample.dubbo.capital.api.dto.CapitalTradeOrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

/**
 * 资金交易服务   包含try  confirm   cancel
 */
@Service("capitalTradeOrderService")
public class CapitalTradeOrderServiceImpl implements CapitalTradeOrderService {

    @Autowired
    CapitalAccountRepository capitalAccountRepository;

    @Autowired
    TradeOrderRepository tradeOrderRepository;

    @Override
    @Compensable(confirmMethod = "confirmRecord", cancelMethod = "cancelRecord", transactionContextEditor = DubboTransactionContextEditor.class)
    @Transactional
    public String record(CapitalTradeOrderDto tradeOrderDto) {

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("调用资金服务，创建交易记录; 时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));

        // 查询交易订单
        TradeOrder foundTradeOrder = tradeOrderRepository.findByMerchantOrderNo(tradeOrderDto.getMerchantOrderNo());


        //如果交易订单已创建，直接返回成功
        if (foundTradeOrder == null) {

            TradeOrder tradeOrder = new TradeOrder(
                    tradeOrderDto.getSelfUserId(),
                    tradeOrderDto.getOppositeUserId(),
                    tradeOrderDto.getMerchantOrderNo(),
                    tradeOrderDto.getAmount()
            );

            try {
                // 保存交易记录，  由于这个字段是唯一的merchantOrderNo，所以并发插入会报错，解决幂等
                tradeOrderRepository.insert(tradeOrder);

                // 查询资金账户
                CapitalAccount transferFromAccount = capitalAccountRepository.findByUserId(tradeOrderDto.getSelfUserId());

                // 资金账户冻结一笔交易资金
                transferFromAccount.transferFrom(tradeOrderDto.getAmount());

                // 保存资金账户
                capitalAccountRepository.save(transferFromAccount);

            } catch (DataIntegrityViolationException e) {
                //当同时插入交易订单时，可能会发生此异常，如果发生，请忽略此插入操作。
            }
        }

        return "success";
    }

    /**
     *确认下单接口，成功时自动触发
     * @param tradeOrderDto
     */
    @Transactional
    public void confirmRecord(CapitalTradeOrderDto tradeOrderDto) {
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("资金服务确认下单接口调用；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));

        // 查询订单交易信息
        TradeOrder tradeOrder = tradeOrderRepository.findByMerchantOrderNo(tradeOrderDto.getMerchantOrderNo());

        //如果不等于空，且待处理，则确认；
        if (tradeOrder != null && tradeOrder.getStatus().equals("DRAFT")) {
            // 修改状态确认
            tradeOrder.confirm();
            // 幂等操作，更新已确认
            tradeOrderRepository.update(tradeOrder);
            // 查询资金账户
            CapitalAccount transferToAccount = capitalAccountRepository.findByUserId(tradeOrderDto.getOppositeUserId());
            // 将账户转换的余额，设置回来
            transferToAccount.transferTo(tradeOrderDto.getAmount());
            // 保存资金账户
            capitalAccountRepository.save(transferToAccount);
        }
    }

    /**
     * 当try发生异常时，此接口触发
     * @param tradeOrderDto
     */
    @Transactional
    public void cancelRecord(CapitalTradeOrderDto tradeOrderDto) {
        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("资金服务，回滚接口触发；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));

        // 查询交易记录
        TradeOrder tradeOrder = tradeOrderRepository.findByMerchantOrderNo(tradeOrderDto.getMerchantOrderNo());

        //检查不为空，并且为待确认状态
        if (null != tradeOrder && "DRAFT".equals(tradeOrder.getStatus())) {
            // 修改状态为取消
            tradeOrder.cancel();
            // 幂等更改交易订单状态
            tradeOrderRepository.update(tradeOrder);
            // 查询资金账户
            CapitalAccount capitalAccount = capitalAccountRepository.findByUserId(tradeOrderDto.getSelfUserId());
            // 回滚余额与转移余额
            capitalAccount.cancelTransfer(tradeOrderDto.getAmount());
            // 保存资金账户
            capitalAccountRepository.save(capitalAccount);
        }
    }
}
