package aero.icarus2020.services;

import aero.icarus2020.models.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class AsyncService {

    @Value("${icarus.api.entry}")
    private String icarusAPI;

    @Value("${icarus.api.token}")
    private String icarusAPIToken;

    @Autowired
    private RestTemplate restTemplate;

    @Async("asyncExecutor")
    public CompletableFuture<Boolean> checkToken(String token) {

        try {
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add("Cookie", "auth_token=" + token);
            HttpEntity requestEntity = new HttpEntity(null, requestHeaders);
            ResponseEntity<String> response = restTemplate.exchange(icarusAPIToken, HttpMethod.GET, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (Exception eek) {
        }
        return CompletableFuture.completedFuture(false);
    }

    @Async("asyncExecutor")
    public CompletableFuture<Boolean> isAssetValid(String assetId) {
        String assetURI = icarusAPI + assetId;
        try {
            String asset = restTemplate.getForObject(assetURI, String.class);
            CompletableFuture<String> assetAsync = CompletableFuture.completedFuture(asset);
            assetAsync.get();
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("asyncExecutor")
    public CompletableFuture<Boolean> isAuthorisedOrganization(String assetId, Long organizationId) {
        String orgURI = icarusAPI + assetId;
        Long[] orgs = restTemplate.getForObject(orgURI, Long[].class);
        CompletableFuture<Long[]> ayncOrgs = CompletableFuture.completedFuture(orgs);
        try {
            orgs = ayncOrgs.get();
        } catch (InterruptedException | ExecutionException e) {
            return CompletableFuture.completedFuture(false);
        }

        for (Long org : orgs) {
            if (org.equals(organizationId))
                return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.completedFuture(false);

    }

    @Async("asyncExecutor")
    public CompletableFuture<Long> getOrganizationId(String userId) {
        String userURI = icarusAPI + userId;
        UserDao user = restTemplate.getForObject(userURI, UserDao.class);
        CompletableFuture<UserDao> asyncUser = CompletableFuture.completedFuture(user);

        try {
            user = asyncUser.get();
        } catch (InterruptedException | ExecutionException e) {
            return CompletableFuture.completedFuture((long) -1);
        }

        return CompletableFuture.completedFuture(user.getOrganizationid());
    }

    public String getIcarusAPIToken() {
        return icarusAPIToken;
    }

    public String getIcarusAPI() {
        return icarusAPI;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
