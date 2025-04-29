package com.example.database.repository.ai_module;

import com.example.database.model.ai_module.PredefinedAnswer;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredefinedAnswerRepository extends JpaRepository<PredefinedAnswer, Integer>, JpaSpecificationExecutor<PredefinedAnswer> {

    @Transactional
    @Modifying
    @Query("DELETE FROM PredefinedAnswer a WHERE a.company.id = :companyId AND a.category = :category")
    int deleteByCompanyIdAndCategory(Integer companyId, String category);

    List<PredefinedAnswer> findByCategoryIgnoreCase(String category);

    @Query("SELECT pa FROM PredefinedAnswer pa JOIN FETCH pa.company WHERE (:companyId IS NULL OR pa.company.id = :companyId)")
    List<PredefinedAnswer> findByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT pa FROM PredefinedAnswer pa JOIN FETCH pa.company WHERE (:category IS NULL OR pa.category = :category)")
    List<PredefinedAnswer> findByCategory(@Param("category") String category);

    @Query("SELECT pa FROM PredefinedAnswer pa JOIN FETCH pa.company WHERE (:companyId IS NULL OR pa.company.id = :companyId) AND (:category IS NULL OR pa.category = :category)")
    List<PredefinedAnswer> findByCompanyIdAndCategory(@Param("companyId") Integer companyId, @Param("category") String category);

    @Override
    @Query("SELECT pa FROM PredefinedAnswer pa JOIN FETCH pa.company")
    @NotNull
    List<PredefinedAnswer> findAll();

}
