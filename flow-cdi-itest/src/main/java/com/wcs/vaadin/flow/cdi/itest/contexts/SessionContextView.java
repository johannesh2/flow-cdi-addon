package com.wcs.vaadin.flow.cdi.itest.contexts;


import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.wcs.vaadin.flow.cdi.VaadinSessionScoped;
import com.wcs.vaadin.flow.cdi.itest.Counter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;


@BodySize(height = "100vh", width = "100vw")
@Route("")
public class SessionContextView extends Div {

    public static final String SETVALUEBTN_ID = "setvalbtn";
    public static final String VALUELABEL_ID = "label";
    public static final String VALUE = "session";
    public static final String INVALIDATEBTN_ID = "invalidatebtn";
    public static final String HTTP_INVALIDATEBTN_ID = "httpinvalidatebtn";
    public static final String EXPIREBTN_ID = "expirebtn";

    @Inject
    SessionScopedBean sessionScopedBean;

    @PostConstruct
    private void init() {
        NativeButton setBtn = new NativeButton("set");
        setBtn.addClickListener(event -> sessionScopedBean.setValue(VALUE));
        setBtn.setId(SETVALUEBTN_ID);
        add(setBtn);

        NativeButton invalidateBtn = new NativeButton("invalidate");
        invalidateBtn.addClickListener(event -> VaadinSession.getCurrent().close());
        invalidateBtn.setId(INVALIDATEBTN_ID);
        add(invalidateBtn);

        NativeButton httpInvalidateBtn = new NativeButton("httpinvalidate");
        httpInvalidateBtn.addClickListener(
                event -> VaadinSession.getCurrent().getSession().invalidate());
        httpInvalidateBtn.setId(HTTP_INVALIDATEBTN_ID);
        add(httpInvalidateBtn);

        NativeButton expireBtn = new NativeButton("httpexpire");
        expireBtn.addClickListener(
                event -> VaadinSession.getCurrent().getSession().setMaxInactiveInterval(1));
        expireBtn.setId(EXPIREBTN_ID);
        add(expireBtn);

        Label label = new Label();
        label.setText(sessionScopedBean.getValue()); // bean instantiated here
        label.setId(VALUELABEL_ID);
        add(label);
    }

    @VaadinSessionScoped
    //Like other vaadin scopes,
    // Serializable is mandatory only if you want a working session serialization
    public static class SessionScopedBean {
        public static final String DESTROY_COUNT = "SessionScopedBeanDestroy";

        @Inject
        Counter counter;

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @PreDestroy
        private void preDestroy() {
            counter.increment(DESTROY_COUNT);
        }
    }
}
