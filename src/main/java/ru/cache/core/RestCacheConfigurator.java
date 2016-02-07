/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import ru.cache.rest.DefaultWSInitializer;

/**
 * Configurator for cache. If none is specified, basic implementation of
 * RestCache is used.
 *
 * @author Natal Kaplya
 */
public class RestCacheConfigurator {

	private static volatile RestCache restCache;
	private static volatile boolean isInitialized = false;
	private static volatile boolean isActive = false;
	private static volatile boolean isBroken = false;
	public static final String enabledParamConfigName = "enabled";
	public static final String denyParamConfigName = "denyParams";
	public static final String excludeParamConfigName = "excludeParams";
	public static final String basicPathParamConfigName = "basicPath";
	public static final String cachingUrlParamConfigName = "cachingUrls";
	public static final String restParamConfigName = "restParam";
	public static final String restPathParamConfigName = "restPath";
	public static final String jerseyEnabledParamName = "jerseyEnabled";
	private static final Logger LOGGER = Logger.getLogger(RestCacheConfigurator.class.getName());
	private static final String propsPath = "/cacheopts.properties";

	private RestCacheConfigurator() {
	}

	public synchronized static void initializeCache() {
		initializeCache(null, null);
	}

	synchronized static void initializeCache(RestCache rc, Properties props) {
		if (!isInitialized && !isBroken()) {
			InputStream propIS = null;
			try {
				LOGGER.log(Level.INFO, "RestCache initializing...");
				if (rc == null) {
					LOGGER.log(Level.INFO, "RestCache cache implementation is default");
					restCache = new RestCache();
				} else {
					LOGGER.log(Level.INFO, "RestCache cache implementation is overriden");
				}
				Properties properties;
				if (props == null) {
					properties = new Properties();
					propIS = RestCacheConfigurator.class.getResourceAsStream(propsPath);
					properties.load(propIS);
				} else
					properties = props;

				String enabled = properties.getProperty(enabledParamConfigName);
				if (enabled != null && enabled.toLowerCase().trim().equals("true")) {
					String basicPath = properties.getProperty(basicPathParamConfigName);
					try {
						if (basicPath != null) {
							restCache.setBasicPath(basicPath);
						} else {
							String exc = "RestCache " + basicPathParamConfigName
									+ " property not specified in properties. \nRestCache not started.";
							throw new CacheInitializationException(exc);
						}
						LOGGER.log(Level.INFO, String.format("RestCache enabled with basic path: %s", basicPath));
					} catch (PatternSyntaxException e) {
						String exc = String.format("RestCache not enabled. Wrong basicPath property: %s", basicPath);
						throw new CacheInitializationException(exc, e);
					}
				} else {
					throw new CacheInitializationException(String.format(
							"RestCache is disabled. Param '%s' not found or is not 'true'", enabledParamConfigName));
				}
				String jerseyEnabled = properties.getProperty(jerseyEnabledParamName);
				if (jerseyEnabled != null && jerseyEnabled.toLowerCase().equals("true")) {
					DefaultWSInitializer.setCacheRestActive(true);
					LOGGER.log(Level.INFO, "RestCache " + jerseyEnabledParamName + " initialized:" + jerseyEnabled);
				} else {
					DefaultWSInitializer.setCacheRestActive(false);
					LOGGER.log(Level.INFO, "RestCache " + jerseyEnabledParamName + " is not initialized");
				}

				String deny = properties.getProperty(denyParamConfigName);
				if (deny != null && !deny.isEmpty()) {
					String[] params = deny.replace(" ", "").split(",");
					restCache.addToQueryParamsDeny(deny.replace("\n", "").replace(" ", "").split(","));
					LOGGER.log(Level.INFO,
							"RestCache " + denyParamConfigName + " initialized:" + Arrays.toString(params));
				}
				String excludeParams = properties.getProperty(excludeParamConfigName);
				if (excludeParams != null && !excludeParams.isEmpty()) {
					String[] params = excludeParams.replace(" ", "").split(",");
					restCache.addToQueryParamsExclude(excludeParams.replace("\n", "").replace(" ", "").split(","));
					LOGGER.log(Level.INFO,
							"RestCache " + excludeParamConfigName + " initialized:" + Arrays.toString(params));
				}
				String urls = properties.getProperty(cachingUrlParamConfigName);
				if (urls != null && !urls.isEmpty()) {
					String[] params = urls.replace("\n", "").split(",");
					restCache.addToCachingUrls(params);
					LOGGER.log(Level.INFO,
							"RestCache " + cachingUrlParamConfigName + " initialized:" + Arrays.toString(params));
				}
				boolean useRest = false;
				String restParam = properties.getProperty(restParamConfigName);
				if (restParam != null) {
					String param = restParam.replace("\n", "").replace(" ", "");
					if (param.matches("[\\d\\w]+")) {
						restCache.setRestParam(param);
						useRest = true;
						LOGGER.log(Level.INFO, "RestCache " + restParamConfigName + " initialized:" + param);
					} else {
						throw new CacheInitializationException(
								restParamConfigName + "is not properly initialized([\\d\\w]+)");
					}
				}
				String restPath = properties.getProperty(restPathParamConfigName);
				if (restPath != null) {
					if (!useRest) {
						throw new CacheInitializationException(
								"RestCache restPath is initialized, but " + restPathParamConfigName + " is not!");
					}
					String param = restPath.replace("\n", "").replace(" ", "");
					restCache.setRestPath(param);
					LOGGER.log(Level.INFO, "RestCache " + restPathParamConfigName + " initialized:" + param);
				}
				RestCacheConfigurator.setIsActive(true);
			} catch (IOException ex) {
				Logger.getLogger(CacheListener.class.getName()).log(Level.SEVERE, "Cache props not found:" + propsPath,
						ex);
				RestCacheConfigurator.setIsActive(false);
				RestCacheConfigurator.setIsBroken(true);
			} catch (CacheInitializationException ex) {
				Logger.getLogger(CacheListener.class.getName()).log(Level.SEVERE, "Cache initialization failed!\n", ex);
				RestCacheConfigurator.setIsActive(false);
				RestCacheConfigurator.setIsBroken(true);
			} finally {
				if (propIS != null) {
					try {
						propIS.close();
					} catch (IOException ex) {
						Logger.getLogger(RestCacheConfigurator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}
	}

	public synchronized static boolean setCache(RestCache rc) {
		return setCache(rc, null);
	}

	public synchronized static boolean setCache(Properties props) {
		return setCache(null, props);
	}

	public synchronized static boolean setCache(RestCache rc, Properties props) {
		if (!isInitialized && !isBroken()) {
			restCache = rc;
			initializeCache(restCache, props);
			isInitialized = true;
			return true;
		} else {
			return false;
		}
	}

	public static RestCache getCache() {
		return restCache;
	}

	public static boolean isInitialized() {
		return isInitialized;
	}

	/**
	 * @return the isActive
	 */
	public static boolean isActive() {
		return isActive;
	}

	/**
	 * @param aIsActive
	 *            the isActive to set
	 */
	public static void setIsActive(boolean aIsActive) {
		if (!isBroken()) {
			isActive = aIsActive;
		}
	}

	/**
	 * @return the isBroken
	 */
	public static boolean isBroken() {
		return isBroken;
	}

	/**
	 * @param aIsBroken
	 *            the isBroken to set
	 */
	static void setIsBroken(boolean aIsBroken) {
		isBroken = aIsBroken;
	}

	public static void deinitializeCache() {
		try {
			setIsBroken(false);
			setIsActive(false);
			if (restCache != null) {
				restCache.closeCache();
			}
			LOGGER.log(Level.INFO, "RestCache deinitialized");
		} catch (Exception e) {
			LOGGER.log(Level.INFO, "RestCache has not been  properly deinitialized", e);
		}
	}
}
