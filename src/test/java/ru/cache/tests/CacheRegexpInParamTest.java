package ru.cache.tests;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cache.core.RestCache;
import ru.cache.core.RestCacheConfigurator;

/**
 * Базовый тест на проверку того, что на запросы с разными неисключенными
 * параметрами кэшируется 2 разных значения.
 * 
 * @author Kaplya Natal
 *
 */
public class CacheRegexpInParamTest {
	RestCache rc;
	String urlMatchedRegexpParam = "http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication/noterm/test?term=w";
	String urlNotMatchedRegexpParam = "http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication/oops?term=";
	String urlNotMatchedFirstParam = "http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication/oops?wow=test";
	String urlMatchedDenyPath = "http://172.20.54.74:8481/MfcDbRestServices/webresources/denyPath/oops?de%3Dny2=deny2";
	String urlNotMatchedDenyPath = "http://172.20.54.74:8481/MfcDbRestServices/webresources/schoolsapplication/oops?de%3Dny2=deny3";

	@Before
	public void configure() {
		Properties props = new Properties();
		props.put("enabled", "true");
		props.put("basicPath", "MfcDbRestServices/webresources");
		props.put("denyParams", "wow=test,term=.+");
		props.put("cachingUrls", "schoolsapplication/*,denyPath/* denyAdd:deny=deny&de%3Dny2=deny2");
		RestCacheConfigurator.setCache(props);
		rc = RestCacheConfigurator.getCache();
		rc.putInCache(urlMatchedRegexpParam, "wow", null);
		rc.putInCache(urlNotMatchedRegexpParam, "wow2", null);
		rc.putInCache(urlNotMatchedFirstParam, "wow3", null);
		rc.putInCache(urlMatchedDenyPath, "wow4", null);
		rc.putInCache(urlNotMatchedDenyPath, "wow5", null);
	}

	@After
	public void close() {
		rc.closeCache();
	}

	@Test
	public void test() {
		assertEquals(null, rc.getFromCache(urlMatchedRegexpParam, null));
		assertEquals("wow2", rc.getFromCache(urlNotMatchedRegexpParam, null));
		assertEquals(null, rc.getFromCache(urlNotMatchedFirstParam, null));
		assertEquals(null, rc.getFromCache(urlMatchedDenyPath, null));
		assertEquals("wow5", rc.getFromCache(urlNotMatchedDenyPath, null));
	}

}
