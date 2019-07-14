package org.mengyun.tcctransaction.sample.dubbo.order.service;

import org.mengyun.tcctransaction.sample.dubbo.capital.api.CapitalAccountService;
import org.mengyun.tcctransaction.sample.dubbo.redpacket.api.RedPacketAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 账户服务实现类
 */
@Service("accountService")
public class AccountServiceImpl {

    /**
     * 此服务是通过dubbo引用来的
     */
    @Autowired
    RedPacketAccountService redPacketAccountService;

    /**
     * 此服务是通过dubbo引用来的
     */
    @Autowired
    CapitalAccountService capitalAccountService;


    public BigDecimal getRedPacketAccountByUserId(long userId){
        return redPacketAccountService.getRedPacketAccountByUserId(userId);
    }

    public BigDecimal getCapitalAccountByUserId(long userId){
        return capitalAccountService.getCapitalAccountByUserId(userId);
    }
}
