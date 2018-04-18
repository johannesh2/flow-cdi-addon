package com.wcs.vaadin.flow.cdi.itest.service;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.NavigationEvent;
import com.wcs.vaadin.flow.cdi.VaadinServiceEnabled;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

@Priority(Interceptor.Priority.APPLICATION)
@Decorator
public abstract class InstantiatorDecorator implements Instantiator {
    @Inject
    @Delegate
    @VaadinServiceEnabled
    Instantiator delegate;

    @Override
    public <T> T getOrCreate(Class<T> type) {
        T instance = delegate.getOrCreate(type);
        if (InstantiatorDecoratorView.class.equals(type)) {
            ((InstantiatorDecoratorView) instance).decorate();
        }
        return instance;
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType, NavigationEvent event) {
        // Need to override it too to make it work on both Weld and OWB.
        return getOrCreate(routeTargetType);
    }
}
