import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import org.junit.Test;

import com.why.spider.LinkQueue;

public class spider {

	public static final String SPIDER_IDENTITY = "www.cncrk.com";
    public static final String NAME = "起点软件";


    // 已访问的 url 集合
	private Set visitedUrl = new HashSet();
	// 待访问的 url 集合
	private Queue unVisitedUrl = new PriorityQueue();

    public spider() {
        super(SPIDER_IDENTITY, NAME);
    }

    @Override
    protected void parseHtml(String url,Integer marketId,Integer categoryId) {
        String visitUrl = null;

        // 将起始url加入未访问队列
        addUnvisitedUrl(url);
        // 判断未访问队列是否为空


        while (!unVisitedUrl.isEmpty()) {
            visitUrl = (String) unVisitedUrl.poll();
            visitedUrl.add(url);
            String html = getHTML(visitUrl);
            Document doc = Jsoup.parse(html);
            doc.setBaseUri(visitUrl);
            parsePerPage(doc,marketId,categoryId);
            final String link = parseNextPageLink(doc);
            if (StringUtils.isNotEmpty(link)) addUnvisitedUrl(link);
        }
    }

    public void addUnvisitedUrl(String url) {
		if (url != null && !url.trim().equals("") && !visitedUrl.contains(url) && !unVisitedUrl.contains(url))
			unVisitedUrl.add(url);
	}

    private String parseNextPageLink(Document doc) {
        Element currEle = doc.select("div.pages p a.on").last();
        if (currEle != null) {
        	Element nextPage = currEle.nextElementSibling();
        	if(nextPage == null){
        		return null;
        	}
            return buildURL(nextPage.attr("href"));
        }
        return null;
    }

    private String buildURL(String href) {
        return String.format("http://%s%s", SPIDER_IDENTITY, href);
    }

    private void parsePerPage(Document docPage,Integer marketId,Integer categoryId) {
        if (docPage != null) {
            Elements appLinks = docPage.select("ul.ul-pic-w4.ul-pic--w4.tab-1.tab-2 li h3 a");
            for (Element appLink : appLinks) {
                final String appURL = buildURL(appLink.attr("href"));
                String appName = null;
				String appVersion = null;
				String author = null;
				String downloads = null;
				String profile = null;
				String publishDate = null;
				String download = null;
				try {
					Document docApp = Jsoup.parse(getHTML(appURL));
					appName = appLink.ownText().trim();
					appVersion = null;
					author = null;
					downloads = null;
					publishDate = null;
					Element downloadEle = docApp.select("div.dnyq > dl > dd.dtj > a").first();
					download = (downloadEle != null) ? downloadEle.attr("href") : null;
				} catch (Exception e) {
					logger.error("html解析错误{}---{}", appName,appURL, e);
				}
				if(StringUtils.isBlank(download)){
					continue;
				}
                ApkInfo apkInfo=new ApkInfo(appName, appVersion, author, null, profile, publishDate, appURL, download, null, null, downloads);
                System.out.println(apkInfo);
            }
        }
    }

    protected String getHTML(final String url) {
        String html = null;
        html = HttpUtils.get(url);
        if (html != null) {
            while (html.contains("<html><script>document.cookie=")) {
                String start = "<html><script>document.cookie=\"";
                String cooike = html.substring(html.indexOf(start) + start.length(),
                        html.indexOf("\";document.location.reload();</script></html>"));

                html = HttpUtils.getByCooike(url, cooike);

                if (!html.contains("<html><script>document.cookie="))
                    return html;
            }
        }
        return html;
    }
    
    public static String get(String url, String encoding, int socketTimeout, int connectTimeout) {
        String result = null;

        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).build();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(requestConfig);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            // int status = response.getStatusLine().getStatusCode();
            // if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            try {
                if (entity != null) {
                    result = EntityUtils.toString(entity, encoding);
                }
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
            // }

        } catch (ClientProtocolException e) {
            logger.error(String.format("exception for url %s", url), e);
        } catch (IOException e) {
            logger.error(String.format("exception for url %s", url), e);
        } finally {
            httpGet.releaseConnection();
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                ;
            }
        }
        return result;
    }

    @Test
    public void test() {
        parseHtml("http://www.cncrk.com/shouji/r_17_987.html",1,1);
       // System.out.println(parseHtml(getHTML("http://tv.45app.com/game/")));

        //HttpUtils.writeToFile("http://tv.45app.com/down/2558.htm", new File("d:/123123.apk"), 5000, 5000);
    }
}