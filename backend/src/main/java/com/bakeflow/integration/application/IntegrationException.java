package com.bakeflow.integration.application;
public class IntegrationException extends RuntimeException { private final String code; public IntegrationException(String code){super(code);this.code=code;} public String code(){return code;} }
