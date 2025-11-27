package org.example.shop.controller;



import lombok.RequiredArgsConstructor;
import org.example.shop.annotation.CurrentUser;
import org.example.shop.common.Result;
import org.example.shop.entity.Order;
import org.example.shop.entity.Product;
import org.example.shop.entity.Shop;
import org.example.shop.entity.User;
import org.example.shop.service.impl.OrderServiceImpl;
import org.example.shop.service.impl.ProductServiceImpl;
import org.example.shop.service.impl.ShopServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final ShopServiceImpl shopService;
    private final ProductServiceImpl productService;
    private final OrderServiceImpl orderService;

    // 申请开店
    @PostMapping("/shop/apply")
    public Result applyShop(@RequestBody Shop shop, @CurrentUser User user) {
        System.out.println("=== 当前用户 ===");
        System.out.println("userId: " + user.getId());
        System.out.println("role: " + user.getRole());   // ← 加上这行！
        System.out.println("==================");
        if (!"merchant".equals(user.getRole())) return Result.fail("权限不足");
        if (shopService.lambdaQuery().eq(Shop::getMerchantId, user.getId()).exists()) {
            return Result.fail("已提交过申请");
        }
        shop.setMerchantId(user.getId());
        shop.setStatus(0);
        shop.setCreateTime(LocalDateTime.now());
        shopService.save(shop);
        return Result.ok("申请提交成功，待审核");
    }

    // 上架商品
    @PostMapping("/product")
    public Result addProduct(@RequestBody Product product, @CurrentUser User user) {
        Shop shop = shopService.lambdaQuery().eq(Shop::getMerchantId, user.getId()).one();
        if (shop == null || shop.getStatus() != 1) {
            return Result.fail("店铺未通过审核");
        }
        product.setShopId(shop.getId());
        productService.save(product);
        return Result.ok("上架成功");
    }

    // 商品下架
    @PutMapping("/product/off/{id}")
    public Result offProduct(@PathVariable Long id, @CurrentUser User user) {
        Product p = productService.getById(id);
        Shop shop = shopService.getById(p.getShopId());
        if (!shop.getMerchantId().equals(user.getId())) return Result.fail("无权限");
        Product product = productService.getById(id);
        if (!shop.getMerchantId().equals(user.getId())) {
            return Result.fail("无权限");
        }
        product.setIsOnSale(0);
        productService.updateById(product);  // 正确！
        return Result.ok("已下架");
    }

    // 商家发货
    @PutMapping("/order/deliver/{orderId}")
    public Result deliver(@PathVariable Long orderId, @CurrentUser User user) {
        Order order = orderService.getById(orderId);
        Shop shop = shopService.lambdaQuery().eq(Shop::getMerchantId, user.getId()).one();
        if (!order.getShopId().equals(shop.getId())) return Result.fail("无权限");
        orderService.lambdaUpdate()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, 3)
                .set(Order::getDeliverTime, LocalDateTime.now())
                .update();
        return Result.ok("已发货");
    }

    // 商家订单列表
    @GetMapping("/orders")
    public Result orders(@CurrentUser User user) {
        Shop shop = shopService.lambdaQuery().eq(Shop::getMerchantId, user.getId()).one();
        return Result.ok(orderService.lambdaQuery().eq(Order::getShopId, shop.getId()).list());
    }
}