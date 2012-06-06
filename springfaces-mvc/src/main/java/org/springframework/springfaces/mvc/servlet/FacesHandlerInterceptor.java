/*
 * Copyright 2010-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.springfaces.mvc.servlet;

import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.springfaces.mvc.context.SpringFacesContext;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * MVC {@link HandlerInterceptor} to setup and release a {@link SpringFacesContext} instance.
 * 
 * @see FacesPostbackHandler
 * @see Postback
 * 
 * @author Phillip Webb
 */
public class FacesHandlerInterceptor extends HandlerInterceptorAdapter implements ServletContextAware {

	private LifecycleAccess lifecycleAccess = createLifecycleAccess();

	private ServletContext servletContext;

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		this.lifecycleAccess.setServletContext(servletContext);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (handler instanceof Postback) {
			handler = ((Postback) handler).getHandler();
		}
		WebApplicationContext webApplicationContext = RequestContextUtils.getWebApplicationContext(request,
				this.servletContext);
		initializeSpringFacesContext(this.lifecycleAccess, webApplicationContext, request, response, handler);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		DefaultSpringFacesContext context = getSpringFacesContext(false);
		if (context != null) {
			context.release();
		}
	}

	/**
	 * Called to setup the {@link SpringFacesContext#getCurrentInstance() SpringFacesContext instance}. Subclasses can
	 * override this method to provide an alternative context.
	 * 
	 * @param lifecycleAccess access to the lifecycle
	 * @param webApplicationContext the Spring web application context
	 * @param request the request
	 * @param response the response
	 * @param handler the handler
	 */
	protected void initializeSpringFacesContext(LifecycleAccess lifecycleAccess,
			WebApplicationContext webApplicationContext, HttpServletRequest request, HttpServletResponse response,
			Object handler) {
		new DefaultSpringFacesContext(this.lifecycleAccess, webApplicationContext, request, response, handler);
	}

	/**
	 * Factory method used to create the {@link LifecycleAccess}.
	 * @return the {@link LifecycleAccess}.
	 */
	protected LifecycleAccess createLifecycleAccess() {
		return new LifecycleAccess();
	}

	/**
	 * Returns the {@link DefaultSpringFacesContext}.
	 * @param required if the context is required
	 * @return the {@link DefaultSpringFacesContext}
	 */
	private DefaultSpringFacesContext getSpringFacesContext(boolean required) {
		SpringFacesContext springFacesContext = SpringFacesContext.getCurrentInstance(required);
		if (springFacesContext == null) {
			return null;
		}
		Assert.isInstanceOf(DefaultSpringFacesContext.class, springFacesContext, "Unable to access SpringFacesContext ");
		return (DefaultSpringFacesContext) springFacesContext;
	}

	/**
	 * Set the lifecycle identifier to use when {@link LifecycleFactory#getLifecycle(String) creating} the JSF
	 * {@link Lifecycle}. When not specified the <tt>javax.faces.LIFECYCLE_ID</tt> initiation parameter of the
	 * {@link DispatcherServlet} will be used. If no explicit initialization parameter is set the
	 * {@link LifecycleFactory#DEFAULT_LIFECYCLE default} lifecycle identifier will be used.
	 * @param lifecycleId The lifecycle id or <tt>null</tt>
	 */
	public void setLifecycleId(String lifecycleId) {
		this.lifecycleAccess.setLifecycleId(lifecycleId);
	}
}
