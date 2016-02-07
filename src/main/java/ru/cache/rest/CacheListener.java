/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.rest;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import ru.cache.core.RestCacheConfigurator;

/**
 *
 * @author Natal Kaplya
 *
 */
@WebListener
public class CacheListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        RestCacheConfigurator.initializeCache();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        RestCacheConfigurator.deinitializeCache();
    }
}
