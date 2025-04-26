package com.hmdp.entity;

import lombok.Data;

@Data
public class VoucherOrderMessage {

    private Long userId;
    private Long orderId;
    private Long voucherId;

}
