/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * @author Natal Kaplya
 */
@ApplicationPath("cache") // set the path to REST web services
public class DefaultWSInitializer extends ResourceConfig {
	private static boolean isActive = false;

	public DefaultWSInitializer() {
		if (isActive) {
			Resource.Builder resourceBuilder = Resource.builder(CacheRestFacade.class).path("");
			final Resource resource = resourceBuilder.build();
			registerResources(resource);
		}
	}

	public static void setCacheRestActive(boolean active) {
		isActive = active;
	}

	// @Override
	// public Set<Class<?>> getClasses() {
	// if (isActive) {
	// final Set<Class<?>> classes = new HashSet<Class<?>>();
	// // register root resource
	// classes.add(CacheRestFacade.class);
	// return classes;
	// } else
	// return Collections.emptySet();
	// }
}
