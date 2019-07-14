package org.mengyun.tcctransaction.sample.capital.domain.entity;



import org.mengyun.tcctransaction.sample.exception.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Created by changming.xie on 4/2/16.
 */
public class CapitalAccount {

    private long id;

    private long userId;

    /**
     * 账户余额
     */
    private BigDecimal balanceAmount;

    private BigDecimal transferAmount = BigDecimal.ZERO;

    public long getUserId() {
        return userId;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void transferFrom(BigDecimal amount) {

        // 从余额里面减去此值
        this.balanceAmount = this.balanceAmount.subtract(amount);

        if (BigDecimal.ZERO.compareTo(this.balanceAmount) > 0) {
            System.out.println("资金不足");
            throw new InsufficientBalanceException();
        }
        // 转换余额加上负数
        transferAmount = transferAmount.add(amount.negate());
    }

    public void transferTo(BigDecimal amount) {
        // 转换余额
        transferAmount = transferAmount.add(amount);
    }

    public void cancelTransfer(BigDecimal amount) {
        this.balanceAmount = this.balanceAmount.add(amount);
        // 转换余额
        transferAmount = transferAmount.add(amount);

    }
}
