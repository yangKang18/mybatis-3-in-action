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
package org.apache.ibatis.io;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 类型解析工具
 */
public class ResolverUtil<T> {
  /** 日志 */
  private static final Log log = LogFactory.getLog(ResolverUtil.class);

  /**
   * 匹配指定类是否是符合的类型
   */
  public interface Test {
    /**
     * 匹配方法
     */
    boolean matches(Class<?> type);
  }

  public static class IsA implements Test {
    /** 父类型 */
    private Class<?> parent;

    /** 构造函数 */
    public IsA(Class<?> parentType) {
      this.parent = parentType;
    }

    /** 匹配指定类型是否是父类的子类型 */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && parent.isAssignableFrom(type);
    }

    @Override
    public String toString() {
      return "is assignable to " + parent.getSimpleName();
    }
  }

  /**
   * 匹配指定类型是否有指定注解
   */
  public static class AnnotatedWith implements Test {
    /** 指定注解 */
    private Class<? extends Annotation> annotation;

    /** 构造函数 */
    public AnnotatedWith(Class<? extends Annotation> annotation) {
      this.annotation = annotation;
    }

    /** 匹配指定类型是否有指定注解 */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && type.isAnnotationPresent(annotation);
    }

    @Override
    public String toString() {
      return "annotated with @" + annotation.getSimpleName();
    }
  }

  /** 匹配的类型集合 */
  private Set<Class<? extends T>> matches = new HashSet<>();

  /**
   * 类加载器
   */
  private ClassLoader classloader;

  /**
   * 获取所有匹配的类型集合
   */
  public Set<Class<? extends T>> getClasses() {
    return matches;
  }

  /**
   * 获取类加载器
   */
  public ClassLoader getClassLoader() {
    return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
  }

  /**
   * 设置类加载器
   */
  public void setClassLoader(ClassLoader classloader) {
    this.classloader = classloader;
  }

  /**
   * 查找实现类
   */
  public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    /**
     * 遍历包名，按包匹配
     */
    Test test = new IsA(parent);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   * 扫描包文件，查找匹配的注解文件类型
   */
  public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new AnnotatedWith(annotation);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   * 扫描包下文件，匹配添加符合的类文件
   */
  public ResolverUtil<T> find(Test test, String packageName) {
    // 获取包名路径
    String path = getPackagePath(packageName);

    try {
      // 获取所有子文件列表
      List<String> children = VFS.getInstance().list(path);
      // 遍历子文件，匹配字节码类型的文件
      for (String child : children) {
        if (child.endsWith(".class")) {
          addIfMatching(test, child);
        }
      }
    } catch (IOException ioe) {
      log.error("Could not read package: " + packageName, ioe);
    }

    return this;
  }

  /**
   * 获取包路径，替换.为/
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * 匹配添加
   */
  @SuppressWarnings("unchecked")
  protected void addIfMatching(Test test, String fqn) {
    try {
      // 文件名字截取.class之前的名字，同时把/替换为.
      String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
      ClassLoader loader = getClassLoader();
      if (log.isDebugEnabled()) {
        log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
      }
      // 通过类加载器加载文件
      Class<?> type = loader.loadClass(externalName);
      // 如果文件类型符合父类类型，则添加至匹配集合中
      if (test.matches(type)) {
        matches.add((Class<T>) type);
      }
    } catch (Throwable t) {
      log.warn("Could not examine class '" + fqn + "'" + " due to a " +
          t.getClass().getName() + " with message: " + t.getMessage());
    }
  }
}
