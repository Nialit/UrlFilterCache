/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import ru.cache.core.RestCache;
import ru.cache.core.RestCacheConfigurator;

/**
 *This cache intercepts URL from any web-app it is connected to. 
On application start it is initialized with settings defined in cacheopts.properties.
E.g:
enabled=true   - if not 'true', do no caching
jerseyEnabled=true   - if not 'true', do no start(this) REST facade
excludeParams=excludeparam  -  these params would be cut off on url cacheable check('../aUrl?excludeparams=3' --> '../aUrl')
denyParams=denyparam  - if one of these params presents, do not cache even if pattern matches('../bUrl?denyparam' --> do not cache)
cachingUrls=aUrl excludeRemove:orgId denyAdd:denyparam=.+&anotherdeny,bUrl   -  here checking patterns go.
Can include regexp for parameter values like 'denyparam=.+', here only non-empty parameter with name 'denyparam' matches. 
The most specified is in priority: if we get 'cacheingUrls=a/b excludeAdd=specifiedExclude,a*', 
then url '../a/b?specifiedExcude=1' turns into '../a/b', but '../a/c?specifiedExcude=1' does not change.
basicPath=Pgu/MfcRestAdapterServlet  - this is basic path prepending Url of request
restPath=MfcDbRestServices/webresources  - if this parameter specified, then url parsing is made upon 
url taken from 'restParam' and with this value as basicPath
restParam=rest

'cacheopts.properties' file must present in web-app resources, ultimately in the root of .war package.
 * @author Natal Kaplya
 */
public class CacheRestFacade {

	private static final String notActiveMsg = "EhCache is not active";
	private static final String infoMsg = "Another commands:\n"
			+ "removecachepath?url={} : all cache for this url(including query params) gonna be cleared and won't be cached from now on.\n"
			+ "clearcache?url={} : all cache for this url(including query params) gonna be cleared.\n"
			+ "clearall : all cache gonna be cleared.\n" + "addcachepath?url={} : add new path for caching\n"
			+ String.format(
					"If Url path is followed by one of the following prefixes, default params fetching is overriden for this Url: %s | %s | %s | %s \nValues of params should be splitted by '&'.\n",
					RestCache.addToExcludePrefix, RestCache.removeFromExcludePrefix, RestCache.addToDenyPrefix,
					RestCache.removeFromDenyPrefix)
			+ "Url params and paths should be urlencoded for avoiding [/s=,&]\n"
			+ "Example url: {denyPath/* denyAdd:deny=deny&de%3Dny2=deny2} adds 2 params, the second one is 'de=ny2':'deny2'"
			+ "This cache intercepts URL from any web-app it is connected to.\n"
			+ "On application start it is initialized with settings defined in cacheopts.properties.\n" + "E.g.:\n"
			+ "enabled=true   - if not 'true', do no caching\n"
			+ "jerseyEnabled=true   - if not 'true', do no start(this) REST facade\n"
			+ "excludeParams=excludeparam  -  these params would be cut off on url cacheable check('../aUrl?excludeparams=3' --> '../aUrl')\n"
			+ "denyParams=denyparam  - if one of these params presents, do not cache even if pattern matches('../bUrl?denyparam' --> do not cache)\n"
			+ "cachingUrls=aUrl excludeRemove:orgId denyAdd:denyparam=.+&anotherdeny,bUrl   -  here checking patterns go.\n"
			+"Can include regexp for parameter values like 'denyparam=.+', here only non-empty parameter with name 'denyparam' matches.\n"
			+ "The most specified is in priority: if we get 'cacheingUrls=a/b excludeAdd=specifiedExclude,a*', \n"
			+ "then url '../a/b?specifiedExcude=1' turns into '../a/b', but '../a/c?specifiedExcude=1' does not change.\n"
			+ "basicPath=Pgu/MfcRestAdapterServlet  - this is basic path prepending Url of request\n"
			+ "restPath=MfcDbRestServices/webresources  - if this parameter specified, then url parsing is made upon \n"
			+ "url taken from 'restParam' and with this value as basicPath\n" + "restParam=rest\n\n"
			+ "'cacheopts.properties' file must present in web-app in the root of .war package.\n(just put it into src/main/resources and ensure it is properly imported by maven)";

	/**
	 * Get all the info about current caching.
	 */
	@GET
	@Path("info")
	@Produces({"text/plain"})
	public String getInfo() {
		if (!RestCacheConfigurator.isBroken() && RestCacheConfigurator.isActive()) {
			StringBuilder sb = new StringBuilder();
			sb.append(infoMsg);
			sb.append("\n\n");
			sb.append("Caching path:\n");
			sb.append(RestCacheConfigurator.getCache().getCachingPath());
			sb.append("\nQuery params excluded(discarded during processing from actual url path):\n");
			sb.append(RestCacheConfigurator.getCache().getQueryParamsExclude().toString());
			sb.append("\nUrls containing these queries are not cached:\n");
			sb.append(RestCacheConfigurator.getCache().getQueryParamsDeny().toString());
			boolean isRest = RestCacheConfigurator.getCache().isUrlFromRest();
			sb.append("\nRest is used:\n");
			sb.append(isRest);
			if (isRest) {
				sb.append("\nRest param:\n");
				sb.append(RestCacheConfigurator.getCache().getRestParam());
				sb.append("\nRest path:\n");
				sb.append(RestCacheConfigurator.getCache().getRestPath());
			}
			sb.append("\nClass for caching:\n");
			sb.append(RestCacheConfigurator.getCache().getClass().getName());
			sb.append("\nCached Keys:\n");
			sb.append(RestCacheConfigurator.getCache().getCachedKeys().toString());

			return sb.toString();
		} else {
			return notActiveMsg;
		}
	}

	/**
	 * Removes entry from caching url's set and clears cache by this entry.
	 * 
	 * @param url
	 */
	@GET
	@Path("removecachepath")
	@Produces({"text/plain"})
	public String removeCachePath(@QueryParam("url") String url) {
		if (!RestCacheConfigurator.isBroken() && RestCacheConfigurator.isActive()) {
			if (url != null && !url.isEmpty()) {
				return Boolean.toString(RestCacheConfigurator.getCache().removeFromCachingUrls(url));
			} else {
				return "false";
			}
		} else {
			return notActiveMsg;
		}
	}

	/**
	 * 
	 * Clear cache by matching url. Use when your back storage has been changed.
	 * 
	 * @param url
	 * @return
	 */
	@GET
	@Path("clearcache")
	@Produces({"text/plain"})
	public String clearCacheByKey(@QueryParam("url") String url) {
		if (!RestCacheConfigurator.isBroken() && RestCacheConfigurator.isActive()) {
			if (url != null && !url.isEmpty()) {
				return RestCacheConfigurator.getCache().clearCacheByKey(url);
			} else {
				return "false";
			}
		} else {
			return notActiveMsg;
		}
	}

	/**
	 * Clear all the cache.
	 */
	@GET
	@Path("clearall")
	@Produces({"text/plain"})
	public String clearAll() {
		if (!RestCacheConfigurator.isBroken() && RestCacheConfigurator.isActive()) {
			RestCacheConfigurator.getCache().clearCache();
			return "true";
		} else {
			return notActiveMsg;
		}
	}

	/**
	 * Add url to be cached during runtime.
	 * 
	 * @param url
	 */
	@GET
	@Path("addcachepath")
	@Produces({"text/plain"})
	public String addCachingUrl(@QueryParam("url") String url) {
		try {
			url = URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!RestCacheConfigurator.isBroken() && RestCacheConfigurator.isActive()) {
			return Boolean.toString(RestCacheConfigurator.getCache().addToCachingUrls(url));
		} else {
			return notActiveMsg;
		}
	}
}
