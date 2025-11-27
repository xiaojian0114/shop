// src/main/java/org/example/shop/mapper/ShopMapper.java
package org.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shop.entity.Shop;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}