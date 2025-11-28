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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
            // 1. 生成文件名（防止重名）
            String originalName = file.getOriginalFilename();
            String suffix = originalName.substring(originalName.lastIndexOf("."));
            String fileName = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + suffix;

            // 2. 保存路径（你项目根目录下建个 upload 文件夹）
            String uploadDir = System.getProperty("user.dir") + "/upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            File savedFile = new File(uploadDir + fileName);
            file.transferTo(savedFile);

            // 3. 返回可访问的 URL（前端直接能显示）
            String url = "http://localhost:8080/upload/" + fileName;

            return Result.ok(url); // 前端就靠这个 url 显示图片和提交

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

        // 如果你只想返回 name 和 logo，可以这样（可选）
        if (shop != null) {
            Shop simple = new Shop();
            simple.setId(shop.getId());
            simple.setName(shop.getName());
            simple.setLogo(shop.getLogo());
            simple.setStatus(shop.getStatus());
            return Result.ok(simple);
        }

        return Result.ok(null); // 没开店返回 null
    }

    // ====== 2. 编辑店铺：只改 name 和 logo（最简最稳！）======
    @PostMapping("/shop/update")
    public Result updateShop(
            @RequestParam(value = "id", required = true) Long id,  // 加这行！
            @RequestParam("name") String name,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @CurrentUser User user) {

        Shop shop = shopService.lambdaQuery()
                .eq(Shop::getMerchantId, user.getId())
                .one();

        if (shop == null) {
            return Result.fail("请先申请开店");
        }

        // 修改店铺名
        shop.setName(name);

        // 如果上传了新头像
        if (logo != null && !logo.isEmpty()) {
            try {
                String originalName = logo.getOriginalFilename();
                String suffix = originalName.substring(originalName.lastIndexOf("."));
                String fileName = System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + suffix;

                String uploadDir = System.getProperty("user.dir") + "/upload/";
                new File(uploadDir).mkdirs();

                File savedFile = new File(uploadDir + fileName);
                logo.transferTo(savedFile);

                String logoUrl = "http://localhost:8080/upload/" + fileName;
                shop.setLogo(logoUrl);  // 关键！保存 logo 字段
            } catch (Exception e) {
                e.printStackTrace();
                return Result.fail("头像上传失败");
            }
        }

        shopService.updateById(shop);
        return Result.ok("店铺信息更新成功");
    }
}