/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.core;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 *
 * @author Natal Kaplya
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
