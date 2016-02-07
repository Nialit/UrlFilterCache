package ru.cache.tests;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cache.core.RestCache;

/**
 * 
 * Базовый тест на проверку того, что на запросы с разными неисключенными
 * параметрами кэшируется 2 разных значения. Для параметров в рестах.
 * 
 * @author Kaplya Natal
 *
 */
public class CacheRestDifferentResultsTest {
	RestCache rc;
	String url;
	String url2;

	@Before
	public void setUp() {
		configure(
				"http://localhost:8080/Pgu/MfcRestAdapterServlet?rest=%2FMfcDbRestServices%2Fwebresources%2Fschoolsapplication?district=59&_=1453885837331",
				"http://localhost:8080/Pgu/MfcRestAdapterServlet?rest=%2FMfcDbRestServices%2Fwebresources%2Fschoolsapplication?district=60&_=1453885837331",
				"schoolsapplication", "Pgu/MfcRestAdapterServlet", "rest", "MfcDbRestServices/webresources");
	}

	void configure(String firstUrl, String secondUrl, String cachingUrl, String basicPath, String restParam,
			String restPath) {
		url = firstUrl;
		url2 = secondUrl;
		rc = new RestCache();
		rc.setBasicPath(basicPath);
		rc.addToCachingUrls(cachingUrl);
		rc.setRestParam(restParam);
		rc.setRestPath(restPath);
		rc.putInCache(url, "wow", null);
		rc.putInCache(url2, "wow2", null);
	}

	@After
	public void close() {
		rc.closeCache();
	}

	@Test
	public void test() {
		assertEquals("wow",rc.getFromCache(url, null));
		assertEquals("wow2",rc.getFromCache(url2, null));
	}

}
