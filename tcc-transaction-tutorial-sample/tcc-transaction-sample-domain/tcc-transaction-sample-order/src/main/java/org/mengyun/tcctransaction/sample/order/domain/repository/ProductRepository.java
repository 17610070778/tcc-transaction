package org.mengyun.tcctransaction.sample.order.domain.repository;

import org.mengyun.tcctransaction.sample.order.domain.entity.Product;
import org.mengyun.tcctransaction.sample.order.infrastructure.dao.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品查询持久层
 */
@Repository
public class ProductRepository {

    /**
     * 商品查询服务
     */
    @Autowired
    ProductDao productDao;

    public Product findById(long productId){
        return productDao.findById(productId);
    }

    public List<Product> findByShopId(long shopId){
        return productDao.findByShopId(shopId);
    }
}
