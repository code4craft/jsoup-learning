package us.codecraft.learning.select;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * @author code4crafter@gmail.com
 */
public class SelectorTest {

    public static void main(String[] args) {
        String html = "<body>\n" +
                " <textarea>\n" +
                "        &lt;!-- Text --&gt;\n" +
                "        xxx\n" +
                "    </textarea> \n" +
                " <div> \n" +
                "  <table> \n" +
                "   <!-- InTable --> \n" +
                "   <!-- InTableText --> xxx \n" +
                "   <tbody> \n" +
                "    <tr> \n" +
                "     <!-- InRow --> \n" +
                "     <td> \n" +
                "      <!-- InCell --> </td> \n" +
                "    </tr> \n" +
                "   </tbody> \n" +
                "  </table> \n" +
                " </div> \n" +
                "</body>";
        Parser parser = Parser.htmlParser();
        Document document = parser.parseInput(html, "");
        Elements select = document.select("body div");
        System.out.println(select);
    }
}
