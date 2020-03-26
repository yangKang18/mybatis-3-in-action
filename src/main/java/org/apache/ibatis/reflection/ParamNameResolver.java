/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 参数名解析器
 */
public class ParamNameResolver {

  /** 生成的名字前缀 */
  public static final String GENERIC_NAME_PREFIX = "param";

  /**
   * 按序的参数名
   */
  private final SortedMap<Integer, String> names;
  /** 是否有参数注解 */
  private boolean hasParamAnnotation;

  /**
   * 构造函数
   * 解析方法参数
   */
  public ParamNameResolver(Configuration config, Method method) {
    // 获取所有参数类型数组
    final Class<?>[] paramTypes = method.getParameterTypes();
    // 获取方法参数的注解
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    final SortedMap<Integer, String> map = new TreeMap<>();
    // 获取参数注解的数量，遍历顺序处理参数
    int paramCount = paramAnnotations.length;
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      // 行界限和结果处理器参数不解析
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // skip special parameters
        continue;
      }
      // 遍历Patam注解，获取注解映射的值
      String name = null;
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      // 如果Param注解
      if (name == null) {
        // 如果配置允许使用参数真实名字，则name等于其真实名字
        if (config.isUseActualParamName()) {
          name = getActualParamName(method, paramIndex);
        }
        // 如果不允许使用参数真实名字，则取map中数量值
        if (name == null) {
          // use the parameter index as the name ("0", "1", ...)
          // gcode issue #71
          name = String.valueOf(map.size());
        }
      }
      // 添加参数索引和对应的参数名
      map.put(paramIndex, name);
    }
    names = Collections.unmodifiableSortedMap(map);
  }

  private String getActualParamName(Method method, int paramIndex) {
    return ParamNameUtil.getParamNames(method).get(paramIndex);
  }

  /**
   * 是否是特殊参数，查询行类型的参数和结果处理器类型的参数
   */
  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * Returns parameter names referenced by SQL providers.
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   * 获取取名的参数
   */
  public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    // 如果没有参数，返回空
    if (args == null || paramCount == 0) {
      return null;
    // 如果只有一个参数没有使用Param注解，则取第一个参数
    } else if (!hasParamAnnotation && paramCount == 1) {
      return args[names.firstKey()];
    // 如果使用了Param注解或者多个参数的化
    } else {
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        // param的name ： 参数
        param.put(entry.getValue(), args[entry.getKey()]);
        // 添加param1 ： 参数
        final String genericParamName = GENERIC_NAME_PREFIX + (i + 1);
        // ensure not to overwrite parameter named with @Param
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      // 最后返回参数
      return param;
    }
  }
}
