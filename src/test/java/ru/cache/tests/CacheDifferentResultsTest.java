package ru.cache.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cache.core.RestCache;

/**
 * Базовый тест на проверку того, что на запросы с разными неисключенными
 * параметрами кэшируется 2 разных значения.
 * 
 * @author Kaplya Natal
 *
 */
public class CacheDifferentResultsTest {
	RestCache rc;
	String url;
	String url2;

	@Before
	public void setUp() {
		configure(
				"http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication?district=59&_=1453885837331",
				"http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication?district=60&_=1453885837331",
				"schoolsapplication", "MfcDbRestServices/webresources");
	}

	void configure(String firstUrl, String secondUrl, String cachingUrl, String basicPath) {
		url = firstUrl;
		url2 = secondUrl;
		rc = new RestCache();
		rc.setBasicPath(basicPath);
		rc.addToCachingUrls(cachingUrl);
		rc.putInCache(url, "wow", null);
		rc.putInCache(url2, "wow2", null);
	}

	@After
	public void close() {
		rc.closeCache();
	}

	@Test
	public void test() {
		// assertNotEquals(rc.getFromCache(url, null), rc.getFromCache(url2,
		// null));
		assertEquals("wow", rc.getFromCache(url, null));
		assertEquals("wow2", rc.getFromCache(url2, null));
	}

}
