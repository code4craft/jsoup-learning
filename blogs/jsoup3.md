Jsoup代码解读之三-Document的输出
-------

Jsoup官方说明里，一个重要的功能就是***output tidy HTML***。这里我们看看Jsoup是如何输出HTML的。

## HTML相关知识

分析代码前，我们不妨先想想，"tidy HTML"到底包括哪些东西：

* 换行，块级标签习惯上都会独占一行
* 缩进，根据HTML标签嵌套层数，行首缩进会不同
* 严格的标签闭合，如果是可以自闭合的标签并且没有内容，则进行自闭合
* HTML实体的转义

这里要补充一下HTML标签的知识。HTML Tag可以分为block和inline两类。关于Tag的inline和block的定义可以参考[http://www.w3schools.com/html/html_blocks.asp](http://www.w3schools.com/html/html_blocks.asp)，而Jsoup的`Tag`类则是对Java开发者非常好的学习资料。

```java
    // internal static initialisers:
    // prepped from http://www.w3.org/TR/REC-html40/sgml/dtd.html and other sources
    //block tags，需要换行
    private static final String[] blockTags = {
            "html", "head", "body", "frameset", "script", "noscript", "style", "meta", "link", "title", "frame",
            "noframes", "section", "nav", "aside", "hgroup", "header", "footer", "p", "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "pre", "div", "blockquote", "hr", "address", "figure", "figcaption", "form", "fieldset", "ins",
            "del", "s", "dl", "dt", "dd", "li", "table", "caption", "thead", "tfoot", "tbody", "colgroup", "col", "tr", "th",
            "td", "video", "audio", "canvas", "details", "menu", "plaintext"
    };
    //inline tags，无需换行
    private static final String[] inlineTags = {
            "object", "base", "font", "tt", "i", "b", "u", "big", "small", "em", "strong", "dfn", "code", "samp", "kbd",
            "var", "cite", "abbr", "time", "acronym", "mark", "ruby", "rt", "rp", "a", "img", "br", "wbr", "map", "q",
            "sub", "sup", "bdo", "iframe", "embed", "span", "input", "select", "textarea", "label", "button", "optgroup",
            "option", "legend", "datalist", "keygen", "output", "progress", "meter", "area", "param", "source", "track",
            "summary", "command", "device"
    };
    //emptyTags是不能有内容的标签，这类标签都是可以自闭合的
    private static final String[] emptyTags = {
            "meta", "link", "base", "frame", "img", "br", "wbr", "embed", "hr", "input", "keygen", "col", "command",
            "device"
    };
    private static final String[] formatAsInlineTags = {
            "title", "a", "p", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "li", "th", "td", "script", "style",
            "ins", "del", "s"
    };
    //在这些标签里，需要保留空格
    private static final String[] preserveWhitespaceTags = {
            "pre", "plaintext", "title", "textarea"
    };
```

另外，Jsoup的`Entities`类里包含了一些HTML实体转义的东西。这些转义的对应数据保存在`entities-full.properties`和`entities-base.properties`里。

## Jsoup的格式化实现

在Jsoup里，直接调用`Document.toString()`(继承自Element)，即可对文档进行输出。另外`OutputSettings`可以控制输出格式，主要是`prettyPrint`(是否重新格式化)、`outline`(是否强制所有标签换行)、`indentAmount`(缩进长度)等。

里面的继承和互相调用关系略微复杂，大概是这样子：

`Document.toString()`=>`Document.outerHtml()`=>`Element.html()`，最终`Element.html()`又会循环调用所有子元素的`outerHtml()`，拼接起来作为输出。

```java
    private void html(StringBuilder accum) {
        for (Node node : childNodes)
            node.outerHtml(accum);
    }
```

而`outerHtml()`会使用一个`OuterHtmlVisitor`对所以子节点做遍历，并拼装起来作为结果。

```java
	protected void outerHtml(StringBuilder accum) {
        new NodeTraversor(new OuterHtmlVisitor(accum, getOutputSettings())).traverse(this);
    }
```

OuterHtmlVisitor会对所有子节点做遍历，并调用`node.outerHtmlHead()`和`node.outerHtmlTail`两个方法。
    
```java
    private static class OuterHtmlVisitor implements NodeVisitor {
        private StringBuilder accum;
        private Document.OutputSettings out;

        public void head(Node node, int depth) {
            node.outerHtmlHead(accum, depth, out);
        }

        public void tail(Node node, int depth) {
            if (!node.nodeName().equals("#text")) // saves a void hit.
                node.outerHtmlTail(accum, depth, out);
        }
    }
```

我们终于找到了真正工作的代码，`node.outerHtmlHead()`和`node.outerHtmlTail`。Jsoup里每种Node的输出方式都不太一样，这里只讲讲两种主要节点：`Element`和`TextNode`。`Element`是格式化的主要对象，它的两个方法代码如下：

```java
    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (accum.length() > 0 && out.prettyPrint()
                && (tag.formatAsBlock() || (parent() != null && parent().tag().formatAsBlock()) || out.outline()) )
            //换行并调整缩进
            indent(accum, depth, out);
        accum
                .append("<")
                .append(tagName());
        attributes.html(accum, out);

        if (childNodes.isEmpty() && tag.isSelfClosing())
            accum.append(" />");
        else
            accum.append(">");
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {
        if (!(childNodes.isEmpty() && tag.isSelfClosing())) {
            if (out.prettyPrint() && (!childNodes.isEmpty() && (
                    tag.formatAsBlock() || (out.outline() && (childNodes.size()>1 || (childNodes.size()==1 && !(childNodes.get(0) instanceof TextNode))))
            )))
                //换行并调整缩进
                indent(accum, depth, out);
            accum.append("</").append(tagName()).append(">");
        }
    }
```

而ident方法的代码只有一行：

```java
    protected void indent(StringBuilder accum, int depth, Document.OutputSettings out) {
        //out.indentAmount()是缩进长度，默认是1
        accum.append("\n").append(StringUtil.padding(depth * out.indentAmount()));
    }
```
    
代码简单明了，就没什么好说的了。值得一提的是，`StringUtil.padding()`方法为了减少字符串生成，把常用的缩进保存到了一个数组中。

好了，水了一篇文章，下一篇将比较有技术含量的parser部分。

另外，通过本节的学习，我们学到了要把StringBuilder命名为**accum**，而不是**sb**。