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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 */
/**
 * 属性解析器
 * 动态属性解析器
 */
public class PropertyParser {


  //禁止构造 PropertyParser 对象，因为它是一个静态方法的工具类
  private PropertyParser() {
    // Prevent Instantiation
  }

  public static String parse(String string, Properties variables) {
    // <2.1> 创建 VariableTokenHandler 对象
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    // <2.2> 创建 GenericTokenParser 对象
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    // <2.3> 执行解析
    return parser.parse(string);
  }

  //就是一个map，用相应的value替换key
  private static class VariableTokenHandler implements TokenHandler {
    private Properties variables;

    public VariableTokenHandler(Properties variables) {
      this.variables = variables;
    }

    /**
     * 处理 Token
     *
     * @param content Token 字符串
     * @return 处理后的结果
     */
    @Override
    public String handleToken(String content) {
      if (variables != null && variables.containsKey(content)) {
        return variables.getProperty(content);
      }
      return "${" + content + "}";
    }
  }
}
