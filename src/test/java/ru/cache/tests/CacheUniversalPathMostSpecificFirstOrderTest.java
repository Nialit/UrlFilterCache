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
public class CacheUniversalPathMostSpecificFirstOrderTest {
	RestCache rc;
	String urlSpecificNotDenied = "http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication/noterm/test?term=oops";
	String urlCommonDenied = "http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication/oops?term=oops";

	@Before
	public void configure() {
		rc = new RestCache();
		rc.setBasicPath("MfcDbRestServices/webresources");
		rc.addToQueryParamsDeny("term");
		rc.addToCachingUrls("schoolsapplication/*");
		rc.addToCachingUrls("schoolsapplication/noterm* denyRemove:term");
		rc.putInCache(urlSpecificNotDenied, "wow", null);
		rc.putInCache(urlCommonDenied, "wow2", null);
	}

	@After
	public void close() {
		rc.closeCache();
	}

	@Test
	public void test() {
		// assertNotEquals(rc.getFromCache(url, null), rc.getFromCache(url2,
		// null));
		assertEquals(null, rc.getFromCache(urlCommonDenied, null));
		assertEquals("wow", rc.getFromCache(urlSpecificNotDenied, null));
	}

}
