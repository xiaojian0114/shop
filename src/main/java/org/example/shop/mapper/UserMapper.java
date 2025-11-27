// src/main/java/org/example/shop/mapper/UserMapper.java
package org.example.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.shop.entity.User;


@Mapper
public interface UserMapper extends BaseMapper<User> {

}