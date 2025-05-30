package com.example.database.model.crm_module.deal;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "deal_stages", indexes = {
        @Index(name = "idx_deal_stages_order_index", columnList = "order_index")
})
@Data

public class DealStage {

    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "order_index")
    private Integer orderIndex;

    protected DealStage(Integer i, String name, String description, Integer orderIndex) {
        this.id = i;
        this.name = name;
        this.description = description;
        this.orderIndex = orderIndex;
    }

    public DealStage() {

    }
}
