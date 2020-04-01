package org.apache.ibatis.cache.decorators;
/*
 *    Copyright 2009-2014 the original author or authors.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * Simple blocking decorator 
 * 
 * Sipmle and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 * 阻塞的 Cache 实现类
 * 这里的阻塞比较特殊，当线程去获取缓存值时，如果不存在，则会阻塞后续的其他线程去获取该缓存。
 * @author Eduardo Macarron
 *
 */
public class BlockingCache implements Cache {

  /**
   * 阻塞等待超时时间
   */
  private long timeout;


  private final Cache delegate;
  /**
   * 缓存键与 ReentrantLock 对象的映射
   */
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<Object, ReentrantLock>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public void putObject(Object key, Object value) {
    try {
      // <2.1> 添加缓存
      delegate.putObject(key, value);
    } finally {
      // <2.2> 释放锁
      releaseLock(key);
    }
  }

  @Override
  public Object getObject(Object key) {
    // <1.1> 获得锁
    acquireLock(key);
    // <1.2> 获得缓存值
    Object value = delegate.getObject(key);
    if (value != null) {
// <1.3> 释放锁
      releaseLock(key);
    }        
    return value;
  }

  @Override
  public Object removeObject(Object key) {
    // 释放锁
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  /**
   * 获得 ReentrantLock 对象。如果不存在，进行添加
   *
   * @param key 缓存键
   * @return ReentrantLock 对象
   */
  private ReentrantLock getLockForKey(Object key) {
    ReentrantLock lock = new ReentrantLock();
    ReentrantLock previous = locks.putIfAbsent(key, lock);
    return previous == null ? lock : previous;
  }


  private void acquireLock(Object key) {
    // 获得 ReentrantLock 对象。
    Lock lock = getLockForKey(key);
    // 获得锁，直到超时
    if (timeout > 0) {
      try {
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        if (!acquired) {
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());  
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 释放锁
      lock.lock();
    }
  }
  
  private void releaseLock(Object key) {
    // 获得 ReentrantLock 对象
    ReentrantLock lock = locks.get(key);
    // 如果当前线程持有，进行释放
    if (lock.isHeldByCurrentThread()) {
      lock.unlock();
    }
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }  
}