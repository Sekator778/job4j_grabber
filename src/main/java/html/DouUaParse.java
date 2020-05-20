package html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 */

public class DouUaParse {

    public static void main(String[] args) throws Exception {
        Document doc = Jsoup.connect("https://jobs.dou.ua/vacancies/?category=Java").get();
        Elements row = doc.select(".title");
        Element href = null;
        Element cities = null;
        for (Element td : row) {
                href = td.child(0);
                cities = td.child(2);
            System.out.println(href.attr("href"));
            System.out.println(href.text());
            System.out.println(cities.getAllElements().text());

        }
    }
}
