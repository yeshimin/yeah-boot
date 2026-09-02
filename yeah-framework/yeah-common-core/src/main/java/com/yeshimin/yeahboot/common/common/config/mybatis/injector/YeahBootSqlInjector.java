package com.yeshimin.yeahboot.common.common.config.mybatis.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.Delete;
import com.baomidou.mybatisplus.core.injector.methods.DeleteById;
import com.baomidou.mybatisplus.core.injector.methods.DeleteByIds;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods.DeleteByIdWithDeleteTime;
import com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods.DeleteByIdsWithDeleteTime;
import com.yeshimin.yeahboot.common.common.config.mybatis.injector.methods.DeleteWithDeleteTime;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * YeahBoot MyBatis Plus SQL注入器
 * <p>
 * MyBatis Plus启动时会通过SQL注入器为每个Mapper生成通用CRUD语句。
 * 本类保留全部默认CRUD能力，只替换三种删除语句，使逻辑删除时自动记录deleteTime。
 * </p>
 */
public class YeahBootSqlInjector extends DefaultSqlInjector {

    /**
     * 获取当前Mapper需要注册的全部通用方法
     *
     * @param configuration MyBatis配置
     * @param mapperClass   当前Mapper类型
     * @param tableInfo     当前实体和数据库表的映射信息
     * @return 最终注册到Mapper的方法列表
     */
    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        // 先获取MyBatis Plus默认提供的全部CRUD方法
        List<AbstractMethod> methods = super.getMethodList(configuration, mapperClass, tableInfo);

        // 逐个检查，仅替换删除相关方法；新增、查询、更新等方法保持框架默认实现
        for (int i = 0; i < methods.size(); i++) {
            methods.set(i, this.replaceDeleteMethod(methods.get(i)));
        }
        return methods;
    }

    /**
     * 将MyBatis Plus默认删除方法替换为YeahBoot扩展实现
     */
    private AbstractMethod replaceDeleteMethod(AbstractMethod method) {
        // deleteById、removeById、ActiveRecord deleteById最终使用该方法
        if (method instanceof DeleteById) {
            return new DeleteByIdWithDeleteTime();
        }
        // deleteByIds、removeBatchByIds最终使用该方法
        if (method instanceof DeleteByIds) {
            return new DeleteByIdsWithDeleteTime();
        }
        // remove(wrapper)、lambdaUpdate().remove()最终使用该方法
        if (method instanceof Delete) {
            return new DeleteWithDeleteTime();
        }
        // 非删除方法不做任何修改
        return method;
    }
}
