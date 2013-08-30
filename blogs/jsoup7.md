Jsoup代码解读之七-实现一个CSS Selector
-----

![street fighter][1]

当当当！终于来到了Jsoup的特色：CSS Selector部分。selector也是我写的爬虫框架[webmagic](https://github.com/code4craft/webmagic)开发的一个重点。附上一张street fighter的图，希望以后webmagic也能挑战Jsoup!

## select机制

Jsoup的select包里，类结构如下：

![uml][2]

在最开始介绍Jsoup的时候，就已经说过`NodeVisitor`和`Selector`了。`Selector`是select部分的对外facade，而`NodeVisitor`则是遍历树的底层API，CSS Selector也是根据`NodeVisitor`实现的遍历。

Jsoup的select核心是`Evaluator`。Selector所传递的表达式，会经过`QueryParser`，最终编译成一个`Evaluator`。`Evaluator`是一个抽象类，它只有一个方法：

```java
	public abstract boolean matches(Element root, Element element);
```

注意这里传入了root，是为了某些情况下对树进行遍历时用的。

Evaluator的设计简洁明了，所有的Selector表达式单词都会编译到对应的Evaluator。例如`#xx`对应`Id`，`.xx`对应`Class`，`[]`对应`Attribute`。这里补充一下w3c的CSS Selector规范：[http://www.w3.org/TR/CSS2/selector.html](http://www.w3.org/TR/CSS2/selector.html)

当然，只靠这几个还不够，Jsoup还定义了`CombiningEvaluator`(对Evaluator进行And/Or组合)，`StructuralEvaluator`(结合DOM树结构进行筛选)。

这里我们可能最关心的是，“div ul li”这样的父子结构是如何实现的。这个的实现方式在`StructuralEvaluator.Parent`中，贴一下代码了：

```java
    static class Parent extends StructuralEvaluator {
        public Parent(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element parent = element.parent();
            while (parent != root) {
                if (evaluator.matches(root, parent))
                    return true;
                parent = parent.parent();
            }
            return false;
        }
    }
```    

这里Parent包含了一个`evaluator`属性，会根据这个evaluator去验证所有父节点。注意Parent是可以嵌套的，所以这个表达式"div ul li"最终会编译成`And(Parent(And(Parent(Tag("div"))，Tag("ul")),Tag("li")))`这样的Evaluator组合。

select部分比想象的要简单，代码可读性也很高。经过了parser部分的研究，这部分应该算是驾轻就熟了。

## 关于webmagic的后续打算

webmagic是一个爬虫框架，它的Selector是用于抓取HTML中指定的文本，其机制和Jsoup的Evaluator非常像，只不过webmagic暂时是将Selector封装成较简单的API，而Evaluator直接上了表达式。之前也考虑过自己定制DSL来写一个HTML，现在看了Jsoup的源码，实现能力算是有了，但是引入DSL，实现只是一小部分，如何让DSL易写易懂才是难点。

其实看了Jsoup的源码，精细程度上比webmagic要好得多了，基本每个类都对应一个真实的概念抽象，可能以后会在这方面下点工夫。

下篇文章将讲最后一部分：白名单及HTML过滤机制。

[1]: http://static.oschina.net/uploads/space/2013/0830/180244_r1Vb_190591.jpg

[2]: http://static.oschina.net/uploads/space/2013/0830/184337_j85b_190591.png