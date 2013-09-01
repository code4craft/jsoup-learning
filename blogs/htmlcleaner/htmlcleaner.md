htmlcleaner代码学习
---
相比Jsoup，htmlcleaner支持XPath进行抽取，也是挺有用的。

htmlcleaner托管在sourceforge下[http://htmlcleaner.sourceforge.net/‎](http://htmlcleaner.sourceforge.net/‎
)，由于某种原因，访问sourceforge不是那么顺畅，最后选了这个比较新的github上的fork:[https://github.com/amplafi/htmlcleaner](https://github.com/amplafi/htmlcleaner)。

htmlcleaner的包结构与Jsoup还是有些差距，一开始就被一字排开的类给吓到了。

htmlcleaner仍然有一套自己的树结构，继承自:`HtmlNode`。但是它提供了到`org.w3c.dom.Document`和`org.jdom2.Document`的转换。

`HtmlTokenizer`是词法分析部分，有状态但是没用状态机，而是用了一些基本类型来保存状态，例如：

    public class HtmlTokenizer {

        private BufferedReader _reader;
        private char[] _working = new char[WORKING_BUFFER_SIZE];

        private transient int _pos;
        private transient int _len = -1;
        private transient int _row = 1;
        private transient int _col = 1;
        

        private transient StringBuffer _saved = new StringBuffer(512);

        private transient boolean _isLateForDoctype;
        private transient DoctypeToken _docType;
        private transient TagToken _currentTagToken;
        private transient List<BaseToken> _tokenList = new ArrayList<BaseToken>();
        private transient Set<String> _namespacePrefixes = new HashSet<String>();

        private boolean _asExpected = true;

        private boolean _isScriptContext;
    }

浓烈的面向过程编程的味道。

`Tokenize`之后就是简单的用栈将树组合起来。

测试了一下，一个44k的文档，用Jsoup做parse是3.5ms，而htmlcleaner是7.9ms，差距在一倍左右。

XPath部分也是云里雾里，