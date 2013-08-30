package us.codecraft.learning.parser;

import org.jsoup.nodes.Document;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.Parser;

import java.util.List;

/**
 * @author code4crafter@gmail.com
 */
public class ParserCorrectorTest {

    public static void main(String[] args) {
        String htmlWithDivUnclosed = "<body>\n" +
                " <textarea>\n" +
                "        &lt;!-- Text --&gt;\n" +
                "        xxx\n" +
                "    </textarea> \n" +
                " <div> \n" +
                " <div>\n" +
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
        parser.setTrackErrors(100);
        Document document = parser.parseInput(htmlWithDivUnclosed, "");
        List<ParseError> errors = parser.getErrors();
        System.out.println(errors);

    }
}
