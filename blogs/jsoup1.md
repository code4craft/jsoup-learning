Jsoup代码解读之一-概述
------
>今天看到一个用python写的抽取正文的东东，美滋滋的用Java实现了一番，放到了webmagic里，然后发现Jsoup里已经有了…觉得自己各种不靠谱啊！算了，静下心来学学好东西吧！

Jsoup是Java世界用作html解析和过滤的不二之选。支持将html解析为DOM树、支持CSS Selector形式选择、支持html过滤，本身还附带了一个Http下载器。

## 概述

Jsoup的代码相当简洁，Jsoup总共53个类，且没有任何第三方包的依赖，对比最终发行包9.8M的SAXON，实在算得上是短小精悍了。

```shell
    jsoup
    ├── examples #样例，包括一个将html转为纯文本和一个抽取所有链接地址的例子。    
    ├── helper #一些工具类，包括读取数据、处理连接以及字符串转换的工具
    ├── nodes #DOM节点定义
    ├── parser #解析html并转换为DOM树
    ├── safety #安全相关，包括白名单及html过滤
    └── select #选择器，支持CSS Selector以及NodeVisitor格式的遍历
```
    
## 使用

Jsoup的入口是`Jsoup`类。examples包里提供了两个例子，解析html后，分别用CSS Selector以及NodeVisitor来操作Dom元素。

这里用`ListLinks`里的例子来说明如何调用Jsoup：

```java
    public static void main(String[] args) throws IOException {
        Validate.isTrue(args.length == 1, "usage: supply url to fetch");
        String url = args[0];
        print("Fetching %s...", url);

        // 下载url并解析成html DOM结构
        Document doc = Jsoup.connect(url).get();
        // 使用select方法选择元素，参数是CSS Selector表达式
        Elements links = doc.select("a[href]");

        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            //使用abs:前缀取绝对url地址
            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
        }
    }
```
    
Jsoup使用了自己的一套DOM代码体系，这里的Elements、Element等虽然名字和概念都与Java XML API`org.w3c.dom`类似，但并没有代码层面的关系。就是说你想用XML的一套API来操作Jsoup的结果是办不到的，但是正因为如此，才使得Jsoup可以抛弃xml里一些繁琐的API，使得代码更加简单。
     
还有一种方式是通过`NodeVisitor`来遍历DOM树，这个在对整个html做分析和替换时比较有用：

```java
    public interface NodeVisitor {

        //遍历到节点开始时，调用此方法
        public void head(Node node, int depth);

        //遍历到节点结束时(所有子节点都已遍历完)，调用此方法
        public void tail(Node node, int depth);
    }
```
    
`HtmlToPlainText`的例子说明了如何使用NodeVisitor来遍历DOM树，将html转化为纯文本，并将需要换行的标签替换为换行\\n：

```java
    public static void main(String... args) throws IOException {
        Validate.isTrue(args.length == 1, "usage: supply url to fetch");
        String url = args[0];

        // fetch the specified URL and parse to a HTML DOM
        Document doc = Jsoup.connect(url).get();

        HtmlToPlainText formatter = new HtmlToPlainText();
        String plainText = formatter.getPlainText(doc);
        System.out.println(plainText);
    }

    public String getPlainText(Element element) {
        //自定义一个NodeVisitor - FormattingVisitor
        FormattingVisitor formatter = new FormattingVisitor();
        //使用NodeTraversor来装载FormattingVisitor
        NodeTraversor traversor = new NodeTraversor(formatter);
        //进行遍历
        traversor.traverse(element);
        return formatter.toString();
    }
```

下一节将从DOM结构开始对Jsoup代码进行分析。