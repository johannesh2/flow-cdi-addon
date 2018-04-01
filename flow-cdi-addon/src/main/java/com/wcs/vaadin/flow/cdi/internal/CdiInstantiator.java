package com.wcs.vaadin.flow.cdi.internal;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.wcs.vaadin.flow.cdi.VaadinServiceEnabled;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.wcs.vaadin.flow.cdi.internal.BeanLookup.SERVICE;

/**
 * Default CDI instantiator.
 * <p>
 * Can be overridden by a @{@link VaadinServiceEnabled}
 * {@link com.vaadin.flow.di.Instantiator} CDI bean.
 */
public class CdiInstantiator extends DefaultInstantiator {

    private static final String CANNOT_USE_CDI_BEANS_FOR_I18_N
            = "Cannot use CDI beans for I18N, falling back to the default behavior.";
    private static final String FALLING_BACK_TO_DEFAULT_INSTANTIATION
            = "Falling back to default instantiation.";

    private AtomicBoolean i18NLoggingEnabled = new AtomicBoolean(true);
    private final BeanManager beanManager;

    public CdiInstantiator(VaadinService service, BeanManager beanManager) {
        super(service);
        this.beanManager = beanManager;
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        return new BeanLookup<>(beanManager, type).single()
                .ifUnsatisfied(() ->
                        getLogger().debug("'{}' is not a CDI bean. "
                                + FALLING_BACK_TO_DEFAULT_INSTANTIATION, type.getName()))
                .ifAmbiguous(e ->
                        getLogger().debug("Multiple CDI beans found. "
                                + FALLING_BACK_TO_DEFAULT_INSTANTIATION, e))
                .fallbackTo(() -> {
                    final T instance = super.getOrCreate(type);
                    BeanProvider.injectFields(instance);
                    return instance;
                })
                .get();
    }

    @Override
    public I18NProvider getI18NProvider() {
        final BeanLookup<I18NProvider>.Single lookup =
                new BeanLookup<>(beanManager, I18NProvider.class, SERVICE)
                        .single();
        if (i18NLoggingEnabled.compareAndSet(true, false)) {
            lookup
                    .ifUnsatisfied(() ->
                            getLogger().info("Can't find any bean implementing '{}'. "
                                            + CANNOT_USE_CDI_BEANS_FOR_I18_N,
                                    I18NProvider.class.getSimpleName()))
                    .ifAmbiguous(e ->
                            getLogger().warn("Found more beans for I18N. "
                                    + CANNOT_USE_CDI_BEANS_FOR_I18_N, e));
        }
        return lookup
                .fallbackTo(super::getI18NProvider)
                .get();
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
