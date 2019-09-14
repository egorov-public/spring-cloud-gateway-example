package com.thoughtmechanix.zuulsvr.filters;

import java.net.URI;
import java.util.Optional;
import java.util.Random;

import com.thoughtmechanix.zuulsvr.model.AbTestingRoute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.factory.AbstractChangeRequestUriGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Component
public class AbFilter extends AbstractChangeRequestUriGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {

    @Autowired
    RestTemplate restTemplate;
    
    public AbFilter() {
        super(NameConfig.class);
    }

    private AbTestingRoute getAbRoutingInfo(String serviceName){
        ResponseEntity<AbTestingRoute> restExchange = null;
        try {
            restExchange = restTemplate.exchange(
                             "http://specialroutesservice/v1/route/abtesting/{serviceName}",
                             HttpMethod.GET,
                             null, AbTestingRoute.class, serviceName);
        }
        catch(HttpClientErrorException ex){
            if (ex.getStatusCode()== HttpStatus.NOT_FOUND) return null;
            throw ex;
        }
        return restExchange.getBody();
    }

    private String buildRouteString(String oldEndpoint, String newEndpoint, String serviceName){
        int index = oldEndpoint.indexOf(serviceName);
        String strippedRoute = oldEndpoint.substring(index + serviceName.length());
        System.out.println("Target route: " + String.format("%s/%s", newEndpoint, strippedRoute));
        return String.format("%s/%s", newEndpoint, strippedRoute);
    }

    public boolean useSpecialRoute(AbTestingRoute testRoute){
        Random random = new Random();

        if (testRoute.getActive().equals("N")) 
            return false;

        int value = random.nextInt((10 - 1) + 1) + 1;
        if (testRoute.getWeight()<value) 
            return true;

        return false;
    }
    
    @Override
    protected Optional<URI> determineRequestUri(ServerWebExchange exchange, NameConfig config) {
        
        URI requestURI = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String serviceId = requestURI.getHost();
        AbTestingRoute abTestRoute = getAbRoutingInfo(serviceId.toLowerCase());

        if (abTestRoute!=null && useSpecialRoute(abTestRoute)) {
            String route = buildRouteString(requestURI.toString(),
                    abTestRoute.getEndpoint(), serviceId);
            return Optional.ofNullable(route).map(url -> URI.create(url));
       }

        return Optional.empty();
    }
}