package com.agnosticrouter.crm.repo;

import com.agnosticrouter.crm.domain.CrmRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmRouteRepository extends JpaRepository<CrmRoute, Long> {
    List<CrmRoute> findByProfileCodeAndActiveTrueOrderByPriorityAsc(String profileCode);
}
