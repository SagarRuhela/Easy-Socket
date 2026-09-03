package org.easySocket;

@FunctionalInterface
public interface ConnectionValidator {
    boolean validate(String roomName, String memberName);
}