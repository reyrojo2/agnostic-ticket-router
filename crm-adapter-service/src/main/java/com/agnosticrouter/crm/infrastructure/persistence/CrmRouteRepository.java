package com.agnosticrouter.crm.infrastructure.persistence;

import com.agnosticrouter.crm.application.port.CrmRouteStore;

import com.agnosticrouter.crm.domain.model.CrmRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmRouteRepository extends JpaRepository<CrmRoute, Long>, CrmRouteStore {
    List<CrmRoute> findByProfileCodeAndActiveTrueOrderByPriorityAsc(String profileCode);
}
