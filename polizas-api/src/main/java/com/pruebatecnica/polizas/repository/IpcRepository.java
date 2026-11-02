package com.pruebatecnica.polizas.repository;

import com.pruebatecnica.polizas.domain.IpcAnual;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IpcRepository extends JpaRepository<IpcAnual, Integer> {

    List<IpcAnual> findAllByOrderByAnioDesc();
}
