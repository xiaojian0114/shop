// src/main/java/org/example/shop/mapper/ProductMapper.java
package org.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shop.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}