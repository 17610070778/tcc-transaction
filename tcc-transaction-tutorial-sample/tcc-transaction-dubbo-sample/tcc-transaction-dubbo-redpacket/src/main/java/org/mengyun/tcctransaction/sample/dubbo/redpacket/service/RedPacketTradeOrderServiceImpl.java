package org.mengyun.tcctransaction.sample.dubbo.redpacket.service;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.dubbo.context.DubboTransactionContextEditor;
import org.mengyun.tcctransaction.sample.dubbo.redpacket.api.RedPacketTradeOrderService;
import org.mengyun.tcctransaction.sample.dubbo.redpacket.api.dto.RedPacketTradeOrderDto;
import org.mengyun.tcctransaction.sample.redpacket.domain.entity.RedPacketAccount;
import org.mengyun.tcctransaction.sample.redpacket.domain.entity.TradeOrder;
import org.mengyun.tcctransaction.sample.redpacket.domain.repository.RedPacketAccountRepository;
import org.mengyun.tcctransaction.sample.redpacket.domain.repository.TradeOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

/**
 * 红包tcc服务
 */
@Service("redPacketTradeOrderService")
public class RedPacketTradeOrderServiceImpl implements RedPacketTradeOrderService {

    @Autowired
    RedPacketAccountRepository redPacketAccountRepository;

    @Autowired
    TradeOrderRepository tradeOrderRepository;

    @Override
    @Compensable(confirmMethod = "confirmRecord", cancelMethod = "cancelRecord", transactionContextEditor = DubboTransactionContextEditor.class)
    @Transactional
    public String record(RedPacketTradeOrderDto tradeOrderDto) {

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("调用红包服务，创建交易信息；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));

        TradeOrder foundTradeOrder = tradeOrderRepository.findByMerchantOrderNo(tradeOrderDto.getMerchantOrderNo());

        //check if trade order has been recorded, if yes, return success directly.
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
                // 查询红包账户
                RedPacketAccount transferFromAccount = redPacketAccountRepository.findByUserId(tradeOrderDto.getSelfUserId());
                // 冻结预处理资金
                transferFromAccount.transferFrom(tradeOrderDto.getAmount());
                // 保存红包账户
                redPacketAccountRepository.save(transferFromAccount);
            } catch (DataIntegrityViolationException e) {
                //当同时插入红包交易订单时，可能会发生此异常，如果发生，请忽略此插入操作
            }
        }

        return "success";
    }

    @Transactional
    public void confirmRecord(RedPacketTradeOrderDto tradeOrderDto) {

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("红包确认接口被调用；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        // 查询红包交易记录
        TradeOrder tradeOrder = tradeOrderRepository.findByMerchantOrderNo(tradeOrderDto.getMerchantOrderNo());

        //红包交易记录状态不为空，并且为待确认
        if (tradeOrder != null && tradeOrder.getStatus().equals("DRAFT")) {
            // 修改为确认状态
            tradeOrder.confirm();
            // 幂等更新
            tradeOrderRepository.update(tradeOrder);
            // 查询红包账户
            RedPacketAccount transferToAccount = redPacketAccountRepository.findByUserId(tradeOrderDto.getOppositeUserId());
            // 红包确认
            transferToAccount.transferTo(tradeOrderDto.getAmount());
            // 保存账户
            redPacketAccountRepository.save(transferToAccount);
        }
    }

    @Transactional
    public void cancelRecord(RedPacketTradeOrderDto tradeOrderDto) {

        try {
            Thread.sleep(1000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("红包服务取消服务调用；时间>>>" + DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        // 查询红包交易记录
        TradeOrder tradeOrder = tradeOrderRepository.findByMerchantOrderNo(tradeOrderDto.getMerchantOrderNo());

        //红包交易记录状态不为空，并且为待确认
        if (null != tradeOrder && "DRAFT".equals(tradeOrder.getStatus())) {
            // 修改状态为取消
            tradeOrder.cancel();
            // 幂等更新
            tradeOrderRepository.update(tradeOrder);
            // 查询红包账户
            RedPacketAccount capitalAccount = redPacketAccountRepository.findByUserId(tradeOrderDto.getSelfUserId());
            // 红包还原
            capitalAccount.cancelTransfer(tradeOrderDto.getAmount());
            // 保存账户
            redPacketAccountRepository.save(capitalAccount);
        }
    }
}
