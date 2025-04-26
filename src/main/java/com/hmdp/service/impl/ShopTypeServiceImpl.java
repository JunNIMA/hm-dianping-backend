package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOP_TYPE_KEY;
        List<String> shopTypeJson = stringRedisTemplate.opsForList().range(key, 0, -1);
        if(CollectionUtil.isNotEmpty(shopTypeJson)){
            //存在，JSON字符串转对象返回
            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeJson.toString(), ShopType.class);
            CollectionUtil.sort(shopTypeList, (o1, o2) -> o1.getSort() - o2.getSort());
            return Result.ok(shopTypeList);
        }
        //不存在，从数据库中查
        List<ShopType> typeList = this.query().orderByAsc("sort").list();
        if(CollectionUtil.isEmpty(typeList)){
            return Result.fail("店铺类型不存在");
        }
        List<String> shopTypesJson = typeList.stream()
                .map(shopType -> JSONUtil.toJsonStr(shopType))
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(key,shopTypesJson);
        stringRedisTemplate.expire(key,CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        //返回
        return Result.ok(typeList);
    }
}
