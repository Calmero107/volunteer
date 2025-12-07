package com.web.volunteer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class VolunteerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolunteerApplication.class, args);
        System.out.println("""
            
            ================================================================================
            🌟 VolunteerHub Backend API Started Successfully! 🌟
            ================================================================================
            
            📚 API Documentation: http://localhost:8080/swagger-ui.html
            📊 API Docs (JSON):   http://localhost:8080/v3/api-docs
            ❤️  Health Check:     http://localhost:8080/actuator/health
            
            🔐 Default Accounts:
            ┌─────────────────────────────────────────────────────────────┐
            │ Admin:          admin@volunteerhub.com / Admin@123          │
            │ Event Manager:  manager@volunteerhub.com / Manager@123      │
            │ Volunteer:      volunteer@volunteerhub.com / Volunteer@123  │
            └─────────────────────────────────────────────────────────────┘
            
            🚀 Ready to accept requests!
            ================================================================================
            """);
    }
}

