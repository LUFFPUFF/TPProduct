package com.example.domain.api.subscription_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.SubscriptionRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.api.subscription_module.exception_handler_subscription.AlreadyInCompanyException;
import com.example.domain.api.subscription_module.exception_handler_subscription.MaxOperatorsCountException;
import com.example.domain.api.subscription_module.exception_handler_subscription.NotFoundSubscriptionException;
import com.example.domain.api.subscription_module.exception_handler_subscription.SubtractOperatorException;
import com.example.domain.api.subscription_module.service.SubscriptionPriceCalculateService;
import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.api.subscription_module.service.mapper.SubscribeDataMapper;
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.SubscribeDataDto;
import com.example.domain.dto.SubscriptionDto;
import com.example.domain.dto.SubscriptionPriceReqDto;
import com.example.domain.dto.mapper.MapperDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final CompanyService companyService;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final SubscribeDataMapper subscribeDataMapper;
    private final RoleService roleService;
    private final MapperDto mapperDto;
    private final SubscriptionPriceCalculateService subscriptionPriceCalculateService;


    @Override
    @Transactional
    public SubscriptionDto subscribe(SubscribeDataDto subscribeDataDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userRoleRepository.findRolesByEmail(email).stream()
                .anyMatch(role -> role.equals(Role.MANAGER) || role.equals(Role.OPERATOR))
        ) {
            throw new AlreadyInCompanyException();
        }
        Company company = companyService.createCompany(CompanyDto.builder()
                .name("")
                .contactEmail(email)
                .build()
        );

        subscribeDataDto.setCompany(company);
        userRepository.updateByCompanyIdAndEmail(company.getId(), email);
        roleService.addRole(email,Role.MANAGER);
        roleService.addRole(email,Role.OPERATOR);
        Subscription subscription = subscribeDataMapper.toSubscription(subscribeDataDto);

        return mapperDto.toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Override
    public void cancel() {
        companyService.disbandCompany("");
    }

    @Override
    @Transactional
    public SubscriptionDto getSubscription() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return mapperDto.toSubscriptionDto(subscriptionRepository.findByCompany(
                userRepository.findByEmail(email).map(User::getCompany).orElseThrow(NotFoundUserException::new)
        ).orElseThrow(NotFoundSubscriptionException::new));

    }

    public Float countPrice(SubscriptionPriceReqDto subscriptionPriceDto) {
        return subscriptionPriceCalculateService.calculateTotalPrice(subscriptionPriceDto)
                .floatValue();
    }

    @Override
    @Transactional
    public void addOperatorCount(Company company) {
        subscriptionRepository.save( subscriptionRepository.findByCompany(company)
                .map(subscription -> {
                    int count = subscription.getMaxOperators() - subscription.getCountOperators();
                    if( count <= 0){
                        throw new MaxOperatorsCountException();
                    }
                    subscription.setCountOperators(subscription.getCountOperators() + 1);
                    return subscription;
                }).orElseThrow(NotFoundSubscriptionException::new));

    }

    @Override
    public void subtractOperatorCount(Company company) {
        subscriptionRepository.save( subscriptionRepository.findByCompany(company)
                .map(subscription -> {
                    int count = subscription.getMaxOperators() - (subscription.getCountOperators()-1);
                    if( count >= subscription.getMaxOperators()){
                        throw new SubtractOperatorException();
                    }
                    subscription.setCountOperators(subscription.getCountOperators() - 1);
                    return subscription;
                }).orElseThrow(NotFoundSubscriptionException::new));
    }
}
/*
{
  "price": {
    "operators_count": 4,
    "months_count": 6
  },
  "tariff": "DYNAMIC"
}
 */