Jsoup代码解读之五-parser(中)
-------
上一篇文章讲到了状态机和词法分析的基本知识，这一节我们来分析Jsoup是如何进行词法分析的。

## 代码结构

先介绍以下parser包里的主要类：

* `Parser`

	Jsoup parser的入口facade，封装了常用的parse静态方法。可以设置`maxErrors`，用于收集错误记录，默认是0，即不收集。我写了一个[`PageErrorChecker`]()
	
* `HtmlTreeBuilder`

	语法分析，通过token构建DOM树的类。
	
* `HtmlTreeBuilderState`

	语法分析状态机。

* `ParseError`

	parse错误结果类。parse过程中会收集错误，并在结束时可以获取这些错误信息。

* `ParseErrorList`

	parse错误容器。

* `CharacterReader`

	字符输入器，对读取字符的逻辑的封装。

* `Token` 
	
	保存单个的词法分析结果。
	
* `Tokeniser` 

	保存词法分析过程的状态及结果。
	
* `TokeniserState`

 	用枚举实现的词法分析状态机。
 	
* `TokenQueue`

	虽然披了个Token的马甲，其实是在query的时候用到，留到select部分再讲。



