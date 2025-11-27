package org.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer stock;
    private Integer sales = 0;
    private Integer isOnSale = 1;
    private LocalDateTime createTime;
}