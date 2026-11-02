package com.pruebatecnica.polizas.repository;

import com.pruebatecnica.polizas.domain.Riesgo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    List<Riesgo> findByPolizaIdOrderById(Long polizaId);

    @Query("select r.poliza.id from Riesgo r where r.id = :id")
    Optional<Long> buscarPolizaId(@Param("id") Long id);
}
