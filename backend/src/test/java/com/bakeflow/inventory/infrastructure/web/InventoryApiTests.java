package com.bakeflow.inventory.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryApiTests {
    @Autowired MockMvc mvc;
    static String itemId;
    static String locationId;

    @Test @Order(1)
    void createsAndGetsFilteredItems() throws Exception {
        String body = "{\"name\":\"API Flour\",\"sku\":\"API-FAR-001\",\"type\":\"RAW_MATERIAL\",\"unit\":\"KG\",\"minimumStock\":5}";
        String response = mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        itemId = response.replaceAll(".*\"id\":\"([^\"]+).*", "$1");
        mvc.perform(get("/api/v1/items").param("search", "API Flour").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("API-FAR-001"));
    }

    @Test @Order(2)
    void rejectsDuplicateSkuAndTogglesItem() throws Exception {
        String duplicate = "{\"name\":\"Duplicate\",\"sku\":\"API-FAR-001\",\"type\":\"OTHER\",\"unit\":\"UNIT\"}";
        mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(duplicate))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("SKU")));
        mvc.perform(patch("/api/v1/items/{id}/deactivate", itemId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test @Order(3)
    void createsAndListsBatch() throws Exception {
        String body = "{\"itemId\":\"" + itemId + "\",\"code\":\"API-LOT-001\",\"manufacturingDate\":\"2026-01-01\",\"expirationDate\":\"2026-02-01\"}";
        mvc.perform(post("/api/v1/batches").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.itemName").value("API Flour"));
        mvc.perform(get("/api/v1/batches").param("itemId", itemId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("API-LOT-001"));
    }

    @Test @Order(4)
    void createsListsAndDeactivatesLocation() throws Exception {
        String body = "{\"name\":\"API Warehouse\",\"code\":\"API-WH\",\"type\":\"WAREHOUSE\"}";
        String response = mvc.perform(post("/api/v1/locations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        locationId = response.replaceAll(".*\"id\":\"([^\"]+).*", "$1");
        mvc.perform(get("/api/v1/locations/tree")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'API-WH')]", hasSize(1)));
        mvc.perform(patch("/api/v1/locations/{id}/deactivate", locationId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
