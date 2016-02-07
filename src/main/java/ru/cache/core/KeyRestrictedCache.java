/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.core;

import javax.servlet.ServletRequest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Generic class for basic caching made for extension. Has methods for checking
 * condition if entity is supposed to be cached. Also makes preparations on key
 * entity before adding it into the cache.
 *
 * @author Natal Kaplya
 */
public abstract class KeyRestrictedCache<K, V> {

	public static final String isCacheableAttributeName = "isCacheable";
	private final CacheManager manager;
	protected final Cache cache;

	public KeyRestrictedCache(String cacheName) {
		manager = CacheManager.newInstance();// Get config from default
												// ehcache.xml from classpath
		cache = manager.getCache(cacheName);// must configure in ehcache.xml
		if (cache == null) {
			throw new IllegalArgumentException("Cache with this name does not exist: " + cacheName);
		}
	}

	/**
	 * Should be overriden for caching restrictions
	 *
	 * @param url
	 * @return true if key is supposed to be cached
	 */
	private boolean isCacheable(K url, ServletRequest request) {
		return isKeyCacheable(url) && isRequestCacheable(request);
	}

	protected boolean isRequestCacheable(ServletRequest request) {
		return true;
	}

	protected boolean isKeyCacheable(K urlPathQuery) {
		return true;
	}

	/**
	 * Cleares cache. Call after source updates for recache.
	 */
	public void clearCache() {
		cache.removeAll();
	}

	/**
	 * Return value from the cache if one is there. Return null otherwise.
	 *
	 * @param key
	 * @return Value from the cache or null
	 */
	@SuppressWarnings("unchecked")
	public V getFromCache(K key, ServletRequest request) {

		key = transformKeyBeforeProcessing(key);
		if (key == null) {
			return null;
		}
		if (isCacheable(key, request)) {
			Element elem;
			if ((elem = cache.get(prepareKey(key))) != null) {
				return (V) elem.getObjectValue();
			} else {
				request.setAttribute(isCacheableAttributeName, true);
				return null;
			}
		} else {
			return null;
		}

	}

	protected K transformKeyBeforeProcessing(K key) {
		return key;
	}

	/**
	 * Caches value
	 *
	 * @param key
	 * @param value
	 * @return true if was cached, false otherwise
	 */
	public boolean putInCache(K key, V value, ServletRequest request) {
		key = transformKeyBeforeProcessing(key);
		if (key == null) {
			return false;
		}
		if (isCacheable(key, request)) {
			cache.put(new Element(prepareKey(key), value));
			return true;
		} else {
			return false;
		}
	}

	protected K prepareKey(K k) {
		return k;
	}

	/**
	 * Call on application shutdown.
	 */
	public void closeCache() {
		cache.dispose();
		manager.shutdown();
	}
}
