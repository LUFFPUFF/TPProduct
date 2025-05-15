package com.example.database.model.company_subscription_module.company;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "company_whatsapp_configurations")
@Data
@ToString(exclude = {"accessToken"}, callSuper = false)
public class CompanyWhatsappConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "phone_number_id", nullable = false, unique = true)
    private Long phoneNumberId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "verify_token")
    private String verifyToken;
}
