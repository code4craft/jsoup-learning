Jsoup代码解读之八-防御XSS攻击
--------
![hacker][1]

## 防御XSS攻击的一般原理

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

Jsoup给出的答案是白名单。下面是`Whitelist`的部分代码。

```java
public class Whitelist {
    private Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private Map<TagName, Set<AttributeKey>> attributes; // tag -> attribute[]. allowed attributes [href] for a tag.
    private Map<TagName, Map<AttributeKey, AttributeValue>> enforcedAttributes; // always set these attribute values
    private Map<TagName, Map<AttributeKey, Set<Protocol>>> protocols; // allowed URL protocols for attributes
    private boolean preserveRelativeLinks; // option to preserve relative links
}
```    

这里定义了标签名/属性名/属性值的白名单。

而`Cleaner`是过滤的执行者。不出所料，Cleaner内部定义了`CleaningVisitor`来进行标签的过滤。CleaningVisitor的过滤过程并不改变原始DOM树的值，而是将符合条件的属性，加入到`Element destination`里去。

```java
    private final class CleaningVisitor implements NodeVisitor {
        private int numDiscarded = 0;
        private final Element root;
        private Element destination; // current element to append nodes to

        private CleaningVisitor(Element root, Element destination) {
            this.root = root;
            this.destination = destination;
        }

        public void head(Node source, int depth) {
            if (source instanceof Element) {
                Element sourceEl = (Element) source;

                if (whitelist.isSafeTag(sourceEl.tagName())) { // safe, clone and copy safe attrs
                    ElementMeta meta = createSafeElement(sourceEl);
                    Element destChild = meta.el;
                    destination.appendChild(destChild);

                    numDiscarded += meta.numAttribsDiscarded;
                    destination = destChild;
                } else if (source != root) { // not a safe tag, so don't add. don't count root against discarded.
                    numDiscarded++;
                }
            } else if (source instanceof TextNode) {
                TextNode sourceText = (TextNode) source;
                TextNode destText = new TextNode(sourceText.getWholeText(), source.baseUri());
                destination.appendChild(destText);
            } else { // else, we don't care about comments, xml proc instructions, etc
                numDiscarded++;
            }
        }

        public void tail(Node source, int depth) {
            if (source instanceof Element && whitelist.isSafeTag(source.nodeName())) {
                destination = destination.parent(); // would have descended, so pop destination stack
            }
        }
    }
```
    

## 结束语

至此，Jsoup的全部模块都已经写完了。Jsoup源码并不多，只有14000多行，但是实现非常精巧，在读代码的过程中，除了相关知识，还验证几个很重要的思想：

* 最好的代码抽象，是对现实概念的映射。

	这句话在看《代码大全》的时候印象很深刻。在Jsoup里，只要有相关知识，每个类的作用都能第一时间明白其作用。

* 不要过度抽象

	在Jsoup里，只用到了两个接口，一个是`NodeVisitor`，一个是`Connection`，其他都是用抽象类或者直接用实现类代替。记得有次面试的时候被问到我们开发中每逢一个功能，都要先定义一个接口的做法是否必要？现在的答案是没有必要，过度的抽象反而会降低代码质量。

	另外，Jsoup的代码内聚性都很高，每个类的功能基本都定义在类的内部，这是一个典型的充血模型。同时有大量的facade使用，而避免了Factory、Configure等类的出现，个人感觉这点是非常好的。

  [1]: http://static.oschina.net/uploads/space/2013/0831/071752_RBZc_190591.png