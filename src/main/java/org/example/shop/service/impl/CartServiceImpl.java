package org.example.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.shop.entity.Cart;
import org.example.shop.mapper.CartMapper;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper , Cart> implements IService<Cart> {
}
