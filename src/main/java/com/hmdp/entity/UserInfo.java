package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author hmdp
 * @since 2021-12-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user_info")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key，User id
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * City
     */
    private String city;

    /**
     */
    private String introduce;

    /**
     */
    private Integer fans;

    /**
     */
    private Integer followee;

    /**
     * Gender
     */
    private Boolean gender;

    /**
     * Birthday
     */
    private LocalDate birthday;

    /**
     */
    private Integer credits;

    /**
     */
    private Boolean level;

    /**
     */
    private LocalDateTime createTime;

    /**
     */
    private LocalDateTime updateTime;


}
