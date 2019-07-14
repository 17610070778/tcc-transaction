package org.mengyun.tcctransaction.sample.order.domain.repository;

import org.mengyun.tcctransaction.sample.order.domain.entity.Shop;
import org.mengyun.tcctransaction.sample.order.infrastructure.dao.ShopDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 商店持久层
 */
@Repository
public class ShopRepository {

    @Autowired
    ShopDao shopDao;

    public Shop findById(long id) {

        return shopDao.findById(id);
    }
}
