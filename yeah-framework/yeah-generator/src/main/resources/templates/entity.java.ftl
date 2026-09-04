package ${package.Entity};

import com.baomidou.mybatisplus.annotation.TableName;
import ${basePackage}.common.domain.base.ConditionBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
<#list importEntityPackages as pkg>
    <#if pkg != "com.baomidou.mybatisplus.annotation.TableName"
            && pkg != "lombok.Data"
            && pkg != "lombok.EqualsAndHashCode">
import ${pkg};
    </#if>
</#list>

/**
 * ${table.comment!}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("${table.name}")
public class ${table.entityName}Entity extends ConditionBaseEntity<${table.entityName}Entity> {
<#-- ----------  BEGIN 字段循环遍历  ---------->
<#list table.fields as field>
    <#if !(baseEntityFields?seq_contains(field.propertyName))>
        <#if field.comment!?length gt 0>
            <#if entityFieldUseJavaDoc>

    /**
     * ${field.comment}
     */
            </#if>
        </#if>
        <#list field.annotationAttributesList as an>
    ${an.displayName}
        </#list>
    private ${field.propertyType} ${field.propertyName};
    </#if>
</#list>
<#------------  END 字段循环遍历  ---------->
}
