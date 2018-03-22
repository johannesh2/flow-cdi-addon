package com.vaadin.flow.cdi.itest.smoke;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import java.io.Serializable;

@Route("")
public class CdiView extends Div {
    @Inject
    private HelloProvider helloProvider;

    public CdiView() {
        NativeButton button = new NativeButton("Click me",
                event -> {
                    final Label hello = new Label(getLabelText());
                    hello.setId("HELLO");
                    add(hello);
                });
        button.setId("CLICK_ME");
        add(button);
    }

    private String getLabelText() {
        if (helloProvider != null) {
            return helloProvider.getHello();
        } else {
            return "no CDI";
        }
    }

    @PostConstruct
    private void init() {
        helloProvider.setHello("hello CDI");
    }

    @SessionScoped
    public static class HelloProvider implements Serializable {
        private String hello;

        public String getHello() {
            return hello;
        }

        public void setHello(String hello) {
            this.hello = hello;
        }
    }

}
