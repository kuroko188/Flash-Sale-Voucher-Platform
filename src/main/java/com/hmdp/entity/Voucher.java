package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Shop id
     */
    private Long shopId;

    /**
     * Voucher title
     */
    private String title;

    /**
     * Subtitle
     */
    private String subTitle;

    /**
     * Usage rules
     */
    private String rules;

    /**
     * Pay amount
     */
    private Long payValue;

    /**
     * Face value
     */
    private Long actualValue;

    /**
     * Voucher type
     */
    private Integer type;

    /**
     * Voucher type
     */
    private Integer status;
    /**
     * Stock
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     */
    private LocalDateTime createTime;


    /**
     */
    private LocalDateTime updateTime;


}
