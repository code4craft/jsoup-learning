Jsoup代码解读之四-parser(上)
-------
作为Java世界最好的HTML 解析库，Jsoup的parser实现非常具有代表性。这部分也是Jsoup最复杂的部分，需要一些数据结构、状态机乃至编译器的知识。好在HTML语法不复杂，解析只是到DOM树为止，所以作为编译器入门倒是挺合适的。这一块不要指望囫囵吞枣，我们还是泡一杯咖啡，细细品味其中的奥妙吧。

## 基础知识

### 编译器

将计算机语言转化为另一种计算机语言(通常是更底层的语言，例如机器码、汇编、或者JVM字节码)的过程就叫做编译(compile)。编译器(Compiler)是计算机科学的一个重要领域，已经有很多年历史了，而最近各种通用语言层出不穷，加上跨语言编译的兴起、DSL概念的流行，都让编译器变成了一个很时髦的东西。

编译器领域相关有三本公认的经典书籍，龙书-[《Compilers: Principles, Techniques, and Tools 》](http://book.douban.com/subject/1866231/)，虎书[《Modern Compiler Implementation in X (X表示各种语言)》](http://book.douban.com/subject/1923484/)，鲸书[《Advanced Compiler Design and Implementation》](http://book.douban.com/subject/1821532/)。其中龙书是编译理论方面公认的不二之选，而后面两本则对实践更有指导意义。另外[@装配脑袋](http://www.cnblogs.com/Ninputer)有个很好的编译器入门系列博客：[http://www.cnblogs.com/Ninputer/archive/2011/06/07/2074632.html](http://www.cnblogs.com/Ninputer/archive/2011/06/07/2074632.html)

编译器的基本流程如下：

![compiler][1]

HTML是一种声明式的语言，最终的输出是浏览器里图形化的页面，而并非可执行的目标语言，因此这里Translate改为Render更为恰当。

![html compiler][2]

在Jsoup(包括类似的HTML parser)里，只做了Lex(词法分析)、Parse(语法分析)两步，而HTML parse最终产出结果，就是DOM树。至于HTML的语义解析以及渲染，不妨看看携程UED团队的这篇文章[浏览器是怎样工作的：渲染引擎，HTML解析](http://ued.ctrip.com/blog/?p=3295)。

### 状态机

Jsoup的词法分析和语法分析都用到了状态机模型。

状态机由状态和转移两部分构成。状态机本身是一个编程模型，Jsoup里使用了状态模式来实现状态机，可读性还是相当强的。

### HTML语法

XML的BNF

[http://xml.coverpages.org/xmlBNF.html](http://xml.coverpages.org/xmlBNF.html)

## Jsoup中的状态机流程




  [1]: http://static.oschina.net/uploads/space/2013/0828/081055_j2Xy_190591.png
  [2]: http://static.oschina.net/uploads/space/2013/0828/103726_uejc_190591.png