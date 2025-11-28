package org.example.shop.controller;



import lombok.Data;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final ShopServiceImpl shopService;
    private final ProductServiceImpl productService;
    private final OrderServiceImpl orderService;


    @GetMapping("/shops")
    public Result getMyShops(@CurrentUser User user) {
        List<Shop> shops = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .list();
        return Result.ok(shops);
    }


    @GetMapping("/products")
    public Result getProducts(@RequestParam Long shopId, @CurrentUser User user) {
        boolean isMyShop = shopService.lambdaQuery()
                .eq(Shop::getId, shopId)
                .eq(Shop::getMerchantId, user.getId())
                .exists();

        if (!isMyShop) return Result.fail("无权限");

        List<Product> products = productService.lambdaQuery()
                .eq(Product::getShopId, shopId)
                .eq(Product::getIsOnSale, 1)
                .orderByDesc(Product::getCreateTime)
                .list();

        return Result.ok(products);
    }


    @PostMapping("/shop/apply")
    public Result applyShop(@RequestBody Shop shop, @CurrentUser User user) {
        if (!"merchant".equals(user.getRole())) return Result.fail("权限不足");
        if (shopService.lambdaQuery().eq(Shop::getMerchantId, user.getId()).exists()) {
            return Result.fail("已提交过申请");
        }
        shop.setMerchantId(user.getId());
        shop.setStatus(0);
        shop.setCreateTime(LocalDateTime.now());
        shopService.save(shop);
        return Result.ok("申请提交成功");
    }

    @PostMapping("/product")
    public Result addProduct(@RequestBody Product product, @CurrentUser User user) {
        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .eq(Shop::getStatus, 1)
                .one();
        if (shop == null) return Result.fail("店铺未通过审核");
        product.setShopId(shop.getId());
        product.setIsOnSale(1);
        productService.save(product);
        return Result.ok("上架成功");
    }

    @PutMapping("/product/off/{id}")
    public Result offProduct(@PathVariable Long id, @CurrentUser User user) {
        Product p = productService.getById(id);
        if (p == null) return Result.fail("商品不存在");

        Shop shop = shopService.getById(p.getShopId());
        if (!shop.getMerchantId().equals(user.getId())) return Result.fail("无权限");

        p.setIsOnSale(0);
        productService.updateById(p);
        return Result.ok("已下架");
    }

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

    @GetMapping("/orders")
    public Result orders(@CurrentUser User user) {
        Shop shop = shopService.lambdaQuery().eq(Shop::getMerchantId, user.getId()).one();
        if (shop == null) return Result.ok(Collections.emptyList());
        return Result.ok(orderService.lambdaQuery().eq(Order::getShopId, shop.getId()).list());
    }


    @PostMapping("/upload")
    public Result uploadImage(@RequestParam("file") MultipartFile file, @CurrentUser User user) {
        if (file.isEmpty()) {
            return Result.fail("文件为空");
        }

        try {

            String originalName = file.getOriginalFilename();
            String suffix = originalName.substring(originalName.lastIndexOf("."));
            String fileName = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + suffix;


            String uploadDir = System.getProperty("user.dir") + "/upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            File savedFile = new File(uploadDir + fileName);
            file.transferTo(savedFile);


            String url = "http://localhost:8080/upload/" + fileName;

            return Result.ok(url);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("上传失败");
        }
    }


    @GetMapping("/shop/info")
    public Result getMyShopInfo(@CurrentUser User user) {
        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .one();

        if (shop != null) {
            Shop simple = new Shop();
            simple.setId(shop.getId());
            simple.setName(shop.getName());
            simple.setLogo(shop.getLogo());
            simple.setStatus(shop.getStatus());
            return Result.ok(simple);
        }

        return Result.ok(null);
    }



    @PostMapping("/shop/update")
    public Result updateShop(
            @RequestBody UpdateShopRequest request,
            @CurrentUser User user) {

        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .one();

        if (shop == null) {
            return Result.fail("请先申请开店");
        }


        shop.setName(request.getName());
        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            shop.setLogo(request.getLogo());
        }

        shopService.updateById(shop);
        return Result.ok("店铺信息更新成功");
    }


    @Data
    public static class UpdateShopRequest {
        private Long id;
        private String name;
        private String logo;
    }


    @GetMapping("/shop/detail/{id}")
    public Result getShopDetail(@PathVariable Long id, @CurrentUser User user) {

        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getId, id)
                .eq(Shop::getMerchantId, user.getId())
                .one();

        if (shop == null) {
            return Result.fail("店铺不存在或无权限");
        }

        List<Product> products = productService.lambdaQuery()
                .eq(Product::getShopId, id)
                .orderByDesc(Product::getCreateTime)
                .list();

        Map<String, Object> result = new HashMap<>();
        result.put("shopInfo", shop);
        result.put("products", products);
        result.put("productCount", products.size());

        return Result.ok(result);
    }


    @GetMapping("/products/all")
    public Result getAllProducts(@RequestParam Long shopId, @CurrentUser User user) {
        boolean isMyShop = shopService.lambdaQuery()
                .eq(Shop::getId, shopId)
                .eq(Shop::getMerchantId, user.getId())
                .exists();

        if (!isMyShop) return Result.fail("无权限");

        List<Product> products = productService.lambdaQuery()
                .eq(Product::getShopId, shopId)
                .orderByDesc(Product::getCreateTime)
                .list();

        return Result.ok(products);
    }


    @GetMapping("/product/{id}")
    public Result getProductDetail(@PathVariable Long id, @CurrentUser User user) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        // 验证权限：确保查询的是自己店铺的商品
        Shop shop = shopService.getById(product.getShopId());
        if (shop == null || !shop.getMerchantId().equals(user.getId())) {
            return Result.fail("无权限查看该商品");
        }

        return Result.ok(product);
    }


    @PutMapping("/product/update")
    public Result updateProduct(@RequestBody Product product, @CurrentUser User user) {
        if (product.getId() == null) {
            return Result.fail("商品ID不能为空");
        }


        Product existingProduct = productService.getById(product.getId());
        if (existingProduct == null) {
            return Result.fail("商品不存在");
        }

        Shop shop = shopService.getById(existingProduct.getShopId());
        if (shop == null || !shop.getMerchantId().equals(user.getId())) {
            return Result.fail("无权限修改该商品");
        }


        existingProduct.setName(product.getName());
        existingProduct.setPrice(product.getPrice());

        existingProduct.setStock(product.getStock());
        existingProduct.setImage(product.getImage());

        boolean success = productService.updateById(existingProduct);
        if (success) {
            return Result.ok("商品更新成功");
        } else {
            return Result.fail("商品更新失败");
        }
    }
    @GetMapping("/merchant/info")
    public Result getMerchantInfo(@CurrentUser User user) {
        // 返回商家基本信息，包括店铺信息
        Map<String, Object> result = new HashMap<>();
        result.put("userInfo", user);

        // 获取店铺信息
        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .one();

        if (shop != null) {
            Map<String, Object> shopInfo = new HashMap<>();
            shopInfo.put("id", shop.getId());
            shopInfo.put("name", shop.getName());
            shopInfo.put("logo", shop.getLogo());
            shopInfo.put("status", shop.getStatus());
            result.put("shopInfo", shopInfo);
        }

        return Result.ok(result);
    }

    // 新增：搜索商品
    @GetMapping("/product/search")
    public Result searchProducts(
            @RequestParam Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @CurrentUser User user) {

        boolean isMyShop = shopService.lambdaQuery()
                .eq(Shop::getId, shopId)
                .eq(Shop::getMerchantId, user.getId())
                .exists();

        if (!isMyShop) return Result.fail("无权限");

        // 构建查询条件
        var query = productService.lambdaQuery()
                .eq(Product::getShopId, shopId);

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like(Product::getName, keyword.trim());
        }

        if (status != null) {
            query.eq(Product::getIsOnSale, status);
        }

        query.orderByDesc(Product::getCreateTime);

        List<Product> products = query.list();
        return Result.ok(products);
    }

    @GetMapping("/order/stats")
    public Result getOrderStats(@CurrentUser User user) {
        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .one();

        if (shop == null) {
            return Result.ok(Collections.emptyMap());
        }

        Map<String, Object> stats = new HashMap<>();

        // 待发货订单数量
        long pendingDelivery = orderService.lambdaQuery()
                .eq(Order::getShopId, shop.getId())
                .eq(Order::getStatus, 2) // 假设状态2是待发货
                .count();

        // 今日订单数量
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        long todayOrders = orderService.lambdaQuery()
                .eq(Order::getShopId, shop.getId())
                .between(Order::getCreateTime, todayStart, todayEnd)
                .count();

        stats.put("pendingDelivery", pendingDelivery);
        stats.put("todayOrders", todayOrders);
        stats.put("totalProducts", productService.lambdaQuery()
                .eq(Product::getShopId, shop.getId())
                .count());

        return Result.ok(stats);
    }
}