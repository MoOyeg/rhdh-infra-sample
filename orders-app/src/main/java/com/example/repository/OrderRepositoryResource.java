package com.example.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.panache.common.Sort;

@Path("repository/orders")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class OrderRepositoryResource {

    @Inject
    OrderRepository orderRepository;

    private static final Logger LOGGER = Logger.getLogger(OrderRepositoryResource.class.getName());

    @GET
    public List<Order> get() {
        return orderRepository.listAll(Sort.by("name"));
    }

    @GET
    @Path("{id}")
    public Order getSingle(Long id) {
        Order entity = orderRepository.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Order with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Transactional
    public Response create(Order order) {
        if (order.id != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        orderRepository.persist(order);
        return Response.ok(order).status(201).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public Order update(Long id, Order order) {
        if (order.name == null) {
            throw new WebApplicationException("Order Name was not set on request.", 422);
        }

        Order entity = orderRepository.findById(id);

        if (entity == null) {
            throw new WebApplicationException("Order with id of " + id + " does not exist.", 404);
        }

        entity.name = order.name;
        entity.address = order.address;

        return entity;
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(Long id) {
        Order entity = orderRepository.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Order with id of " + id + " does not exist.", 404);
        }
        orderRepository.delete(entity);
        return Response.status(204).build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception exception) {
            LOGGER.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", exception.getClass().getName());
            exceptionJson.put("code", code);

            if (exception.getMessage() != null) {
                exceptionJson.put("error", exception.getMessage());
            }

            return Response.status(code)
                    .entity(exceptionJson)
                    .build();
        }

    }
}
