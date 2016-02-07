package ru.cache.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cache.core.RestCache;

/**
 * Этот тест проверяет фичу переопределения обработки параметров на конкретный
 * путь.
 * 
 * @author Kaplya Natal
 */
public class CacheOverridenUrlTest {
	RestCache rcOk;
	RestCache rcNoExclude;
	RestCache rcNoDeny;
	String urlOrgId = "/MfcDbRestServices/webresources/services/short?orgId=1249&term=&";
	String urlOrgId2 = "/MfcDbRestServices/webresources/services/short?orgId=1250&term=&";
	String urlOrgSame = "/MfcDbRestServices/webresources/services/short?orgId=1231&term=&";
	String urlOrgSame2 = "/MfcDbRestServices/webresources/services/short?orgId=1231&term=&";
	String urlLifeSit = "/MfcDbRestServices/webresources/services/short?orgId=1250&life_situation=10&term=&";

	@Before
	public void configure() {
		rcOk = new RestCache();
		rcOk.setBasicPath("MfcDbRestServices/webresources");
		rcOk.addToQueryParamsExclude(new String[] { "orgId" });
		rcOk.addToQueryParamsDeny(new String[] { "term" });
		rcOk.addToCachingUrls("services/short excludeRemove:orgId denyRemove:term denyAdd:test&life_situation");
		rcOk.putInCache(urlOrgId, "wow", null);
		rcOk.putInCache(urlOrgId2, "wow2", null);
		rcOk.putInCache(urlOrgSame, "wow3", null);
		rcOk.putInCache(urlOrgSame2, "wow4", null);
		rcOk.putInCache(urlLifeSit, "wow2", null);

		rcNoExclude = new RestCache();
		rcNoExclude.setBasicPath("MfcDbRestServices/webresources");
		rcNoExclude.addToQueryParamsExclude(new String[] { "orgId" });
		rcNoExclude.addToQueryParamsDeny(new String[] { "term" });
		rcNoExclude.addToCachingUrls("services/short denyRemove:term denyAdd:life_situation");
		rcNoExclude.putInCache(urlOrgId, "wow", null);
		rcNoExclude.putInCache(urlOrgId2, "wow2", null);
		rcNoExclude.putInCache(urlOrgSame, "wow3", null);
		rcNoExclude.putInCache(urlOrgSame2, "wow4", null);
		rcNoExclude.putInCache(urlLifeSit, "wow2", null);

		rcNoDeny = new RestCache();
		rcNoDeny.setBasicPath("MfcDbRestServices/webresources");
		rcNoDeny.addToQueryParamsExclude(new String[] { "orgId" });
		rcNoDeny.addToQueryParamsDeny(new String[] { "term" });
		rcNoDeny.addToCachingUrls("services/short denyRemove:term excludeRemove:orgId");
		rcNoDeny.putInCache(urlOrgId, "wow", null);
		rcNoDeny.putInCache(urlOrgId2, "wow2", null);
		rcNoDeny.putInCache(urlOrgSame, "wow3", null);
		rcNoDeny.putInCache(urlOrgSame2, "wow4", null);
		rcNoDeny.putInCache(urlLifeSit, "wow2", null);

	}

	@After
	public void close() {
		rcOk.closeCache();
	}

	@Test
	public void test() {
		// assertNotEquals(rc.getFromCache(url, null), rc.getFromCache(url2,
		// null));
		assertEquals("wow", rcOk.getFromCache(urlOrgId, null));
		assertEquals("wow2", rcOk.getFromCache(urlOrgId2, null));
		assertEquals("wow4", rcOk.getFromCache(urlOrgSame, null));
		assertEquals("wow4", rcOk.getFromCache(urlOrgSame2, null));
		assertEquals(null, rcOk.getFromCache(urlLifeSit, null));

		assertEquals("wow4", rcNoExclude.getFromCache(urlOrgId, null));
		assertEquals("wow4", rcNoExclude.getFromCache(urlOrgId2, null));
		assertEquals("wow4", rcNoExclude.getFromCache(urlOrgSame, null));
		assertEquals("wow4", rcNoExclude.getFromCache(urlOrgSame2, null));
		assertEquals(null, rcNoExclude.getFromCache(urlLifeSit, null));

		assertEquals("wow", rcNoDeny.getFromCache(urlOrgId, null));
		assertEquals("wow2", rcNoDeny.getFromCache(urlOrgId2, null));
		assertEquals("wow4", rcNoDeny.getFromCache(urlOrgSame, null));
		assertEquals("wow4", rcNoDeny.getFromCache(urlOrgSame2, null));
		assertEquals("wow2", rcNoDeny.getFromCache(urlLifeSit, null));
	}

}
