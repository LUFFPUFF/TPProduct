package com.example.database.model.company_subscription_module.company;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "company_vk_configurations")
@Data
@ToString(exclude = {"accessToken"})
public class CompanyVkConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "community_id", nullable = false, unique = true)
    private Long communityId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "community_name")
    private String communityName;

    public boolean isActive() {
        return accessToken != null && !accessToken.trim().isEmpty() && communityId != null;
    }
}
