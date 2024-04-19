package com.example.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Cacheable
public class CustomerEntity extends PanacheEntity {

    @Column(length = 40, unique = true)
    public String name;

    @Column
    public String address;

    public CustomerEntity() {
    }

    public CustomerEntity(String name) {
        this.name = name;
    }
}