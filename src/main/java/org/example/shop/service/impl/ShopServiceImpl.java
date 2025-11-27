package org.example.shop.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.shop.entity.Shop;
import org.example.shop.mapper.ShopMapper;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper , Shop> implements IService<Shop> {
}
