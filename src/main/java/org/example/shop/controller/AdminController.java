package org.example.shop.controller;


import lombok.RequiredArgsConstructor;
import org.example.shop.common.Result;
import org.example.shop.entity.Shop;
import org.example.shop.service.impl.OrderServiceImpl;
import org.example.shop.service.impl.ShopServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShopServiceImpl shopService;
    private final OrderServiceImpl orderService;

    // 店铺审核列表
    @GetMapping("/shops/pending")
    public Result pendingShops() {
        return Result.ok(shopService.lambdaQuery().eq(Shop::getStatus, 0).list());
    }

    // 审核通过
    @PutMapping("/shop/approve/{id}")
    public Result approve(@PathVariable Long id) {
        shopService.lambdaUpdate()
                .eq(Shop::getId, id)
                .set(Shop::getStatus, 1)
                .update();
        return Result.ok("审核通过");
    }

    // 审核驳回
    @PutMapping("/shop/reject/{id}")
    public Result reject(@PathVariable Long id) {
        shopService.lambdaUpdate()
                .eq(Shop::getId, id)
                .set(Shop::getStatus, 2)
                .update();
        return Result.ok("已驳回");
    }

    // 所有订单
    @GetMapping("/orders")
    public Result allOrders() {
        return Result.ok(orderService.list());
    }
}