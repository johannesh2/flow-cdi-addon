package com.wcs.vaadin.flow.cdi.itest.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.wcs.vaadin.flow.cdi.UIScoped;
import com.wcs.vaadin.flow.cdi.VaadinServiceScoped;
import com.wcs.vaadin.flow.cdi.VaadinSessionScoped;
import org.apache.deltaspike.core.util.ContextUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import java.lang.annotation.Annotation;

public class PushComponent extends Div {
    public static final String RUN_BACKGROUND = "RUN_BACKGROUND";
    public static final String RUN_FOREGROUND = "RUN_FOREGROUND";

    @Resource
    private ManagedThreadFactory threadFactory;

    public class ContextCheckTask implements Runnable {
        private final UI ui;

        public ContextCheckTask(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {
            try {
                // Needed to make sure that this is sent as a push message
                Thread.sleep(500);
            } catch (InterruptedException e1) {
            }
            ui.access(PushComponent.this::print);
        }
    }

    private void print() {
        printContextIsActive(RequestScoped.class);
        printContextIsActive(SessionScoped.class);
        printContextIsActive(ApplicationScoped.class);
        printContextIsActive(UIScoped.class);
        printContextIsActive(VaadinServiceScoped.class);
        printContextIsActive(VaadinSessionScoped.class);
    }

    private void printContextIsActive(Class<? extends Annotation> scope) {
        Label label = new Label(ContextUtils.isContextActive(scope) + "");
        label.setId(scope.getName());
        add(new Div(new Label(scope.getSimpleName() + ": "), label));
    }


    @PostConstruct
    private void init() {
        NativeButton bgButton = new NativeButton("background", event -> {
            ContextCheckTask task = new ContextCheckTask(UI.getCurrent());
            Thread thread = threadFactory.newThread(task);
            thread.start();
        });
        bgButton.setId(RUN_BACKGROUND);

        NativeButton fgButton = new NativeButton("foreground", event
                -> print());
        fgButton.setId(RUN_FOREGROUND);

        add(bgButton, fgButton);
    }
}
