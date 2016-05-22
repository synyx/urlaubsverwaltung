package org.synyx.urlaubsverwaltung.config;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import org.synyx.urlaubsverwaltung.web.UserInterceptor;


/**
 * Configuration for WebMvc. Configuration is done by overriding base methods of {@link WebMvcConfigurerAdapter}.
 *
 * @author  David Schilling - schilling@synyx.de
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private UserInterceptor userInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(userInterceptor).addPathPatterns("/web/**");
    }
}
