Jsoup代码解读之七-实现一个CSS Selector
-----

![street fighter][1]

当当当！终于来到了Jsoup的特色：CSS Selector部分。selector也是[webmagic](https://github.com/code4craft/webmagic)开发的一个重点。附上一张street fighter的图，希望以后webmagic也能挑战Jsoup!

w3c的CSS Selector规范：[http://www.w3.org/TR/CSS2/selector.html](http://www.w3.org/TR/CSS2/selector.html)

Jsoup的select包里，类结构如下：

![uml][2]

Jsoup的select核心是`Evaluator`。`Evaluator`是一个抽象类，它只有一个方法：

```java
	public abstract boolean matches(Element root, Element element);
```

注意这里传入了root，是为了某些情况下对树进行遍历时用的。在我们调用document.select(css)方法之后，Jsoup会将



<!----> [1]: http://static.oschina.net/uploads/space/2013/0830/180244_r1Vb_190591.jpg

[2]: http://static.oschina.net/uploads/space/2013/0830/184337_j85b_190591.png