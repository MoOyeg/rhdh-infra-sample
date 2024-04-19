package com.example.repository;


import jakarta.persistence.Cacheable;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


@Entity
@Cacheable
public class Customer {
    
    @Id
    @GeneratedValue
    public Long id;
    
    @Column(length = 40, unique = true)
    public String name;

    @Column
    public String address;

    public Customer() {
    }

    public Customer(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
