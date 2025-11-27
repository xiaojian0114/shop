// src/main/java/org/example/shop/mapper/OrderMapper.java
package org.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shop.entity.Order;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}