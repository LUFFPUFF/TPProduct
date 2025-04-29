package com.example.domain.api.ans_api_module.template.services.answer.impl;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.ai_module.PredefinedAnswerRepository;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.mapper.PredefinedAnswerMapper;
import com.example.domain.api.ans_api_module.template.services.answer.PredefinedAnswerService;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredefinedAnswerServiceImpl implements PredefinedAnswerService {

    private final PredefinedAnswerRepository answerRepository;
    private final CompanyRepository companyRepository;
    private final PredefinedAnswerMapper answerMapper;

    @Override
    @Transactional
    public AnswerResponse createAnswer(PredefinedAnswerUploadDto dto) {
        Company company = companyRepository.findById(dto.getCompanyDto().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Company with id %d not found", dto.getCompanyDto().getId())));

        PredefinedAnswer predefinedAnswer = answerMapper.toEntity(dto);
        predefinedAnswer.setCompany(company);
        predefinedAnswer.setCreatedAt(LocalDateTime.now());

        PredefinedAnswer savedAnswer = answerRepository.save(predefinedAnswer);

        return buildResponseFromEntity(savedAnswer);

    }

    @Override
    @Transactional
    public AnswerResponse updateAnswer(Integer id, PredefinedAnswerUploadDto dto) {
        PredefinedAnswer existingAnswer = answerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Answer with id %d not found", id)));

        answerMapper.updateFromDto(dto, existingAnswer);

        PredefinedAnswer updatedAnswer = answerRepository.save(existingAnswer);

        return buildResponseFromEntity(updatedAnswer);
    }

    @Override
    @Transactional
    public void deleteAnswer(Integer id) {
        if (!answerRepository.existsById(id)) {
            throw new EntityNotFoundException(String.format("Answer with id %d not found", id));
        }

        answerRepository.deleteById(id);
    }

    @Override
    public AnswerResponse getAnswerById(Integer id) {
        return answerRepository.findById(id)
                .map(this::buildResponseFromEntity)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Answer with id %d not found", id)));
    }

    @Override
    public Page<AnswerResponse> searchAnswers(String searchTerm, Integer companyId, Pageable pageable) {
        return answerRepository.findAll(
                (Root<PredefinedAnswer> root, CriteriaQuery<?> _, CriteriaBuilder cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (companyId != null) {
                        predicates.add(cb.equal(root.get("company").get("id"), companyId));
                    }

                    if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                        String likePattern = STR."%\{searchTerm.toLowerCase()}%";
                        predicates.add(cb.or(
                                cb.like(cb.lower(root.get("title")), likePattern),
                                cb.like(cb.lower(root.get("answer")), likePattern),
                                cb.like(cb.lower(root.get("category")), likePattern)
                        ));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                },
                pageable
        ).map(this::buildResponseFromEntity);
    }

    @Override
    public List<AnswerResponse> getAnswersByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new IllegalArgumentException("Category cannot be empty");
        }

        return answerRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::buildResponseFromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public int deleteByCompanyIdAndCategory(Integer companyId, String category) {
        if (companyId == null || !StringUtils.hasText(category)) {
            throw new IllegalArgumentException("Company ID and category must be provided");
        }

        return answerRepository.deleteByCompanyIdAndCategory(companyId, category);
    }

    @Override
    public List<AnswerResponse> getAllAnswers() {
        return answerRepository.findAll().stream().map(this::buildResponseFromEntity).collect(Collectors.toList());
    }

    private AnswerResponse buildResponseFromEntity(PredefinedAnswer entity) {
        return new AnswerResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getAnswer(),
                entity.getCategory(),
                entity.getCompany().getName(),
                entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                true
        );
    }
}
