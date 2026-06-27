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
@TableName("tb_blog")
public class Blog implements Serializable {

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
     * User id
     */
    private Long userId;
    /**
     */
    @TableField(exist = false)
    private String icon;
    /**
     */
    @TableField(exist = false)
    private String name;
    /**
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * Title
     */
    private String title;

    /**
     * Images (comma-separated, up to 9)
     */
    private String images;

    /**
     * Content
     */
    private String content;

    /**
     * Like count
     */
    private Integer liked;

    /**
     * Comment count
     */
    private Integer comments;

    /**
     */
    private LocalDateTime createTime;

    /**
     */
    private LocalDateTime updateTime;


}
