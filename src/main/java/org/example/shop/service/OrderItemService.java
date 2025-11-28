package org.example.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.shop.entity.OrderItem;
import java.util.List;

public interface OrderItemService extends IService<OrderItem> {
    List<OrderItem> getOrderItemsByOrderId(Long orderId);
}