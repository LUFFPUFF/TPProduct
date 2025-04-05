package com.example.domain.api.authentication_service.testAuthService;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/all-perm")
    @Cacheable("permit")
    public String allPerm() throws InterruptedException {
        Thread.sleep(2000);
        return "yeeees";
    }

    @GetMapping("/auth-only")

    public String authOnly() {
        return "you are allowed";
    }

    @GetMapping("/operator-only")

    public String adminOnly() {
        return "you are operator";
    }

    @GetMapping("/manager-only")

    public String managerOnly() {
        return "you are manager";
    }

    @GetMapping("/company-data")

    public String companyData() {
        return "company";
    }

    @GetMapping("/no-perm")
    public String noPerm() {
        return "how?";
    }
}
