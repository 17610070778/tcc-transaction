package org.mengyun.tcctransaction.sample.dubbo.order.web.controller;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mengyun.tcctransaction.sample.dubbo.order.service.AccountServiceImpl;
import org.mengyun.tcctransaction.sample.order.domain.entity.Order;
import org.mengyun.tcctransaction.sample.order.domain.entity.Product;
import org.mengyun.tcctransaction.sample.order.domain.repository.ProductRepository;
import org.mengyun.tcctransaction.sample.order.domain.service.OrderServiceImpl;
import org.mengyun.tcctransaction.sample.dubbo.order.service.PlaceOrderServiceImpl;
import org.mengyun.tcctransaction.sample.dubbo.order.web.controller.vo.PlaceOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * 订单接口
 */
@Controller
@RequestMapping("")
public class OrderController {

    /**
     * 下订单服务
     */
    @Autowired
    PlaceOrderServiceImpl placeOrderService;

    /**
     * 商品持久层
     */
    @Autowired
    ProductRepository productRepository;

    /**
     * 商品查询服务，内部注入了dubbo发布的资金服务和红包服务
     */
    @Autowired
    AccountServiceImpl accountService;

    /**
     * 订单持久层
     */
    @Autowired
    OrderServiceImpl orderService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("/index");
        return mv;
    }

    /**
     * 根据商店id，查询商品服务
     * @param userId
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/user/{userId}/shop/{shopId}", method = RequestMethod.GET)
    public ModelAndView getProductsInShop(@PathVariable long userId,
                                          @PathVariable long shopId) {
        List<Product> products = productRepository.findByShopId(shopId);

        ModelAndView mv = new ModelAndView("/shop");

        mv.addObject("products", products);
        mv.addObject("userId", userId);
        mv.addObject("shopId", shopId);

        return mv;
    }

    /**
     * 用户购买确认页，会进行查询账务，红包余额
     * @param userId
     * @param shopId
     * @param productId
     * @return
     */
    @RequestMapping(value = "/user/{userId}/shop/{shopId}/product/{productId}/confirm", method = RequestMethod.GET)
    public ModelAndView productDetail(@PathVariable long userId,
                                      @PathVariable long shopId,
                                      @PathVariable long productId) {

        ModelAndView mv = new ModelAndView("product_detail");

        // 根据用户id查询资金账户余额
        mv.addObject("capitalAmount", accountService.getCapitalAccountByUserId(userId));
        // 根据用户id查询红包账户余额
        mv.addObject("redPacketAmount", accountService.getRedPacketAccountByUserId(userId));

        // 根据商品id，查询商品
        mv.addObject("product", productRepository.findById(productId));

        mv.addObject("userId", userId);
        mv.addObject("shopId", shopId);

        return mv;
    }

    /**
     * 下单接口
     * @param redPacketPayAmount
     * @param shopId
     * @param payerUserId
     * @param productId
     * @return
     */
    @RequestMapping(value = "/placeorder", method = RequestMethod.POST)
    public RedirectView placeOrder(@RequestParam String redPacketPayAmount,
                                   @RequestParam long shopId,
                                   @RequestParam long payerUserId,
                                   @RequestParam long productId) {


        // 构建下单请求
        PlaceOrderRequest request = buildRequest(redPacketPayAmount, shopId, payerUserId, productId);

        String merchantOrderNo = placeOrderService.placeOrder(request.getPayerUserId(), request.getShopId(),
                request.getProductQuantities(), request.getRedPacketPayAmount());

        return new RedirectView("/payresult/" + merchantOrderNo);
    }

    @RequestMapping(value = "/payresult/{merchantOrderNo}", method = RequestMethod.GET)
    public ModelAndView getPayResult(@PathVariable String merchantOrderNo) {

        ModelAndView mv = new ModelAndView("pay_success");

        String payResultTip = null;
        Order foundOrder = orderService.findOrderByMerchantOrderNo(merchantOrderNo);

        if ("CONFIRMED".equals(foundOrder.getStatus()))
            payResultTip = "Success";
        else if ("PAY_FAILED".equals(foundOrder.getStatus()))
            payResultTip = "Failed, please see log";
        else
            payResultTip = "Unknown";

        mv.addObject("payResult", payResultTip);

        mv.addObject("capitalAmount", accountService.getCapitalAccountByUserId(foundOrder.getPayerUserId()));
        mv.addObject("redPacketAmount", accountService.getRedPacketAccountByUserId(foundOrder.getPayerUserId()));

        return mv;
    }


    /**
     * 构建下单请求
     * @param redPacketPayAmount
     * @param shopId
     * @param payerUserId
     * @param productId
     * @return
     */
    private PlaceOrderRequest buildRequest(String redPacketPayAmount, long shopId, long payerUserId, long productId) {
        BigDecimal redPacketPayAmountInBigDecimal = new BigDecimal(redPacketPayAmount);
        if (redPacketPayAmountInBigDecimal.compareTo(BigDecimal.ZERO) < 0)
            throw new InvalidParameterException("invalid red packet amount :" + redPacketPayAmount);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setPayerUserId(payerUserId);
        request.setShopId(shopId);
        request.setRedPacketPayAmount(new BigDecimal(redPacketPayAmount));
        request.getProductQuantities().add(new ImmutablePair<Long, Integer>(productId, 1));
        return request;
    }
}
