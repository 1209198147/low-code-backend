package com.shikou.aicode.mapper;

import com.mybatisflex.core.BaseMapper;
import com.shikou.aicode.model.entity.App;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用 映射层。
 *
 * @author Shikou
 */
public interface AppMapper extends BaseMapper<App> {
    List<App> findList(@Param("ids") List<Long> ids, @Param("withDelete") boolean withDelete);
}
