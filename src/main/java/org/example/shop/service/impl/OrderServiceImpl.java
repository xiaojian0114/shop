package org.example.shop.service.impl;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.shop.entity.Order;
import org.example.shop.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper , Order> implements IService<Order> {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getOrderItemsByOrderId(Long orderId) {
        String sql = "SELECT id, order_id, product_id, product_name, product_image, price, quantity " +
                "FROM order_item WHERE order_id = ?";
        return jdbcTemplate.queryForList(sql, orderId);
    }
}
