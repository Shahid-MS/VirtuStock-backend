package com.virtu_stock.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.virtu_stock.Models.Registrar;

public interface RegistrarRepository
        extends JpaRepository<Registrar, Long> {

    Optional<Registrar> findBySlug(String slug);

}