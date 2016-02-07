/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.core;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class can be extended. Basic class for caching by urls. Works perfect
 * together with filter intercepting requests and responses, such as
 * {@link ru.cache.filters.CacheFilter}
 *
 * @author Natal Kaplya
 */
public class RestCache extends KeyRestrictedCache<String, String> {
	static Pattern queryPattern = Pattern.compile("[\\?&]([^&=?]+)=([^&=?]+)");
	static Pattern iteratingPathQuery = Pattern.compile("([^/]+?)(?=[/?]|$)");
	Pattern basicPathPattern;
	@SuppressWarnings("unused")
	private String basicPath;
	private String restPath;
	Pattern restPattern;
	private String restParam;
	private boolean urlFromRest;
	private Map<String, CachingUrl> cachingUrls = new ConcurrentHashMap<String, CachingUrl>();
	private static final Logger LOGGER = Logger.getLogger(RestCache.class.getName());
	public static final String addToExcludePrefix = "excludeAdd:";
	public static final String removeFromExcludePrefix = "excludeRemove:";
	public static final String addToDenyPrefix = "denyAdd:";
	public static final String removeFromDenyPrefix = "denyRemove:";
	// Set of params which if present do not cache
	protected Map<String, Parameter> queryParamsDeny = new ConcurrentHashMap<String, Parameter>();
	/*
	 * Set of params which are removed from cache cycle
	 */
	private Map<String, Parameter> queryParamsExclude = new ConcurrentHashMap<String, Parameter>();

	/**
	 * @return the urlFromRest
	 */
	public boolean isUrlFromRest() {
		return urlFromRest;
	}

	/**
	 * @return the restPath
	 */
	public String getRestPath() {
		return restPath;
	}

	/**
	 * This class encapsulates logic of parameter fetching in excluding and
	 * denying.
	 * 
	 * @author Kaplya Natal
	 *
	 */
	protected static class Parameter {
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parameter other = (Parameter) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		private final String value;
		private final boolean hasPattern;
		private final Pattern pattern;

		/**
		 * 
		 * @param value
		 *            Can be like 'param=regexpvalue' or just param. If has
		 *            regexpvalue, all further '=' in that regexp are considered
		 *            as part of that regexp.
		 * @throws ParameterInitializationException
		 */
		public Parameter(String value) throws ParameterInitializationException {
			if (value == null || value.length() == 0)
				throw new ParameterInitializationException("Parameter should not be empty:" + value);
			String[] parts = value.split("=");
			if (parts.length > 2)
				throw new ParameterInitializationException(
						"Parameter should have max 1 '=' sign, use URL encoding for shielding it inside your regexp:"
								+ value);

			try {
				this.value = URLDecoder.decode(URLEncoder.encode(parts[0], "UTF-8"), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new ParameterInitializationException("Parameter should have UTF-8 encoding:" + value);
			}
			if (parts.length > 1) {
				boolean hasPat = false;
				Pattern pat = null;
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < parts.length; i++) {
					try {
						sb.append(URLDecoder.decode(URLEncoder.encode(parts[i], "UTF-8"), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new IllegalArgumentException(e);
					}
				}
				try {
					pat = Pattern.compile(sb.toString());
					hasPat = true;
				} catch (PatternSyntaxException ex) {
					hasPat = false;
				}
				hasPattern = hasPat;
				pattern = pat;
			} else {
				hasPattern = false;
				pattern = null;
			}
		}

		public String getValue() {
			return value;
		}

		public boolean hasPattern() {
			return hasPattern;
		}

		public Pattern getPattern() {
			return pattern;
		}

		public boolean matches(String value) {
			if (pattern == null)
				return true;
			return pattern.matcher(value).matches();
		}

		String strRepr;

		@Override
		public String toString() {
			if (strRepr == null) {
				strRepr = this.value;
				strRepr = hasPattern ? strRepr + ":" + pattern.toString() : strRepr;
			}
			return strRepr;
		}
	}

	/**
	 * This object encapsulates logic of url to be cached. Url's being filtered
	 * are checked for mathing at least one of objects from this class.
	 * CachingUrl can have overriden exclusion and denying parameter logic. Urls
	 * are checked for equality only by url paths, no other parameters are
	 * considered.
	 * 
	 * 
	 * @author Kaplya Natal
	 *
	 */
	protected class CachingUrl {
		private final String urlPath;
		private final boolean isUniversal;
		private final boolean hasSpecialRules;
		private final Map<String, Parameter> privateExcludeParams;
		private final Map<String, Parameter> privateDenyParams;

		/**
		 * 
		 * @param url
		 * @return
		 */
		public String getUrlQueryWithExcludedParams(String url) {
			Matcher m = queryPattern.matcher(url);
			boolean hasQuery = false;
			StringBuilder sb = new StringBuilder();
			while (m.find()) {
				Parameter tempP = getPrivateExcludeParams().get(m.group(1));
				if (tempP == null || !tempP.matches(m.group(2))) {
					if (!hasQuery) {
						sb.append("?");
						hasQuery = true;
					} else {
						sb.append("&");
					}
					sb.append(m.group(1));
					sb.append("=");
					sb.append(m.group(2));
				}
			}
			// LOG.log(Level.INFO, "Url after trimming:" + sb.toString());
			return sb.toString();
		}

		public boolean hasUrlDeniedParams(String url) {
			Matcher m = queryPattern.matcher(url);
			while (m.find()) {
				Parameter tempP = getPrivateDenyParams().get(m.group(1));
				if (tempP != null && tempP.matches(m.group(2))) {
					return true;
				}
			}
			return false;
		}

		public Map<String, Parameter> getPrivateExcludeParams() {
			return privateExcludeParams;
		}

		public Map<String, Parameter> getPrivateDenyParams() {
			return privateDenyParams;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 29 * hash + (this.urlPath != null ? this.urlPath.hashCode() : 0);
			hash = 29 * hash + (this.isUniversal ? 1 : 0);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final CachingUrl other = (CachingUrl) obj;
			if ((this.urlPath == null) ? (other.urlPath != null) : !this.urlPath.equals(other.urlPath)) {
				return false;
			}
			if (this.isUniversal != other.isUniversal) {
				return false;
			}
			return true;
		}

		public CachingUrl(String url) {
			if (url == null || url.isEmpty())
				throw new IllegalArgumentException("Wrong url for caching: " + url);
			url = url.trim();
			url.replaceAll("\\s+", " ");
			String[] urlParts = url.split(" ");
			String pathPart = null;
			try {
				pathPart = URLDecoder.decode(urlParts[0], "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				throw new IllegalArgumentException(url, e1);
			}
			if (pathPart.endsWith("/"))
				pathPart = pathPart.substring(0, pathPart.length() - 1);
			if (pathPart.endsWith("/*"))
				pathPart = pathPart.substring(0, pathPart.length() - 2) + "*";
			boolean isUniversal = pathPart.endsWith("*") ? true : false;
			this.urlPath = isUniversal ? pathPart.substring(0, pathPart.length() - 1) : pathPart;
			this.isUniversal = isUniversal;
			try {
				url = URLDecoder.decode(url, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException(url, e);
			}
			boolean hasSpecRules = false;
			Map<String, Parameter> addToExcludeParams = new HashMap<String, Parameter>();
			Map<String, Parameter> removeFromExcludeParams = new HashMap<String, Parameter>();
			Map<String, Parameter> addToDenyParams = new HashMap<String, Parameter>();
			Map<String, Parameter> removeFromDenyParams = new HashMap<String, Parameter>();
			for (String part : urlParts) {
				if (part.startsWith(addToExcludePrefix)) {
					String[] spec = part.substring(addToExcludePrefix.length()).split("&");
					for (String param : spec) {
						try {
							Parameter parameter = new Parameter(param);
							addToExcludeParams.put(parameter.value, parameter);
						} catch (ParameterInitializationException e) {
							throw new IllegalArgumentException(url, e);
						}
						hasSpecRules = true;
					}
				}
				if (part.startsWith(removeFromExcludePrefix)) {
					String[] spec = part.substring(removeFromExcludePrefix.length()).split("&");
					for (String param : spec) {
						try {
							Parameter parameter = new Parameter(param);
							removeFromExcludeParams.put(parameter.value, parameter);
						} catch (ParameterInitializationException e) {
							LOGGER.log(Level.SEVERE, String.format("Parameter initialization failed:%s", param), e);
						}
						hasSpecRules = true;
					}
				}
				if (part.startsWith(addToDenyPrefix)) {
					String[] spec = part.substring(addToDenyPrefix.length()).split("&");
					for (String param : spec) {
						try {
							Parameter parameter = new Parameter(param);
							addToDenyParams.put(parameter.value, parameter);
						} catch (ParameterInitializationException e) {
							LOGGER.log(Level.SEVERE, String.format("Parameter initialization failed:%s", param), e);
						}
						hasSpecRules = true;
					}
				}
				if (part.startsWith(removeFromDenyPrefix)) {
					String[] spec = part.substring(removeFromDenyPrefix.length()).split("&");
					for (String param : spec) {
						try {
							Parameter parameter = new Parameter(param);
							removeFromDenyParams.put(parameter.value, parameter);
						} catch (ParameterInitializationException e) {
							LOGGER.log(Level.SEVERE, String.format("Parameter initialization failed:%s", param), e);
						}
						hasSpecRules = true;
					}
				}
			}
			hasSpecialRules = hasSpecRules;
			if (hasSpecialRules) {
				privateExcludeParams = new HashMap<String, Parameter>();
				privateExcludeParams.putAll(queryParamsExclude);
				removeFromMap(privateExcludeParams, removeFromExcludeParams.keySet().toArray());
				privateExcludeParams.putAll(addToExcludeParams);

				privateDenyParams = new HashMap<String, Parameter>();
				privateDenyParams.putAll(queryParamsDeny);
				removeFromMap(privateDenyParams, removeFromDenyParams.keySet().toArray());
				privateDenyParams.putAll(addToDenyParams);
			} else {
				privateExcludeParams = queryParamsExclude;
				privateDenyParams = queryParamsDeny;
			}
		}

		/**
		 * 
		 * @return true if this rule has its own params deny and exclude rules
		 */
		public boolean hasSpecialRules() {
			return hasSpecialRules;
		}

		public String getUrlPath() {
			return urlPath;
		}
		// Returns if this url allows any path appendings

		boolean isUniversal() {
			return isUniversal;
		}

		private String strRepr;

		@Override
		public String toString() {
			if (strRepr == null) {
				StringBuilder sb = new StringBuilder();
				sb.append(urlPath);
				if (isUniversal)
					sb.append("/..");
				if (hasSpecialRules) {
					sb.append(String.format("Deny:%s|", getPrivateDenyParams().values()));
					sb.append(String.format("Exclude:%s", getPrivateExcludeParams().values()));
				}
				strRepr = sb.toString();
			}
			return strRepr;
		}
	}

	private static String getParamFromUrl(String url, String param) {
		Matcher m = queryPattern.matcher(url);
		while (m.find()) {
			if (m.group(1).equals(param)) {
				return m.group(2);
			}
		}
		return null;
	}
	/*
	 * Set if this cache should take url for processing from url parameter.
	 * restPattern should be set then.
	 */

	public void setUrlFromRest(boolean status) {
		this.urlFromRest = status;
	}

	/**
	 * @return the restParam
	 */
	public String getRestParam() {
		return restParam;
	}

	/**
	 * @param restRapam
	 *            the restParam to set
	 */
	public void setRestParam(String restParam) {
		this.restParam = restParam;
		if (restParam != null) {
			urlFromRest = true;
		} else {
			urlFromRest = false;
		}
	}

	private static void removeFromMap(Map<? extends Object, ? extends Object> map, Object... keys) {
		for (Object key : keys) {
			map.remove(key);
		}
	}

	public void setRestPath(String url) throws PatternSyntaxException {
		if (!url.endsWith("/")) {
			url += "/?";
		}
		restPath = url;
		this.restPattern = Pattern.compile(url + "([^?]+)?(/|\\?|$)");
	}

	public void setBasicPath(String url) throws PatternSyntaxException {
		if (!url.endsWith("/")) {
			url += "/?";
		}
		basicPath = url;
		this.basicPathPattern = Pattern.compile(url + "([^?]+)?(/|\\?|$)");
	}

	public Map<String, Parameter> getQueryParamsDeny() {
		return queryParamsDeny;
	}

	public boolean addToQueryParamsDeny(String... denyQueries) {
		boolean hasChanged = false;
		for (String str : denyQueries) {
			try {
				int qStart = str.indexOf("=");
				String strKey = qStart == -1 ? str : str.substring(0, qStart);
				hasChanged = queryParamsDeny.put(strKey, new Parameter(str)) == null ? true : hasChanged;
			} catch (ParameterInitializationException e) {
				LOGGER.log(Level.SEVERE, String.format("Parameter initialization failed:%s", str), e);
			}
		}
		return hasChanged;
	}

	/**
	 * Adds certain url path to be cached. If ends with asterisk, then during
	 * path checking only the start path is taken into account.
	 * 
	 * @param url
	 */
	public boolean addToCachingUrls(String... cachingUrlsP) {
		boolean hasChanged = false;
		for (String str : cachingUrlsP) {
			hasChanged = addToCachingUrls(str) ? true : hasChanged;
		}
		return hasChanged;
	}

	private boolean addToCachingUrls(String url) {
		CachingUrl cachingUrl = new CachingUrl(url);
		cachingUrls.put(cachingUrl.urlPath, cachingUrl);
		return true;
	}

	public boolean addToQueryParamsExclude(String... queryParamsExcludeP) {
		boolean hasChanged = false;
		for (String str : queryParamsExcludeP) {
			try {
				hasChanged = queryParamsExclude.put(str, new Parameter(str)) == null ? false : hasChanged;
			} catch (ParameterInitializationException e) {
				LOGGER.log(Level.SEVERE, "This exclude parameter can not be created:" + str, e);
			}
		}
		return hasChanged;
	}

	public Map<String, Parameter> getQueryParamsExclude() {
		return queryParamsExclude;
	}

	public RestCache() {
		super("restCache");
	}

	protected CachingUrl getCachingUrl(String url) {
		Matcher m;
		if (!isUrlFromRest()) {
			m = basicPathPattern.matcher(url);
		} else {
			m = restPattern.matcher(url);
		}
		StringBuilder pathSb = new StringBuilder();
		List<String> path = new ArrayList<String>();
		CachingUrl matchedUrl;
		if (!m.find()) {
			return null;
		} else {
			// check if full path is contained, otherwise check for universal
			// path
			// System.out.println(cachingUrls.get(m.group(1)));
			matchedUrl = cachingUrls.get(m.group(1));
			if (matchedUrl == null && m.group(1) != null) {
				Matcher iterM = iteratingPathQuery.matcher(m.group(1));
				boolean universalMatch = false;
				while (iterM.find()) {
					pathSb.append(iterM.group(1));
					path.add(pathSb.toString());
					pathSb.append("/");
				}
				for (ListIterator<String> iterator = path.listIterator(path.size()); iterator.hasPrevious();) {
					CachingUrl urlTmp = cachingUrls.get(iterator.previous());
					if (urlTmp != null && urlTmp.isUniversal) {
						universalMatch = true;
						matchedUrl = urlTmp;
						break;
					}
				}

				if (!universalMatch) {
					return null;
				} else
					return matchedUrl;
			} else
				return matchedUrl;
		}

	}

	/**
	 * Removes obsolete query params and returns proper url for cache key.
	 *
	 * @param urlPathQuery
	 * @return
	 */
	@Override
	protected String prepareKey(String urlPathQuery) {
		// LOG.log(Level.INFO, "Url before trimming:" + urlPathQuery);
		Matcher m;
		if (!urlFromRest) {
			m = basicPathPattern.matcher(urlPathQuery);
		} else {
			m = restPattern.matcher(urlPathQuery);
		}
		StringBuilder sb = new StringBuilder();
		if (m.find()) {
			sb.append(m.group(1));
		}
		CachingUrl matchedUrl = getCachingUrl(urlPathQuery);
		if (matchedUrl == null)
			throw new IllegalStateException(String
					.format("This URL should not have been prepared due to caching set absense: %s", urlPathQuery));
		return sb.append(matchedUrl.getUrlQueryWithExcludedParams(urlPathQuery)).toString();
	}

	/**
	 * Removes certain url path from caching.
	 *
	 * @param url
	 */
	public boolean removeFromCachingUrls(String url) {
		for (Object cacheUrl : cache.getKeys()) {
			if (((String) cacheUrl).startsWith(url)) {
				cache.remove(cacheUrl);
			}
		}
		return cachingUrls.remove(url) != null ? true : false;
	}

	public String clearCacheByKey(String key) {
		StringBuilder sb = new StringBuilder();
		for (Object cacheUrl : cache.getKeys()) {
			if (((String) cacheUrl).startsWith(key)) {
				cache.remove(cacheUrl);
				sb.append(cacheUrl);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public List<String> getCachedKeys() {
		return cache.getKeys();
	}

	public String getCachingPath() {
		// System.out.println(cachingUrls.keySet());
		return cachingUrls.values().toString();
	}

	/**
	 * For now this method only cuts off rest param from url(neede for fetching
	 * queries through adapters)
	 * 
	 * @see ru.cache.core.KeyRestrictedCache#transformKeyBeforeProcessing(java.lang.
	 *      Object)
	 */
	@Override
	protected String transformKeyBeforeProcessing(String key) {
		if (isUrlFromRest()) {
			try {
				String result = getParamFromUrl(key, restParam);
				if (result == null) {
					return null;
				}
				result = URLDecoder.decode(result, "UTF-8");
				Matcher m = queryPattern.matcher(key);
				boolean hasQuery = result.contains("?");
				StringBuilder sb = new StringBuilder(result);
				while (m.find()) {
					if (!m.group(1).equals(restParam)) {
						if (!hasQuery) {
							sb.append("?");
							hasQuery = true;
						} else {
							sb.append("&");
						}
						sb.append(m.group(1));
						sb.append("=");
						sb.append(m.group(2));
					}
				}
				return sb.toString();
			} catch (UnsupportedEncodingException ex) {
				LOGGER.log(Level.SEVERE, "Url encoding differs from UTF-8, not cacheable!", ex);
			}
		}
		return key;
	}

	@Override
	protected boolean isKeyCacheable(String urlPathQuery) {
		CachingUrl matchedUrl = getCachingUrl(urlPathQuery);
		if (matchedUrl == null)
			return false;
		return !matchedUrl.hasUrlDeniedParams(urlPathQuery);
	}
}
