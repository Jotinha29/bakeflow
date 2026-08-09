package com.bakeflow.inventory.infrastructure.external;
import com.bakeflow.inventory.application.ProductInformationGateway;import com.bakeflow.inventory.application.InventoryDtos.*;import java.util.*;import org.springframework.stereotype.Component;import org.springframework.web.client.*;import tools.jackson.databind.JsonNode;
@Component public class OpenFoodFactsClient implements ProductInformationGateway{
 private final RestClient client;OpenFoodFactsClient(RestClient openFoodFactsRestClient){client=openFoodFactsRestClient;}
 public ProductInformation findByBarcode(String barcode){
  if(barcode==null||barcode.isBlank())return unavailable(barcode,"A barcode is required.");
  try{JsonNode root=client.get().uri("/api/v2/product/{barcode}.json?fields=code,product_name,brands,image_front_url,quantity,categories_tags",barcode).retrieve().body(JsonNode.class);if(root==null||root.path("status").asInt()==0)return new ProductInformation(LookupStatus.NOT_FOUND,barcode,null,null,null,null,List.of(),"No product was found for this barcode.");JsonNode p=root.path("product");List<String>categories=new ArrayList<>();p.path("categories_tags").forEach(n->categories.add(n.asText().replaceFirst("^[a-z]{2}:","").replace('-', ' ')));return new ProductInformation(LookupStatus.FOUND,barcode,text(p,"product_name"),text(p,"brands"),text(p,"image_front_url"),text(p,"quantity"),categories,null);
  }catch(RuntimeException e){return unavailable(barcode,"External product information is unavailable at the moment.");}
 }
 private String text(JsonNode n,String field){String value=n.path(field).asText();return value.isBlank()?null:value;}
 private ProductInformation unavailable(String barcode,String message){return new ProductInformation(LookupStatus.UNAVAILABLE,barcode,null,null,null,null,List.of(),message);}
}
