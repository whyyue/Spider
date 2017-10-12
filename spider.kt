package com.why.spider

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ConnectException
import java.nio.charset.CodingErrorAction
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.util.HashSet
import java.util.PriorityQueue
import java.util.Queue
import java.util.Random
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.NoHttpResponseException
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.config.ConnectionConfig
import org.apache.http.config.MessageConstraints
import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.config.SocketConfig
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.SSLContexts
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils

class spider {
	// 已访问的 url 集合
	@SuppressWarnings("rawtypes")
	private val visitedUrl = HashSet()
	// 待访问的 url 集合
	@SuppressWarnings("rawtypes")
	private val unVisitedUrl = PriorityQueue()

	@SuppressWarnings("unchecked")
	protected fun parseHtml(url: String?, marketId: Integer?, categoryId: Integer?) {
		val visitUrl: String? = null
// 将起始url加入未访问队列
		addUnvisitedUrl(url)
// 判断未访问队列是否为空
		while (!unVisitedUrl!!.isEmpty()) {
			visitUrl = unVisitedUrl!!.poll() as String
			visitedUrl!!.add(url)
			val time_start = System.currentTimeMillis() //
			val html = getHTML(visitUrl)
			val time_end = System.currentTimeMillis() //
			WEB_REQUEST_TIME = WEB_REQUEST_TIME + (time_end - time_start) //
			val doc = Jsoup.parse(html)
			doc!!.setBaseUri(visitUrl)
			parsePerPage(doc, marketId, categoryId)
			val link = parseNextPageLink(doc)
			if (StringUtils.isNotEmpty(link)) addUnvisitedUrl(link)
		}
	}

	@SuppressWarnings("unchecked")
	fun addUnvisitedUrl(url: String?) {
		if (url != null && !url!!.trim().equals("") && !visitedUrl!!.contains(url) && !unVisitedUrl!!.contains(url))
			unVisitedUrl!!.add(url)
	}

	private fun parseNextPageLink(doc: Document?): String? {
		val currEle = doc!!.select("div.pages p a.on").last()
		if (currEle != null) {
			val nextPage = currEle!!.nextElementSibling()
			if (nextPage == null) {
				return null
			}
			return buildURL(nextPage!!.attr("href"))
		}
		return null
	}

	private fun buildURL(href: String?): String? {
		return String.format("http://%s%s", SPIDER_IDENTITY, href)
	}

	private fun parsePerPage(docPage: Document?, marketId: Integer?, categoryId: Integer?) {
		if (docPage != null) {
			val appLinks = docPage!!.select("ul.ul-pic-w4.ul-pic--w4.tab-1.tab-2 li h3 a")
			for (appLink in appLinks!!) {
				val appURL = buildURL(appLink!!.attr("href"))
				val appName: String? = null
				val appVersion: String? = null
				val author: String? = null
				val downloads: String? = null
				val publishDate: String? = null
				val download: String? = null
				try {
					val time_start = System.currentTimeMillis() //
					val html = getHTML(appURL)
					val time_end = System.currentTimeMillis() //
					WEB_REQUEST_TIME = WEB_REQUEST_TIME + (time_end - time_start) //
					System.out.println("[WEB_REQUEST_TIME]parsePerPage:" + String.valueOf(WEB_REQUEST_TIME))
					val docApp = Jsoup.parse(html)
					appName = appLink!!.ownText().trim()
					appVersion = null
					author = null
					downloads = null
					publishDate = null
					val downloadEle = docApp!!.select("div.dnyq > dl > dd.dtj > a").first()
					download = if ((downloadEle != null)) downloadEle!!.attr("href") else null
				} catch (e: Exception) {
					System.out.println("html解析错误{}---{}" + appName + appURL + e)
				}
				if (StringUtils.isBlank(download)) {
					continue
				}
// ApkInfo apkInfo=new ApkInfo(appName, appVersion, author, null, profile, publishDate, appURL, download, null, null, downloads);
				System.out.println("appName=" + appName + "appVersion=" + appVersion + "author=" + author + "publishDate=" + publishDate + "appURL=" + appURL + "download" + download + "downloads=" + downloads)
			}
		}
	}

	protected fun getHTML(url: String?): String? {
		val html: String? = null
		html = get(url)
		if (html != null) {
			while (html!!.contains("<html><script>document.cookie=")) {
				val start = "<html><script>document.cookie=\""
				val cooike = html!!.substring(html!!.indexOf(start) + start!!.length(),
						html!!.indexOf("\";document.location.reload();</script></html>"))
				html = getByCooike(url, cooike)
				if (!html!!.contains("<html><script>document.cookie="))
					return html
			}
		}
		return html
	}

	@Test
	fun test() {
		val time_start = System.currentTimeMillis() //
		parseHtml("http://www.cncrk.com/shouji/r_17_987.html", 1, 1)
		val time_end = System.currentTimeMillis() //
		val time_consume = (time_end - time_start).toFloat()
		time_consume = time_consume - WEB_REQUEST_TIME
		System.out.println("[Java]time consumed is: " + String.valueOf(time_consume / 1000))
		System.out.println(String.valueOf(time_start))
		System.out.println(String.valueOf(time_end))
// System.out.println(parseHtml(getHTML("http://tv.45app.com/game/")));
//HttpUtils.writeToFile("http://tv.45app.com/down/2558.htm", new File("d:/123123.apk"), 5000, 5000);
	}

	companion object {
		val SPIDER_IDENTITY: String? = "www.cncrk.com"
		val NAME: String? = "起点软件"
		var WEB_REQUEST_TIME = 0.000.toFloat()
		val USER_AGENT: String? = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19"
		val TIMEOUT = 1000 * 30
		private val httpClient: CloseableHttpClient? = null
		private val connManager: PoolingHttpClientConnectionManager? = null
// public spider() {
// super(SPIDER_IDENTITY, NAME);
// }

		init {
			try {
				val sslContext = SSLContexts.custom().useTLS().build()
				sslContext!!.init(null, arrayOf<TrustManager>(object : X509TrustManager() {
					val acceptedIssuers: Array<X509Certificate>?
						get() {
							return null
						}

					fun checkClientTrusted(certs: Array<X509Certificate>?, authType: String?) {}
					fun checkServerTrusted(certs: Array<X509Certificate>?, authType: String?) {}
				}), null)
				val socketFactoryRegistry = RegistryBuilder
						.create<ConnectionSocketFactory>().register("http", PlainConnectionSocketFactory.INSTANCE)
						.register("https", SSLConnectionSocketFactory(sslContext)).build()
				connManager = PoolingHttpClientConnectionManager(socketFactoryRegistry)
// Create socket configuration
				val socketConfig = SocketConfig.custom().setTcpNoDelay(true).build()
				connManager!!.setDefaultSocketConfig(socketConfig)
// Create message constraints
				val messageConstraints = MessageConstraints.custom().setMaxHeaderCount(200)
						.setMaxLineLength(2000).build()
// Create connection configuration
				val connectionConfig = ConnectionConfig.custom()
						.setMalformedInputAction(CodingErrorAction.IGNORE)
						.setUnmappableInputAction(CodingErrorAction.IGNORE).setMessageConstraints(messageConstraints)
						.build()
				connManager!!.setDefaultConnectionConfig(connectionConfig)
				connManager!!.setMaxTotal(512)
				connManager!!.setDefaultMaxPerRoute(150)
				httpClient = HttpClients.custom().setUserAgent(USER_AGENT).setConnectionManager(connManager)
						.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy()).setRetryHandler(RetryHandler())
						.build()
			} catch (e: KeyManagementException) {
				System.out.println("httpUtils exception:" + e!!)
			} catch (e: NoSuchAlgorithmException) {
				System.out.println("httpUtils exception:" + e!!)
			}
		}

		fun getByCooike(url: String?, cooike: String?): String? {
			return getByCooike(url, "UTF-8", TIMEOUT, TIMEOUT, cooike)
		}

		fun getByCooike(url: String?, encoding: String?, socketTimeout: Int, connectTimeout: Int, cooike: String?): String? {
			val result: String? = null
			val requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
					.setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).build()
			val httpGet = HttpGet(url)
			httpGet!!.setConfig(requestConfig)
			httpGet!!.setHeader("Cookie", cooike)
			val response: CloseableHttpResponse? = null
			try {
				response = httpClient!!.execute(httpGet)
// int status = response.getStatusLine().getStatusCode();
// if (status >= 200 && status < 300) {
				val entity = response!!.getEntity()
				try {
					if (entity != null) {
						result = EntityUtils.toString(entity, encoding)
					}
				} finally {
					EntityUtils.consumeQuietly(entity)
				}
// }
			} catch (e: ClientProtocolException) {
				System.out.println(String.format("exception for url %s", url) + e!!)
			} catch (e: IOException) {
				System.out.println(String.format("exception for url %s", url) + e!!)
			} finally {
				httpGet!!.releaseConnection()
				try {
					if (response != null) {
						response!!.close()
					}
				} catch (e: IOException) {
				}
			}
			return result
		}

		@JvmOverloads fun get(url: String?, encoding: String? = "UTF-8", socketTimeout: Int = TIMEOUT, connectTimeout: Int = TIMEOUT): String? {
			val result: String? = null
			val requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
					.setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).build()
			val httpGet = HttpGet(url)
			httpGet!!.setConfig(requestConfig)
			val response: CloseableHttpResponse? = null
			try {
				response = httpClient!!.execute(httpGet)
// int status = response.getStatusLine().getStatusCode();
// if (status >= 200 && status < 300) {
				val entity = response!!.getEntity()
				try {
					if (entity != null) {
						result = EntityUtils.toString(entity, encoding)
					}
				} finally {
					EntityUtils.consumeQuietly(entity)
				}
// }
			} catch (e: ClientProtocolException) {
				System.out.println(String.format("exception for url %s", url) + e!!)
			} catch (e: IOException) {
				System.out.println(String.format("exception for url %s", url) + e!!)
			} finally {
				httpGet!!.releaseConnection()
				try {
					if (response != null) {
						response!!.close()
					}
				} catch (e: IOException) {
					System.out.println(String.format("exception for url %s", url) + e!!)
				}
			}
			return result
		}
	}
}

internal class RetryHandler : HttpRequestRetryHandler {
	@Override
	fun retryRequest(e: IOException?, executionCount: Int, httpContext: HttpContext?): Boolean {
		val clientContext = HttpClientContext.adapt(httpContext)
		val request = clientContext!!.getRequest()
		val url = request!!.getRequestLine().getUri()
		try {
			logger!!.warn("连接超时，重试等待中，url=" + url!!)
			Thread.sleep(RETRY_TIME_OUT)
		} catch (ex: InterruptedException) {
			logger!!.error("连接超时等待中出错", ex)
		}
		logger!!.warn("重试次数=" + executionCount + "，url=" + url)
		if (executionCount >= MAX_RETRY_TIME) {
// Do not retry if over max retry count
			return false
		}
		if (e is NoHttpResponseException || e is ConnectException) {
// Retry if the server dropped connection on us
			try {
				Thread.sleep(randInt(2000, 5000))
			} catch (e1: InterruptedException) {
				logger!!.error("连接超时等待中出错", e1)
			}
			return true
		}
		if (e is SSLHandshakeException) {
// Do not retry on SSL handshake exception
			return false
		}
		val idempotent = !(request is HttpEntityEnclosingRequest)
		if (idempotent) {
// Retry if the request is considered idempotent
			return true
		}
		return false
	}

	companion object {
		private val logger = LoggerFactory.getLogger(RetryHandler::class.java)
		val RETRY_TIME_OUT = 3000
		val MAX_RETRY_TIME = 5
		fun randInt(min: Int, max: Int): Int {
			val rand = Random()
			return rand!!.nextInt((max - min) + 1) + min
		}
	}
}