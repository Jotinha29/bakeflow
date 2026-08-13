package com.bakeflow.integration.application;

import java.util.List;

public final class IntegrationDtos {
    private IntegrationDtos() {}
    public enum ResultStatus { FOUND, NOT_FOUND, UNAVAILABLE }
    public enum DataSource { OPEN_FOOD_FACTS, BRASIL_API }
    public record ExternalProductResult(ResultStatus status, String barcode, String name, String brand,
        String quantity, String imageUrl, List<String> categories, DataSource source, boolean fresh, String errorCode) {}
    public record ExternalCompanyResult(ResultStatus status, String taxId, String legalName, String tradeName,
        String companyStatus, String street, String number, String district, String city, String state,
        String postalCode, DataSource source, boolean fresh, String errorCode) {}
    public record ProviderStatus(boolean configured, String circuitState) {}
    public record IntegrationsStatus(ProviderStatus openFoodFacts, ProviderStatus brasilApi, boolean redisAvailable) {}
}
