package org.example.shop.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.shop.annotation.CurrentUser;
import org.example.shop.common.Result;
import org.example.shop.entity.*;
import org.example.shop.service.impl.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final ProductServiceImpl productService;
    private final CartServiceImpl cartService;
    private final OrderServiceImpl orderService;
    private final OrderItemServiceImpl orderItemService;

    private final ShopServiceImpl shopService;


    @GetMapping("/products")
    public Result products() {
        List<Product> list = productService.lambdaQuery()
                .eq(Product::getIsOnSale, 1)
                .list();
        return Result.ok(list);
    }


    @PostMapping("/cart/add")
    public Result addCart(@RequestBody Cart cart, @CurrentUser User user) {
        cart.setUserId(user.getId());
        cartService.saveOrUpdate(cart,
                Wrappers.<Cart>lambdaUpdate()
                        .eq(Cart::getUserId, user.getId())
                        .eq(Cart::getProductId, cart.getProductId()));
        return Result.ok("加入成功");
    }




    @GetMapping("/cart")
    public Result cart(@CurrentUser User user) {

        List<Cart> cartList = cartService.lambdaQuery()
                .eq(Cart::getUserId, user.getId())
                .list();

        if (cartList.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }


        List<Long> productIds = cartList.stream()
                .map(Cart::getProductId)
                .collect(Collectors.toList());


        List<Product> products = productService.lambdaQuery()
                .in(Product::getId, productIds)
                .eq(Product::getIsOnSale, 1)
                .list();


        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));


        List<Map<String, Object>> result = new ArrayList<>();
        for (Cart cart : cartList) {
            Product p = productMap.get(cart.getProductId());
            if (p == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id", cart.getId());
            item.put("productId", p.getId());
            item.put("productName", p.getName());
            item.put("productImage", p.getImage());
            item.put("price", p.getPrice());
            item.put("num", cart.getNum());
            item.put("checked", true);
            result.add(item);
        }

        return Result.ok(result);
    }

    // 提交订单 - 终极无敌修复版（再也不会报 shop_id 外键错误！）
    @PostMapping("/order/submit")
    public Result submitOrder(@RequestBody SubmitOrderDTO dto, @CurrentUser User user) {
        List<Cart> carts = cartService.lambdaQuery()
                .eq(Cart::getUserId, user.getId())
                .in(!dto.getProductIds().isEmpty(), Cart::getProductId, dto.getProductIds())
                .list();

        if (carts.isEmpty()) {
            return Result.fail("购物车为空或所选商品不存在");
        }

        Long shopId = null;
        BigDecimal total = BigDecimal.ZERO;

        for (Cart c : carts) {
            Product p = productService.getById(c.getProductId());
            if (p == null || p.getIsOnSale() == 0 || p.getShopId() == null) {
                return Result.fail("商品已下架或不存在");
            }

            // 从商品正确获取 shopId
            if (shopId == null) {
                shopId = p.getShopId();
            } else if (!shopId.equals(p.getShopId())) {
                return Result.fail("暂不支持跨店铺下单");
            }

            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(c.getNum())));
        }

        // 保险校验：店铺真的存在
        if (shopId == null || shopService.getById(shopId) == null) {
            return Result.fail("店铺不存在或未审核通过");
        }

        Order order = new Order();
        order.setOrderNo("ORD" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        order.setUserId(user.getId());
        order.setShopId(shopId);
        order.setTotalAmount(total);
        order.setAddress(dto.getAddress());
        order.setCreateTime(LocalDateTime.now());
        order.setStatus(1); // 待支付
        orderService.save(order);

        // 生成订单项
        for (Cart c : carts) {
            Product p = productService.getById(c.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(p.getId());
            item.setProductName(p.getName());
            item.setProductImage(p.getImage());
            item.setPrice(p.getPrice());
            item.setNum(c.getNum());
            orderItemService.save(item);
        }

        // 清空已下单的购物车商品
        cartService.lambdaUpdate()
                .eq(Cart::getUserId, user.getId())
                .in(Cart::getProductId, dto.getProductIds())
                .remove();

        return Result.ok(order.getOrderNo());
    }

    @GetMapping("/product/{id}")
    public Result productDetail(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return Result.fail("商品ID错误");
        }

        Product product = productService.getOne(
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getId, id)
                        .eq(Product::getIsOnSale, 1)
        );

        if (product == null) {
            return Result.fail("商品不存在或已下架");
        }



        return Result.ok(product);
    }

    @GetMapping("/shop/info/{id}")
    public Result getShopInfo(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        if (shop == null || shop.getStatus() != 1) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }


    @PostMapping("/cart/delete")
    public Result deleteCart(@RequestBody Map<String, Object> params, @CurrentUser User user) {


        Object productIdObj = params.get("productId");
        Object productIdsObj = params.get("productIds");

        if (productIdObj != null) {
            // 单个删除
            Long productId = Long.valueOf(productIdObj.toString());
            cartService.lambdaUpdate()
                    .eq(Cart::getUserId, user.getId())
                    .eq(Cart::getProductId, productId)
                    .remove();
        } else if (productIdsObj != null) {
            // 批量删除
            List<Long> productIds = ((List<?>) productIdsObj).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());

            cartService.lambdaUpdate()
                    .eq(Cart::getUserId, user.getId())
                    .in(Cart::getProductId, productIds)
                    .remove();
        } else {
            return Result.fail("参数错误");
        }

        return Result.ok("删除成功");
    }


    @GetMapping("/order/list")
    public Result orderList(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") Integer status,   // 0=全部
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {


        page = Math.max(page, 1);
        pageSize = pageSize < 1 ? 10 : Math.min(pageSize, 50);

        var query = Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, user.getId())
                .orderByDesc(Order::getCreateTime);


        if (status > 0) {
            query.eq(Order::getStatus, status);
        }

        var orderPage = orderService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                query
        );


        List<Map<String, Object>> list = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus());
            map.put("statusText", getStatusText(order.getStatus()));
            map.put("createTime", order.getCreateTime());
            map.put("address", order.getAddress());


            List<OrderItem> items = orderItemService.lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId())
                    .list();

            List<Map<String, Object>> goods = items.stream().map(item -> {
                Map<String, Object> g = new HashMap<>();
                g.put("productId", item.getProductId());
                g.put("productName", item.getProductName());
                g.put("productImage", item.getProductImage());
                g.put("price", item.getPrice());
                g.put("num", item.getNum());
                return g;
            }).collect(Collectors.toList());

            map.put("goods", goods);
            map.put("goodsCount", goods.size());

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", orderPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.ok(result);
    }


    private String getStatusText(Integer status) {
        return switch (status) {
            case 1 -> "待支付";
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "已完成";
            case 5 -> "已取消";
            default -> "未知";
        };
    }


    @Data
    static class SubmitOrderDTO {
        private List<Long> productIds;
        private String address;
    }


    @PostMapping("/order/pay")
    public Result payOrder(@RequestBody PayDTO dto, @CurrentUser User user) {
        if (dto.getOrderNo() == null || dto.getOrderNo().isBlank()) {
            return Result.fail("订单号不能为空");
        }

        Order order = orderService.lambdaQuery()
                .eq(Order::getOrderNo, dto.getOrderNo())
                .eq(Order::getUserId, user.getId())
                .one();

        if (order == null) {
            return Result.fail("订单不存在");
        }

        if (order.getStatus() != 1) {
            return Result.fail("订单状态异常，无法支付");
        }


        boolean success = orderService.lambdaUpdate()
                .eq(Order::getId, order.getId())
                .set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .update();

        if (success) {
            return Result.ok("支付成功！订单号：" + order.getOrderNo());
        } else {
            return Result.fail("支付失败，请重试");
        }
    }


    @Data
    static class PayDTO {
        private String orderNo;
    }


    @GetMapping("/info")
    public Result getUserInfo(@CurrentUser User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("nickname", user.getNickname());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole());
        map.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");

        return Result.ok(map);
    }


    @GetMapping("/order/count")
    public Result orderCount(@CurrentUser User user) {

        long total = orderService.lambdaQuery()
                .eq(Order::getUserId, user.getId())
                .count();

        long pending = orderService.lambdaQuery()
                .eq(Order::getUserId, user.getId())
                .eq(Order::getStatus, 1)   // 待支付
                .count();

        long paid = orderService.lambdaQuery()
                .eq(Order::getUserId, user.getId())
                .eq(Order::getStatus, 2)   // 待发货
                .count();

        long delivering = orderService.lambdaQuery()
                .eq(Order::getUserId, user.getId())
                .eq(Order::getStatus, 3)   // 待收货
                .count();

        long finished = orderService.lambdaQuery()
                .eq(Order::getUserId, user.getId())
                .eq(Order::getStatus, 4)   // 已完成
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("pending", pending);
        result.put("paid", paid);
        result.put("delivering", delivering);
        result.put("finished", finished);

        return Result.ok(result);
    }

    @GetMapping("/products/search")
    public Result searchProducts(@RequestParam String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Product> list = productService.list(
                new QueryWrapper<Product>()
                        .like("name", keyword)
        );
        return Result.ok(list);
    }


}