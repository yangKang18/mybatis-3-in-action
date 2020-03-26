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
package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * 缓存
 * 缓存的设计类似流的设计，是典型的装饰器模式，在构建缓存时，会追加很多缓存修饰功能
 */
public interface Cache {

  /**
   * 缓存唯一标识，命名空间
   */
  String getId();

  /**
   * 添加缓存
   */
  void putObject(Object key, Object value);

  /**
   * 获取缓存
   */
  Object getObject(Object key);

  /**
   * 移除缓存
   */
  Object removeObject(Object key);

  /**
   * 清除缓存
   */
  void clear();

  /**
   * 获取缓存大小
   */
  int getSize();

  /**
   * 获取读写锁
   */
  default ReadWriteLock getReadWriteLock() {
    return null;
  }

}
