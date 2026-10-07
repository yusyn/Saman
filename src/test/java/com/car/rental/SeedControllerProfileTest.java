package com.car.rental;

import com.car.rental.api.SeedController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class SeedControllerProfileTest {

    @Autowired
    ApplicationContext context;

    @Test
    void seedController_isRegistered_inDevProfile() {
        assertThat(context.getBeansOfType(SeedController.class)).isNotEmpty();
    }
}
