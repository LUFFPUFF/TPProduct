package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.task.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {
    @Query("SELECT t FROM Task t WHERE t.deal.id = :dealId")
    Task findTaskByDealId(@Param("dealId") Integer dealId);
}
