Jsoup代码解读之八-防御XSS攻击
--------
![hacker][1]

## 一般原理

cleaner是Jsoup的重要功能之一，我们常用它来进行富文本输入中的XSS防御。

我们知道，XSS攻击的一般方式是，通过在页面输入中嵌入一段恶意脚本，对输出时的DOM结构进行修改，从而达到执行这段脚本的目的。对于纯文本输入，过滤/转义HTML特殊字符`<`,`>`,`"`,`'`是行之有效的办法，但是如果本身用户输入的就是一段HTML文本(例如博客文章)，这种方式就不太有效了。这个时候，就是Jsoup大显身手的时候了。

在前面，我们已经知道了，Jsoup里怎么将HTML变成一棵DOM树，怎么对DOM树进行遍历，怎么对DOM文档进行输出，那么其实cleaner的实现方式，也能猜出大概了。使用Jsoup进行XSS防御，大致分为三个步骤:

1. 将HTML解析为DOM树

	这一步可以过滤掉一些企图搞破坏的非闭合标签、非正常语法等。例如一些输入，会尝试用`</textarea>`闭合当前Tag，然后写入攻击脚本。而根据前面对Jsoup的parser的分析，这种时候，这些非闭合标签会被当做错误并丢弃。

2. 过滤高风险标签/属性/属性值

	高风险标签是指`<script>`以及类似标签，对属性/属性值进行过滤是因为某些属性值里也可以写入javascript脚本，例如`onclick='alert("xss!")'`。


3. 重新将DOM树输出为HTML文本

	DOM树的输出，在前面(Jsoup代码解读之三)已经提到过了。

## Cleaner与Whitelist

对于上述的两个步骤，1、3都已经分别在parser和输出中完成，现在只剩下步骤 2：过滤高风险标签等。

Jsoup给出的答案是白名单。

```java
public class Whitelist {
    private Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private Map<TagName, Set<AttributeKey>> attributes; // tag -> attribute[]. allowed attributes [href] for a tag.
    private Map<TagName, Map<AttributeKey, AttributeValue>> enforcedAttributes; // always set these attribute values
    private Map<TagName, Map<AttributeKey, Set<Protocol>>> protocols; // allowed URL protocols for attributes
    private boolean preserveRelativeLinks; // option to preserve relative links
}
```    


  [1]: http://static.oschina.net/uploads/space/2013/0831/071752_RBZc_190591.png