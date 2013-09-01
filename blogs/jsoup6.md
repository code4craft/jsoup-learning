Jsoup代码解读之六-parser(下)
--------
最近生活上有点忙，女儿老是半夜不睡，精神状态也不是很好。工作上的事情也谈不上顺心，有很多想法但是没有几个被认可，有些事情也不是说代码写得好就行的。算了，还是端正态度，毕竟资历尚浅，我还是继续我的。

读Jsoup源码并非无聊，目的其实是为了将webmagic做的更好一点，毕竟parser也是爬虫的重要组成部分之一。读了代码后，收获也不少，对HTML的知识也更进一步了。

## DOM树产生过程

这里单独将`TreeBuilder`部分抽出来叫做语法分析过程可能稍微不妥，其实就是根据Token生成DOM树的过程，不过我还是沿用这个编译器里的称呼了。

`TreeBuilder`同样是一个facade对象，真正进行语法解析的是以下一段代码：
	
```java
    protected void runParser() {
        while (true) {
            Token token = tokeniser.read();
            
            process(token);

            if (token.type == Token.TokenType.EOF)
                break;
        }
    }
```

`TreeBuilder`有两个子类，`HtmlTreeBuilder`和`XmlTreeBuilder`。`XmlTreeBuilder`自然是构建XML树的类，实现颇为简单，基本上是维护一个栈，并根据不同Token插入节点即可：

```java
	@Override
    protected boolean process(Token token) {
        // start tag, end tag, doctype, comment, character, eof
        switch (token.type) {
            case StartTag:
                insert(token.asStartTag());
                break;
            case EndTag:
                popStackToClose(token.asEndTag());
                break;
            case Comment:
                insert(token.asComment());
                break;
            case Character:
                insert(token.asCharacter());
                break;
            case Doctype:
                insert(token.asDoctype());
                break;
            case EOF: // could put some normalisation here if desired
                break;
            default:
                Validate.fail("Unexpected token type: " + token.type);
        }
        return true;
    }
```
    
`insertNode`的代码大致是这个样子(为了便于展示，对方法进行了一些整合)：

```java
    Element insert(Token.StartTag startTag) {
        Tag tag = Tag.valueOf(startTag.name());
        Element el = new Element(tag, baseUri, startTag.attributes);
        stack.getLast().appendChild(el);
        if (startTag.isSelfClosing()) {
            tokeniser.acknowledgeSelfClosingFlag();
            if (!tag.isKnownTag()) // unknown tag, remember this is self closing for output. see above.
                tag.setSelfClosing();
        } else {
            stack.add(el);
        }
        return el;
    }
```

## HTML解析状态机

相比`XmlTreeBuilder`，`HtmlTreeBuilder`则实现较为复杂，除了类似的栈结构以外，还用到了`HtmlTreeBuilderState`来构建了一个状态机来分析HTML。这是为什么呢？不妨看看`HtmlTreeBuilderState`到底用到了哪些状态吧（在代码中中用`<!-- State: -->`标明状态）：

```html
    <!-- State: Initial -->
    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    <!-- State: BeforeHtml -->
    <html lang='zh-CN' xml:lang='zh-CN' xmlns='http://www.w3.org/1999/xhtml'>
    <!-- State: BeforeHead -->
    <head>
      <!-- State: InHead -->
      <script type="text/javascript">
      //<!-- State: Text -->
        function xx(){
        }
      </script>
      <noscript>
        <!-- State: InHeadNoscript -->
        Your browser does not support JavaScript!
      </noscript>
    </head>
    <!-- State: AfterHead -->
    <body>
    <!-- State: InBody -->
    <textarea>
        <!-- State: Text -->
        xxx
    </textarea>
    <table>
        <!-- State: InTable -->
        <!-- State: InTableText -->
        xxx
        <tbody>
        <!-- State: InTableBody -->
        </tbody>
        <tr>
            <!-- State: InRow -->
            <td>
                <!-- State: InCell -->
            </td>
        </tr>    
    </table>
    </html>
```

这里可以看到，HTML标签是有嵌套要求的，例如`<tr>`,`<td>`需要组合`<table>`来使用。根据Jsoup的代码，可以发现，`HtmlTreeBuilderState`做了以下一些事情：

* ### 语法检查
	
	例如`tr`没有嵌套在`table`标签内，则是一个语法错误。当`InBody`状态直接出现以下tag时，则出错。Jsoup里遇到这种错误，会发现这个Token的解析并记录错误，然后继续解析下面内容，并不会直接退出。
	
```java
	    InBody {
	        boolean process(Token t, HtmlTreeBuilder tb) {
				if (StringUtil.in(name,
				"caption", "col", "colgroup", "frame", "head", "tbody", "td", "tfoot", "th", "thead", "tr")) {
				tb.error(this);
				return false;
				}
	        }
```
	
* ### 标签补全

	例如`head`标签没有闭合，就写入了一些只有body内才允许出现的标签，则自动闭合`</head>`。`HtmlTreeBuilderState`有的方法`anythingElse()`就提供了自动补全标签，例如`InHead`状态的自动闭合代码如下：
	
```java
	        private boolean anythingElse(Token t, TreeBuilder tb) {
	            tb.process(new Token.EndTag("head"));
	            return tb.process(t);
	        }
```	
	
还有一种标签闭合方式，例如下面的代码：
	
```java
		private void closeCell(HtmlTreeBuilder tb) {
            if (tb.inTableScope("td"))
                tb.process(new Token.EndTag("td"));
            else
                tb.process(new Token.EndTag("th")); // only here if th or td in scope
        }
```

## 实例研究

### 缺少标签时，会发生什么事？

好了，看了这么多parser的源码，不妨回到我们的日常应用上来。我们知道，在页面里多写一个两个未闭合的标签是很正常的事，那么它们会被怎么解析呢？

就拿`<div>`标签为例：

1. 漏写了开始标签，只写了结束标签

	```java
		case EndTag:
			if (StringUtil.in(name,"div","dl", "fieldset", "figcaption", "figure", "footer", "header", "pre", "section", "summary", "ul")) {                
				if (!tb.inScope(name)) {
				tb.error(this);
				return false;
				} 
			}	
	```
			
	恭喜你，这个`</div>`会被当做错误处理掉，于是你的页面就毫无疑问的乱掉了！当然，如果单纯多写了一个`</div>`，好像也不会有什么影响哦？(记得有人跟我讲过为了防止标签未闭合，而在页面底部多写了几个`</div>`的故事)
	
2. 写了开始标签，漏写了结束标签

	这个情况分析起来更复杂一点。如果是无法在内部嵌套内容的标签，那么在遇到不可接受的标签时，会进行闭合。而`<div>`标签可以包括大多数标签，这种情况下，其作用域会持续到HTML结束。
	
好了，parser系列算是分析结束了，其间学到不少HTML及状态机内容，但是离实际使用比较远。下面开始select部分，这部分可能对日常使用更有意义一点。