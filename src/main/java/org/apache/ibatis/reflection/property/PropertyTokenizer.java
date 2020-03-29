/*
 *    Copyright 2009-2012 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * @author Clinton Begin
 */
/**
 * 属性分词器
 * 属性分解为标记，迭代子模式
 * 如person[0].birthdate.year，将依次取得person[0], birthdate, year
 * 
 */
public class PropertyTokenizer implements Iterable<PropertyTokenizer>, Iterator<PropertyTokenizer> {
  //例子： person[0].birthdate.year
  /**
   * 当前字符串
   */
  private String name; //person
  /**
   * 索引的 {@link #name} ，因为 {@link #name} 如果存在 {@link #index} 会被更改
   */
  private String indexedName; //person[0]
  /**
   * 编号。
   *
   * 对于数组 name[0] ，则 index = 0
   * 对于 Map map[key] ，则 index = key
   */
  private String index; //0
  /**
   * 剩余字符串
   */
  private String children; //birthdate.year

  public PropertyTokenizer(String fullname) {
      //person[0].birthdate.year
      //找.
    // <1> 初始化 name、children 字符串，使用 . 作为分隔
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      name = fullname.substring(0, delim);
      children = fullname.substring(delim + 1);
    } else {
        //找不到.的话，取全部部分
      name = fullname;
      children = null;
    }
    // <2> 记录当前 name
    indexedName = name;
    //把中括号里的数字给解析出来
    // 若存在 [ ，则获得 index ，并修改 name 。
    delim = name.indexOf('[');
    if (delim > -1) {
      index = name.substring(delim + 1, name.length() - 1);
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    return children != null;
  }

  //取得下一个,非常简单，直接再通过儿子来new另外一个实例
  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }

  @Override
  public Iterator<PropertyTokenizer> iterator() {
    return this;
  }
}
