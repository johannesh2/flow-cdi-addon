package com.wcs.vaadin.flow.cdi.internal;

import com.vaadin.flow.component.Component;
import com.wcs.vaadin.flow.cdi.NormalRouteScoped;
import com.wcs.vaadin.flow.cdi.NormalUIScoped;
import org.apache.deltaspike.core.util.context.AbstractContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

/**
 * CDI Extension needed to register Vaadin scopes to the runtime.
 */
public class VaadinExtension implements Extension {

    private UIScopedContext uiScopedContext;
    private RouteScopedContext routeScopedContext;
    private List<String> normalScopedComponentWarnings = new LinkedList<String>();

    public void initializeContexts(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
        uiScopedContext.init(beanManager);
        routeScopedContext.init(beanManager);
    }

    void processManagedBean(@Observes ProcessManagedBean pmb,
                            final BeanManager beanManager) {
        Bean<?> bean = pmb.getBean();
        Class<?> beanClass = bean.getBeanClass();
        Class<? extends Annotation> beanScope = bean.getScope();

        if (Component.class.isAssignableFrom(beanClass)
                && beanManager.isNormalScope(beanScope)) {
            normalScopedComponentWarnings.add("@"
                    + String.format("%-20s", beanScope.getSimpleName()) + " "
                    + beanClass.getCanonicalName());
        }
    }

    void afterBeanDiscovery(
            @Observes final AfterBeanDiscovery afterBeanDiscovery,
            final BeanManager beanManager) {

        if (normalScopedComponentWarnings.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following Vaadin components are injected into "
                    + "normal scoped contexts:\n");
            for (String proxiedComponent : normalScopedComponentWarnings) {
                sb.append("   ");
                sb.append(proxiedComponent);
                sb.append("\n");
            }
            sb.append("This approach uses proxy objects and is "
                    + "not supported in Vaadin framework.");
            getLogger().error(sb.toString());
        }

        addContext(afterBeanDiscovery, new VaadinServiceScopedContext(beanManager));
        addContext(afterBeanDiscovery, new VaadinSessionScopedContext(beanManager));
        uiScopedContext = new UIScopedContext(beanManager);
        addContext(afterBeanDiscovery, uiScopedContext, NormalUIScoped.class);
        routeScopedContext = new RouteScopedContext(beanManager);
        addContext(afterBeanDiscovery, routeScopedContext, NormalRouteScoped.class);
    }

    private void addContext(AfterBeanDiscovery afterBeanDiscovery,
                            AbstractContext context,
                            Class<? extends Annotation>... additionalScope) {
        Class<? extends Annotation> scope = context.getScope();
        int i = 0;
        do {
            afterBeanDiscovery.addContext(new ContextWrapper(context, scope));
            scope = additionalScope.length > i ? additionalScope[i++] : null;
        } while (scope != null);
        getLogger().info("{} registered for Vaadin CDI", context.getClass().getSimpleName());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinExtension.class.getCanonicalName());
    }
}
