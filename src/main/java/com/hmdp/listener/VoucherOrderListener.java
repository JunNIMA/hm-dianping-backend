package com.hmdp.listener;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.entity.VoucherOrderMessage;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RabbitMQConstants;
import io.lettuce.core.dynamic.annotation.Key;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class VoucherOrderListener {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private RedissonClient redissonClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = RabbitMQConstants.ORDER_QUEUE,durable = "true"),
            exchange = @Exchange(name = RabbitMQConstants.ORDER_EXCHANGE),
            key = RabbitMQConstants.ORDER_ROUTING_KEY
    ))
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //获取用户
        Long userId = voucherOrder.getUserId();
        //创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if(!isLock){
            //获取锁失败,返回错误信息或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
        }finally {
            //释放锁
            lock.unlock();
        }
    }

}
