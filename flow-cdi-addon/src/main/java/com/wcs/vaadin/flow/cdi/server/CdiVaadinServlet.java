package com.wcs.vaadin.flow.cdi.server;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.*;
import com.wcs.vaadin.flow.cdi.internal.CdiUI;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

/**
 * Servlet to create CdiVaadinServletService.
 * <p>
 * An instance of this servlet is automatically deployed by
 * {@link CdiServletDeployer} if no VaadinServlet is deployed based on web.xml or
 * Servlet 3.0 annotations. A subclass of this servlet and of
 * {@link CdiVaadinServletService} can be used and explicitly deployed
 * to customize it, in which case
 * {@link #createServletService(DeploymentConfiguration)} must call
 * service.init() .
 */

public class CdiVaadinServlet extends VaadinServlet {
    @Inject
    private BeanManager beanManager;

    private static final ThreadLocal<String> servletName = new ThreadLocal<>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        try {
            servletName.set(servletConfig.getServletName());
            super.init(servletConfig);
        } finally {
            servletName.set(null);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            servletName.set(getServletName());
            super.service(request, response);
        } finally {
            servletName.set(null);
        }
    }

    /**
     * Name of the Vaadin servlet for the current thread.
     * <p>
     * Until VaadinService appears in CurrentInstance,
     * it have to be used to get the servlet name.
     * <p>
     * This method is meant for internal use only.
     *
     * @see VaadinServlet#getCurrent()
     * @return currently processing vaadin servlet name
     */
    public static String getCurrentServletName() {
        return servletName.get();
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration configuration) throws ServiceException {
        final CdiVaadinServletService service =
                new CdiVaadinServletService(this, configuration, beanManager);
        service.init();
        return service;
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        return new CdiDeploymentConfiguration(getClass(), initParameters);
    }

    private static class CdiDeploymentConfiguration extends DefaultDeploymentConfiguration {
        public CdiDeploymentConfiguration(Class<?> systemPropertyBaseClass, Properties initParameters) {
            super(systemPropertyBaseClass, initParameters);
        }

        @Override
        public String getUIClassName() {
            return getStringProperty(VaadinSession.UI_PARAMETER,
                    CdiUI.class.getName());
        }
    }

}
