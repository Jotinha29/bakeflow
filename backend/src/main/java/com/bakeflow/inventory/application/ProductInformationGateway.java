package com.bakeflow.inventory.application;
import com.bakeflow.inventory.application.InventoryDtos.ProductInformation;
public interface ProductInformationGateway { ProductInformation findByBarcode(String barcode); }
