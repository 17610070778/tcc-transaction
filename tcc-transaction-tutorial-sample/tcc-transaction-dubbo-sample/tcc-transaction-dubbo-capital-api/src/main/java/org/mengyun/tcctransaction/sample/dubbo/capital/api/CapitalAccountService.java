package org.mengyun.tcctransaction.sample.dubbo.capital.api;

import java.math.BigDecimal;

/**
 * 资金账户服务
 */
public interface CapitalAccountService {

    /**
     * 根据账户用户查询资金
     * @param userId
     * @return
     */
    BigDecimal getCapitalAccountByUserId(long userId);
}
