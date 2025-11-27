// src/main/java/org/example/shop/mapper/CartMapper.java
package org.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shop.entity.Cart;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}