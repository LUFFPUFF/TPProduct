package com.example.domain.api.subscription_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.subscription.Subscription;
import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.SubscriptionRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.authentication_module.service.interfaces.RoleService;
import com.example.domain.api.company_module.service.CompanyService;
import com.example.domain.api.subscription_module.exception_handler_subscription.AlreadyInCompanyException;
import com.example.domain.api.subscription_module.exception_handler_subscription.MaxOperatorsCountException;
import com.example.domain.api.subscription_module.exception_handler_subscription.NotFoundSubscriptionException;
import com.example.domain.api.subscription_module.exception_handler_subscription.SubtractOperatorException;
import com.example.domain.api.subscription_module.service.SubscriptionPriceCalculateService;
import com.example.domain.api.subscription_module.service.SubscriptionService;
import com.example.domain.api.subscription_module.service.mapper.SubscribeDataMapper;
import com.example.domain.dto.*;
import com.example.domain.dto.mapper.MapperDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final SubscribeDataMapper subscribeDataMapper;
    private final RoleService roleService;
    private final MapperDto mapperDto;
    private final SubscriptionPriceCalculateService subscriptionPriceCalculateService;
    private final CurrentUserDataService currentUserDataService;


    @Override
    @Transactional
    public SubscriptionDto subscribe(SubscribeDataDto subscribeDataDto) {
        String email = currentUserDataService.getUserEmail();
        List<Role> roleList = currentUserDataService.getRoleList();
        if (roleList.contains(Role.MANAGER)) {
         return  renewSubscription(subscribeDataDto);
        } else if (roleList.contains(Role.OPERATOR)) {
            throw new AlreadyInCompanyException();
        }
        return subscribeNewUser(subscribeDataDto, email);

    }
    @Override
    @Transactional
    public SubscriptionDto renewSubscription(SubscribeDataDto subscribeDataDto) {
        Company company = currentUserDataService.getUser().getCompany();
        Subscription subscription = subscriptionRepository.findByCompany(company).orElseThrow(NotFoundSubscriptionException::new);
        subscribeDataDto.setCompany(company);
        if(subscribeDataDto.getPrice().getOperators_count() < subscription.getCountOperators()){
            throw new MaxOperatorsCountException();
        }
        //оплата
        subscription = subscribeDataMapper.toSubscription(subscribeDataDto);
        return mapperDto.toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Override
    public PriceDto getExtendPrice(SubscriptionExtendPriceDto subscriptionExtendPriceDto) {
        return PriceDto.builder().price(subscriptionPriceCalculateService.calculateTotalPrice(subscriptionExtendPriceDto,
                        subscriptionRepository.findByCompany(currentUserDataService.getUser()
                                .getCompany()).orElseThrow(NotFoundUserException::new))
                .floatValue()).build();
    }

    @Override
    @Transactional
    public SubscriptionDto extendSubscription(SubscribeExtendDataDto subscribeDataDto) {
        Subscription subscription = subscriptionRepository.findByCompany(currentUserDataService.getUser().getCompany()).orElseThrow(NotFoundSubscriptionException::new);
        subscription.setCost(getExtendPrice(subscribeDataDto.getPrice()).getPrice());
        subscription.setEndSubscription(subscription.getEndSubscription().plusMonths(subscribeDataDto.getPrice().getMonths_count()));
        subscription.setCountOperators(subscription.getCountOperators()+subscribeDataDto.getPrice().getOperators_count());
        return mapperDto.toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Override
    public SubscriptionDto subscribeNewUser(SubscribeDataDto subscribeDataDto, String email) {
        Company company = companyService.createCompany(CompanyDto.builder()
                .name("")
                .contactEmail(email)
                .build()
        );

        subscribeDataDto.setCompany(company);
        userRepository.updateByCompanyIdAndEmail(company.getId(), email);
        roleService.addRole(email, Role.MANAGER);
        roleService.addRole(email, Role.OPERATOR);
        Subscription subscription = subscribeDataMapper.toSubscription(subscribeDataDto);

        return mapperDto.toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Override
    public void cancel() {

    }


    @Override
    @Transactional
    public SubscriptionDto getSubscription() {
        return mapperDto.toSubscriptionDto(subscriptionRepository.findByCompany(
                currentUserDataService.getUser().getCompany()
        ).orElseThrow(NotFoundSubscriptionException::new));

    }

    @Override
    public PriceDto countPrice(SubscriptionPriceReqDto subscriptionPriceDto) {
        return PriceDto.builder().price(subscriptionPriceCalculateService.calculateTotalPrice(subscriptionPriceDto)
                .floatValue()).build();
    }

    @Override
    @Transactional
    public void addOperatorCount(Company company) {
        subscriptionRepository.save(subscriptionRepository.findByCompany(company)
                .map(subscription -> {
                    int count = subscription.getMaxOperators() - subscription.getCountOperators();
                    if (count <= 0) {
                        throw new MaxOperatorsCountException();
                    }
                    subscription.setCountOperators(subscription.getCountOperators() + 1);
                    return subscription;
                }).orElseThrow(NotFoundSubscriptionException::new));

    }

    @Override
    public void subtractOperatorCount(Company company) {
        subscriptionRepository.save(subscriptionRepository.findByCompany(company)
                .map(subscription -> {
                    int count = subscription.getMaxOperators() - (subscription.getCountOperators() - 1);
                    if (count >= subscription.getMaxOperators()) {
                        throw new SubtractOperatorException();
                    }
                    subscription.setCountOperators(subscription.getCountOperators() - 1);
                    return subscription;
                }).orElseThrow(NotFoundSubscriptionException::new));
    }
}