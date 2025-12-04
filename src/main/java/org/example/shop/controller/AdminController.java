package org.example.shop.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.shop.common.Result;
import org.example.shop.entity.*;
import org.example.shop.service.impl.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShopServiceImpl shopService;
    private final OrderServiceImpl orderService;
    private final UserServiceImpl userService;
    private final ProductServiceImpl productService;
    private final OrderItemServiceImpl orderItemService;

    // ==================== 用户管理 ====================

    /**
     * 用户列表（分页）
     */
    @GetMapping("/users")
    public Result userList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        page = Math.max(page, 1);
        pageSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        if (role != null && !role.trim().isEmpty()) {
            query.eq(User::getRole, role);
        }
        query.orderByDesc(User::getCreateTime);

        Page<User> userPage = userService.page(new Page<>(page, pageSize), query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", userPage.getRecords());
        result.put("total", userPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    /**
     * 用户详情
     */
    @GetMapping("/user/{id}")
    public Result getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.ok(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/{id}")
    public Result updateUser(@PathVariable Long id, @RequestBody User user) {
        User existing = userService.getById(id);
        if (existing == null) {
            return Result.fail("用户不存在");
        }
        if (user.getNickname() != null) existing.setNickname(user.getNickname());
        if (user.getAvatar() != null) existing.setAvatar(user.getAvatar());
        if (user.getRole() != null) existing.setRole(user.getRole());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        userService.updateById(existing);
        return Result.ok("更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/{id}")
    public Result deleteUser(@PathVariable Long id) {
        if (userService.getById(id) == null) {
            return Result.fail("用户不存在");
        }
        userService.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/user/{id}/status")
    public Result updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
        return Result.ok(status == 1 ? "已启用" : "已禁用");
    }

    // ==================== 店铺管理 ====================

    /**
     * 店铺列表（分页）
     */
    @GetMapping("/shops")
    public Result shopList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        page = Math.max(page, 1);
        pageSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<Shop> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(Shop::getName, keyword);
        }
        if (status != null) {
            query.eq(Shop::getStatus, status);
        }
        query.orderByDesc(Shop::getCreateTime);

        Page<Shop> shopPage = shopService.page(new Page<>(page, pageSize), query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", shopPage.getRecords());
        result.put("total", shopPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    /**
     * 待审核店铺列表
     */
    @GetMapping("/shops/pending")
    public Result pendingShops() {
        return Result.ok(shopService.lambdaQuery().eq(Shop::getStatus, 0).list());
    }

    /**
     * 店铺详情
     */
    @GetMapping("/shop/{id}")
    public Result getShopDetail(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 更新店铺信息
     */
    @PutMapping("/shop/{id}")
    public Result updateShop(@PathVariable Long id, @RequestBody Shop shop) {
        Shop existing = shopService.getById(id);
        if (existing == null) {
            return Result.fail("店铺不存在");
        }
        if (shop.getName() != null) existing.setName(shop.getName());
        if (shop.getLogo() != null) existing.setLogo(shop.getLogo());
        if (shop.getStatus() != null) existing.setStatus(shop.getStatus());
        shopService.updateById(existing);
        return Result.ok("更新成功");
    }

    /**
     * 删除店铺
     */
    @DeleteMapping("/shop/{id}")
    public Result deleteShop(@PathVariable Long id) {
        if (shopService.getById(id) == null) {
            return Result.fail("店铺不存在");
        }
        shopService.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 审核通过
     */
    @PutMapping("/shop/approve/{id}")
    public Result approve(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        shopService.lambdaUpdate()
                .eq(Shop::getId, id)
                .set(Shop::getStatus, 1)
                .update();
        return Result.ok("审核通过");
    }

    /**
     * 审核驳回
     */
    @PutMapping("/shop/reject/{id}")
    public Result reject(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        shopService.lambdaUpdate()
                .eq(Shop::getId, id)
                .set(Shop::getStatus, 2)
                .update();
        return Result.ok("已驳回");
    }

    // ==================== 商品管理 ====================

    /**
     * 商品列表（分页）
     */
    @GetMapping("/products")
    public Result productList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer isOnSale) {
        page = Math.max(page, 1);
        pageSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(Product::getName, keyword);
        }
        if (shopId != null) {
            query.eq(Product::getShopId, shopId);
        }
        if (isOnSale != null) {
            query.eq(Product::getIsOnSale, isOnSale);
        }
        query.orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productService.page(new Page<>(page, pageSize), query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", productPage.getRecords());
        result.put("total", productPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    /**
     * 商品详情
     */
    @GetMapping("/product/{id}")
    public Result getProductDetail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        return Result.ok(product);
    }

    /**
     * 更新商品信息
     */
    @PutMapping("/product/{id}")
    public Result updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Product existing = productService.getById(id);
        if (existing == null) {
            return Result.fail("商品不存在");
        }
        if (product.getName() != null) existing.setName(product.getName());
        if (product.getImage() != null) existing.setImage(product.getImage());
        if (product.getPrice() != null) existing.setPrice(product.getPrice());
        if (product.getStock() != null) existing.setStock(product.getStock());
        if (product.getIsOnSale() != null) existing.setIsOnSale(product.getIsOnSale());
        productService.updateById(existing);
        return Result.ok("更新成功");
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/product/{id}")
    public Result deleteProduct(@PathVariable Long id) {
        if (productService.getById(id) == null) {
            return Result.fail("商品不存在");
        }
        productService.removeById(id);
        return Result.ok("删除成功");
    }

    /**
     * 上架/下架商品
     */
    @PutMapping("/product/{id}/sale")
    public Result updateProductSale(@PathVariable Long id, @RequestParam Integer isOnSale) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        product.setIsOnSale(isOnSale);
        productService.updateById(product);
        return Result.ok(isOnSale == 1 ? "已上架" : "已下架");
    }

    // ==================== 订单管理 ====================

    /**
     * 订单列表（分页）
     */
    @GetMapping("/orders")
    public Result orderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long userId) {
        page = Math.max(page, 1);
        pageSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            query.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            query.eq(Order::getStatus, status);
        }
        if (shopId != null) {
            query.eq(Order::getShopId, shopId);
        }
        if (userId != null) {
            query.eq(Order::getUserId, userId);
        }
        query.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderService.page(new Page<>(page, pageSize), query);

        // 为每个订单添加订单项信息
        List<Map<String, Object>> orderList = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("userId", order.getUserId());
            orderMap.put("shopId", order.getShopId());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus());
            orderMap.put("address", order.getAddress());
            orderMap.put("payTime", order.getPayTime());
            orderMap.put("deliverTime", order.getDeliverTime());
            orderMap.put("createTime", order.getCreateTime());

            // 获取订单项
            List<OrderItem> items = orderItemService.lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId())
                    .list();
            orderMap.put("items", items);

            return orderMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", orderList);
        result.put("total", orderPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/order/{id}")
    public Result getOrderDetail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, id)
                .list();

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return Result.ok(result);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/order/{id}/status")
    public Result updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        order.setStatus(status);
        if (status == 2) { // 已支付
            order.setPayTime(LocalDateTime.now());
        } else if (status == 3) { // 已发货
            order.setDeliverTime(LocalDateTime.now());
        }
        orderService.updateById(order);
        return Result.ok("状态更新成功");
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/order/{id}")
    public Result deleteOrder(@PathVariable Long id) {
        if (orderService.getById(id) == null) {
            return Result.fail("订单不存在");
        }
        // 先删除订单项
        orderItemService.lambdaUpdate()
                .eq(OrderItem::getOrderId, id)
                .remove();
        // 再删除订单
        orderService.removeById(id);
        return Result.ok("删除成功");
    }

    // ==================== 数据统计 ====================

    /**
     * 数据统计
     */
    @GetMapping("/stats")
    public Result getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        long totalUsers = userService.count();
        long userCount = userService.lambdaQuery().eq(User::getRole, "user").count();
        long merchantCount = userService.lambdaQuery().eq(User::getRole, "merchant").count();
        long adminCount = userService.lambdaQuery().eq(User::getRole, "admin").count();

        // 店铺统计
        long totalShops = shopService.count();
        long pendingShops = shopService.lambdaQuery().eq(Shop::getStatus, 0).count();
        long approvedShops = shopService.lambdaQuery().eq(Shop::getStatus, 1).count();
        long rejectedShops = shopService.lambdaQuery().eq(Shop::getStatus, 2).count();

        // 商品统计
        long totalProducts = productService.count();
        long onSaleProducts = productService.lambdaQuery().eq(Product::getIsOnSale, 1).count();
        long offSaleProducts = productService.lambdaQuery().eq(Product::getIsOnSale, 0).count();

        // 订单统计
        long totalOrders = orderService.count();
        long pendingPayOrders = orderService.lambdaQuery().eq(Order::getStatus, 1).count();
        long pendingDeliverOrders = orderService.lambdaQuery().eq(Order::getStatus, 2).count();
        long deliveringOrders = orderService.lambdaQuery().eq(Order::getStatus, 3).count();
        long finishedOrders = orderService.lambdaQuery().eq(Order::getStatus, 4).count();
        long cancelledOrders = orderService.lambdaQuery().eq(Order::getStatus, 5).count();

        // 销售额统计（已完成订单的总金额）
        List<Order> finishedOrderList = orderService.lambdaQuery()
                .eq(Order::getStatus, 4)
                .list();
        BigDecimal totalSales = finishedOrderList.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("users", Map.of(
                "total", totalUsers,
                "user", userCount,
                "merchant", merchantCount,
                "admin", adminCount
        ));

        stats.put("shops", Map.of(
                "total", totalShops,
                "pending", pendingShops,
                "approved", approvedShops,
                "rejected", rejectedShops
        ));

        stats.put("products", Map.of(
                "total", totalProducts,
                "onSale", onSaleProducts,
                "offSale", offSaleProducts
        ));

        stats.put("orders", Map.of(
                "total", totalOrders,
                "pendingPay", pendingPayOrders,
                "pendingDeliver", pendingDeliverOrders,
                "delivering", deliveringOrders,
                "finished", finishedOrders,
                "cancelled", cancelledOrders
        ));

        stats.put("sales", Map.of(
                "totalAmount", totalSales,
                "finishedCount", finishedOrders
        ));

        return Result.ok(stats);
    }
}