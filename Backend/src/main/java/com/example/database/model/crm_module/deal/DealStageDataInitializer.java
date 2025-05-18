package com.example.database.model.crm_module.deal;

import com.example.database.repository.crm_module.DealStageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DealStageDataInitializer {
    private final DealStageRepository dealStageRepository;

    @PostConstruct
    public void init() {
        List<DealStage> predefined = List.of(
                new DealStage(0, "Новая", "0", 0),
                new DealStage(1, "Пауза", "1", 1),
                new DealStage(2, "В работе", "2", 2),
                new DealStage(3, "Завершена", "3", 3),
                new DealStage(4, "Провалена", "4", 4)

        );

        for (DealStage stage : predefined) {
            if (!dealStageRepository.existsById(stage.getId())) {
                dealStageRepository.save(stage);
            }
        }
    }
}
