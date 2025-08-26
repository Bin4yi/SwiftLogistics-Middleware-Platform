package com.swiftlogistics.order.service;

import com.swiftlogistics.order.entity.Client;
import com.swiftlogistics.order.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClientService {

    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    @Autowired
    private ClientRepository clientRepository;

    public Client createClient(String companyName, String contactPerson, String email) {
        logger.info("Creating new client: {}", companyName);

        if (clientRepository.existsByEmail(email)) {
            throw new RuntimeException("Client with email already exists: " + email);
        }

        Client client = new Client(companyName, contactPerson, email);
        return clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public Client getClientById(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId));
    }

    @Transactional(readOnly = true)
    public List<Client> getAllActiveClients() {
        return clientRepository.findByActive(true);
    }

    public void validateClient(String clientId) {
        if (!clientRepository.existsByClientId(clientId)) {
            logger.warn("Validation failed - client not found: {}", clientId);
            throw new RuntimeException("Client not found: " + clientId);
        }
    }

    public Client updateClient(Client client) {
        logger.info("Updating client: {}", client.getClientId());
        return clientRepository.save(client);
    }

    public void deactivateClient(String clientId) {
        Client client = getClientById(clientId);
        client.setActive(false);
        clientRepository.save(client);
        logger.info("Client deactivated: {}", clientId);
    }
}