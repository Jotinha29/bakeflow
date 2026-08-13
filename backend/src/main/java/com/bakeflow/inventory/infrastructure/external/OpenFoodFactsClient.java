package com.bakeflow.inventory.infrastructure.external;
import com.bakeflow.integration.application.IntegrationService;import com.bakeflow.integration.application.IntegrationDtos.ResultStatus;import com.bakeflow.inventory.application.ProductInformationGateway;import com.bakeflow.inventory.application.InventoryDtos.*;import org.springframework.stereotype.Component;
@Component public class OpenFoodFactsClient implements ProductInformationGateway{
 private final IntegrationService integrations;OpenFoodFactsClient(IntegrationService integrations){this.integrations=integrations;}
 public ProductInformation findByBarcode(String barcode){var p=integrations.product(barcode,"legacy");return new ProductInformation(p.status()==ResultStatus.FOUND?LookupStatus.FOUND:p.status()==ResultStatus.NOT_FOUND?LookupStatus.NOT_FOUND:LookupStatus.UNAVAILABLE,p.barcode(),p.name(),p.brand(),p.imageUrl(),p.quantity(),p.categories(),p.errorCode());}
}
