package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;

/**
 * <p>
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    /**
     *
   * @param current
     * @return {@link Result}
     */
    Result queryHotBlog(Integer current);

    /**
     *
     * @param id id
     * @return {@link Result}
     */
    Result queryBlogById(Long id);

    /**
     *
     * @param id id
     * @return {@link Result}
     */
    Result likeBlog(Long id);

    /**
     *
     * @param id id
     * @return {@link Result}
     */
    Result queryBlogLikesById(Long id);

    /**
     *
   * @param blog
     * @return {@link Result}
     */
    Result saveBlog(Blog blog);

    /**
     *
   * @param max
   * @param offset
     * @return {@link Result}
     */
    Result queryBlogOfFollow(Long max, Integer offset);
}
