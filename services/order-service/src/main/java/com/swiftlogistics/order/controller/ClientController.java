// services/order-service/src/main/java/com/swiftlogistics/order/controller/ClientController.java
package com.swiftlogistics.order.controller;

import com.swiftlogistics.order.dto.ApiResponse;
import com.swiftlogistics.order.entity.Client;
import com.swiftlogistics.order.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private ClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<Client>> createClient(
            @RequestParam String companyName,
            @RequestParam String contactPerson,
            @RequestParam String email) {

        logger.info("Creating new client: {}", companyName);

        try {
            Client client = clientService.createClient(companyName, contactPerson, email);
            return ResponseEntity.ok(ApiResponse.success("Client created successfully", client));

        } catch (Exception e) {
            logger.error("Error creating client: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create client: " + e.getMessage()));
        }
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<Client>> getClient(@PathVariable String clientId) {
        logger.debug("Fetching client: {}", clientId);

        try {
            Client client = clientService.getClientById(clientId);
            return ResponseEntity.ok(ApiResponse.success(client));

        } catch (Exception e) {
            logger.error("Error fetching client {}: {}", clientId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Client>>> getAllActiveClients() {
        logger.debug("Fetching all active clients");

        try {
            List<Client> clients = clientService.getAllActiveClients();
            return ResponseEntity.ok(ApiResponse.success(clients));

        } catch (Exception e) {
            logger.error("Error fetching clients: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch clients: " + e.getMessage()));
        }
    }

    @PutMapping("/{clientId}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateClient(@PathVariable String clientId) {
        logger.info("Deactivating client: {}", clientId);

        try {
            clientService.deactivateClient(clientId);
            return ResponseEntity.ok(ApiResponse.success("Client deactivated successfully", null));

        } catch (Exception e) {
            logger.error("Error deactivating client: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to deactivate client: " + e.getMessage()));
        }
    }
}