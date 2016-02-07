/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 *
 * @author Natal Kaplya
 */
@ApplicationPath("cache") // set the path to REST web services
public class DefaultWSInitializer extends Application {
	private static boolean isActive = false;

	public static void setCacheRestActive(boolean active) {
		isActive = active;
	}

	@Override
	public Set<Class<?>> getClasses() {
		if (isActive) {
			final Set<Class<?>> classes = new HashSet<Class<?>>();
			// register root resource
			classes.add(CacheRestFacade.class);
			return classes;
		} else
			return Collections.emptySet();
	}
}
