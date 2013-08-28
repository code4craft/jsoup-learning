package us.codecraft.learning.parser;

import org.jsoup.Jsoup;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.util.List;

/**
 * @author code4crafter@gmail.com
 */
public class PageErrorChecker {

    public static List<ParseError> check(String url) throws IOException {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(100);
        String body = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36")
                .execute().body();
        parser.parseInput(body, url);
        List<ParseError> errors = parser.getErrors();
        return errors;
    }

    public static void main(String[] args) throws IOException {
        List<ParseError> check = check("http://www.dianping.com");
        System.out.println(check);
    }
}
