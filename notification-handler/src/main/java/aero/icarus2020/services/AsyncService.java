package aero.icarus2020.services;

import aero.icarus2020.models.AssetDao;
import aero.icarus2020.models.OrganizationDao;
import aero.icarus2020.models.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {

    @Value("${icarus.api.entry}")
    private String icarusAPI;

    @Autowired
    private RestTemplate restTemplate;

    @Async("asyncExecutor")
    public CompletableFuture<UserDao> getUser(String userId) throws InterruptedException {
        String userURI = icarusAPI + userId;
        UserDao user = restTemplate.getForObject(userURI, UserDao.class);
        return CompletableFuture.completedFuture(user);
    }

    @Async("asyncExecutor")
    public CompletableFuture<AssetDao> getAsset(String assetId) throws InterruptedException {
        String assetURI = icarusAPI + assetId;
        AssetDao asset = restTemplate.getForObject(assetURI, AssetDao.class);
        return CompletableFuture.completedFuture(asset);
    }

    @Async("asyncExecutor")
    public CompletableFuture<UserDao[]> getOrganizationUsers(String orgId) throws InterruptedException {
        String usersURI = icarusAPI + orgId;
        UserDao[] users = restTemplate.getForObject(usersURI, UserDao[].class);
        return CompletableFuture.completedFuture(users);
    }

    @Async("asyncExecutor")
    public CompletableFuture<OrganizationDao> getOrganization(String orgId) throws InterruptedException {
        String orgURI = icarusAPI + orgId;
        OrganizationDao org = restTemplate.getForObject(orgURI, OrganizationDao.class);
        return CompletableFuture.completedFuture(org);
    }

    @Async("asyncExecutor")
    public CompletableFuture<OrganizationDao[]> getAllOrganizations() throws InterruptedException {
        String orgURI = icarusAPI;
        OrganizationDao[] orgs = restTemplate.getForObject(orgURI, OrganizationDao[].class);
        return CompletableFuture.completedFuture(orgs);
    }

    @Async("asyncExecutor")
    public CompletableFuture<Long[]> getAllAuthorisecOrganizations(String assetId) throws InterruptedException {
        String orgURI = icarusAPI + assetId;
        Long[] orgs = restTemplate.getForObject(orgURI, Long[].class);
        return CompletableFuture.completedFuture(orgs);
    }

    public String getIcarusAPI() {
        return icarusAPI;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
