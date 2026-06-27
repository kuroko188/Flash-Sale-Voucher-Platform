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
@TableName("tb_shop")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Shop name
     */
    private String name;

    /**
     * Shop type id
     */
    private Long typeId;

    /**
     * Images (comma-separated)
     */
    private String images;

    /**
     * Business area
     */
    private String area;

    /**
     * Address
     */
    private String address;

    /**
     * Longitude
     */
    private Double x;

    /**
     * Latitude
     */
    private Double y;

    /**
     * Average price
     */
    private Long avgPrice;

    /**
     * Sold count
     */
    private Integer sold;

    /**
     * Comment count
     */
    private Integer comments;

    /**
     * Score (1-5, stored x10)
     */
    private Integer score;

    /**
     */
    private String openHours;

    /**
     */
    private LocalDateTime createTime;

    /**
     */
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private Double distance;
}
