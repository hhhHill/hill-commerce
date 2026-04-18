package com.hillcommerce.common.core.id;

@FunctionalInterface
public interface IdGenerator {

    Long nextId();
}
