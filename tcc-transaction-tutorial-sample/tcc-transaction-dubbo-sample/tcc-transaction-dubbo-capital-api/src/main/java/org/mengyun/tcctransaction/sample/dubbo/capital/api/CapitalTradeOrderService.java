package org.mengyun.tcctransaction.sample.dubbo.capital.api;

import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.sample.dubbo.capital.api.dto.CapitalTradeOrderDto;

/**
 * 创建交易订单
 */
public interface CapitalTradeOrderService {

    /**
     * 创建交易订单
     * @param tradeOrderDto
     * @return
     */
    @Compensable
    public String record(CapitalTradeOrderDto tradeOrderDto);

}
