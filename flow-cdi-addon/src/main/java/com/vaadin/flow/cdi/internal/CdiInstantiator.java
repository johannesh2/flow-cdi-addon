package com.vaadin.flow.cdi.internal;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Stream;

public class CdiInstantiator extends DefaultInstantiator {

    private static final String FALLING_BACK_TO_DEFAULT_INSTANTIATION
            = "Falling back to default instantiation. ";
    private final BeanManager beanManager;

    public CdiInstantiator(VaadinService service, BeanManager beanManager) {
        super(service);
        this.beanManager = beanManager;
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        final Set<Bean<?>> beans = beanManager.getBeans(type);
        if (beans == null || beans.isEmpty()) {
            getLogger().warn(FALLING_BACK_TO_DEFAULT_INSTANTIATION +
                    "'{}' is not a CDI bean.", type.getName());
            return super.getOrCreate(type);
        }
        final Bean<?> bean;
        try {
            bean = beanManager.resolve(beans);
        } catch (AmbiguousResolutionException e) {
            getLogger().warn(FALLING_BACK_TO_DEFAULT_INSTANTIATION +
                    "Multiple CDI beans found. ", e);
            return super.getOrCreate(type);
        }
        final CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        //noinspection unchecked
        return  (T) beanManager.getReference(bean, type, ctx);
    }

    @Override
    public I18NProvider getI18NProvider() {
        try {
            return BeanProvider.getContextualReference(beanManager, I18NProvider.class, false);
        } catch (AmbiguousResolutionException  e) {
            logI18NFallback("Found more beans implementing '{}'.");
        } catch (IllegalStateException e) {
            logI18NFallback("Can't find any bean implementing '{}'.");
        }
        return super.getI18NProvider();
    }

    private void logI18NFallback(String s) {
        getLogger().info(
                s + " Cannot use CDI beans for I18N, falling back to the default behavior",
                I18NProvider.class.getSimpleName());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(CdiInstantiator.class.getName());
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        final ServiceInitBroadcaster broadcaster = BeanProvider
                .getDependent(beanManager, ServiceInitBroadcaster.class)
                .get();
        return Stream.concat(
                super.getServiceInitListeners(),
                Stream.of(broadcaster));
    }

    public static class ServiceInitBroadcaster
            implements VaadinServiceInitListener {
        @Inject
        private Event<ServiceInitEvent> eventTrigger;

        @Override
        public void serviceInit(ServiceInitEvent event) {
            eventTrigger.fire(event);
        }
    }

}
