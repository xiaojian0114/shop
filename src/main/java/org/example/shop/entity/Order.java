package org.example.shop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private BigDecimal totalAmount;
    private Integer status = 1; // 1待支付 2已支付 3已发货 4已完成 5已取消
    private String address;
    private LocalDateTime payTime;
    private LocalDateTime deliverTime;
    private LocalDateTime createTime;
}