package com.example.entity;

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

import com.example.repository.Customer;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.panache.common.Sort;

@Path("entity/customers")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class CustomerEntityResource {

    private static final Logger LOGGER = Logger.getLogger(CustomerEntityResource.class.getName());

    @GET
    public List<CustomerEntity> get() {
        return CustomerEntity.listAll(Sort.by("name"));
    }

    @GET
    @Path("{id}")
    public CustomerEntity getSingle(Long id) {
        CustomerEntity entity = CustomerEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Customer with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Transactional
    public Response create(CustomerEntity customer) {
        if (customer.id != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        customer.persist();
        return Response.ok(customer).status(201).build();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public CustomerEntity update(Long id, Customer customer) {
        if (customer.name == null) {
            throw new WebApplicationException("Customer Name was not set on request.", 422);
        }

        CustomerEntity entity = CustomerEntity.findById(id);

        if (entity == null) {
            throw new WebApplicationException("Customer with id of " + id + " does not exist.", 404);
        }

        entity.name = customer.name;
        entity.address = customer.address;

        return entity;
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(Long id) {
        CustomerEntity entity = CustomerEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Customer with id of " + id + " does not exist.", 404);
        }
        entity.delete();
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