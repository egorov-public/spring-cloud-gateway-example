# Introduction
An initial services were borrowed from  "Spring Microservices in Action".
I've made several cosmetic modification in services and replaced zuul gateway with spring cloud gateway.

Services:

1.  A Spring Cloud Config server that is deployed as Docker container and can manage a services configuration information using a file system or GitHub-based repository.
2.  A Eureka server running as a Spring-Cloud based service.  This service will allow multiple service instances to register with it.  Clients that need to call a service will use Eureka to lookup the physical location of the target service.
3.  A Spring Cloud API Gateway.  All of our microservices can be routed through the gateway and have pre, response and
post policies enforced on the calls.
4.  A organization service that will manage organization data used within EagleEye.
5.  A new version of the organization service.  This service is used to demonstrate how to use the Zuul API gateway to route to different versions of a service.
6.  A special routes services that the the API gateway will call to determine whether or not it should route a user's service call to a different service then the one they were originally calling.  This service is used in conjunction with the orgservice-new service to determine whether a call to the organization service gets routed to an old version of the organization service vs. a new version of the service.
7.  A licensing service that will manage licensing data used within EagleEye.
8.  A Postgres SQL database used to hold the data for these two services.

# Building the Docker Images
To build the code examples run the following command at /  

   **mvn clean package docker:build**

# Running the services 

   **docker-compose -f docker/common/docker-compose.yml up**

If everything starts correctly you should see a bunch of Spring Boot information fly by on standard out.  At this point all of the services needed for the chapter code examples will be running.

I also suggest to check if all services up and running with 

   **docker ps
