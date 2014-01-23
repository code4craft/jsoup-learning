Jsoup学习笔记 
------
**Jsoup**是Java世界的一款HTML解析工具，它支持用CSS Selector方式选择DOM元素，也可过滤HTML文本，防止XSS攻击。

学习Jsoup是为了更好的开发我的另一个爬虫框架[webmagic](https://github.com/code4craft/webmagic)，为了学的比较详细，就强制自己用很规范的方式写出这部分文章。

代码部分来自[https://github.com/jhy/jsoup](https://github.com/jhy/jsoup)，添加了一些中文注释以及示例代码。

---------------

## 提纲

1. [概述](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup1.md)

2. [DOM相关对象](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup2.md)

3. [Document的输出](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup3.md)

4. HTML语法分析parser

	1. [语法分析与状态机基础](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup4.md)
	2. [词法分析Tokenizer](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup5.md)
	3. [语法检查及DOM树构建](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup6.md)

5. [CSS Selector](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup7.md)

6. [防御XSS攻击](https://github.com/code4craft/jsoup-learning/blob/master/blogs/jsoup8.md)

7. [为Jsoup增加XPath选择功能](https://github.com/code4craft/xsoup)
	
	Jsoup默认没有XPath功能，我写了一个项目[Xsoup](https://github.com/code4craft/xsoup)，可以使用XPath来选择HTML文本。Java里较常用的XPath抽取器是HtmlCleaner，Xsoup的性能比它快了一倍。

-------

## 协议：

相关代码遵循MIT协议。

文档遵循CC-BYNC协议。

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/code4craft/jsoup-learning/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

