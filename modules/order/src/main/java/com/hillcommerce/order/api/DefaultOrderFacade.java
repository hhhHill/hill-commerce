package com.hillcommerce.order.api;

public class DefaultOrderFacade implements OrderFacade {

    @Override
    public String moduleName() {
        return "order";
    }
}
