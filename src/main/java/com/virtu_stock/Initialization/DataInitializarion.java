package com.virtu_stock.Initialization;

import java.util.LinkedHashMap;
import java.util.List;

import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.virtu_stock.Enum.Role;
import com.virtu_stock.IPO.IPO;
import com.virtu_stock.IPO.IPORepository;
import com.virtu_stock.User.User;
import com.virtu_stock.User.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializarion implements CommandLineRunner {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final IPORepository ipoRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        boolean datainit = true;

        if (datainit) {
            Optional<User> userOptional = userRepository.findByEmail("ms2.o.edu@gmail.com");
            User user = userOptional.get();
            Set<Role> userRoles = user.getRoles();
            userRoles.removeAll(userRoles);
            userRoles.add(Role.ROLE_USER);
            userRoles.add(Role.ROLE_ADMIN);
            user.setRoles(userRoles);
            user.setPassword(passwordEncoder.encode("ms@2.o"));
            userRepository.save(user);
        }

        boolean subscriptionInit = false;
        if (subscriptionInit) {
            List<IPO> totalIpos = ipoRepository.findAll();
            // int count = 2;
            for (IPO ipo : totalIpos) {
                if (ipo.getSubscriptions() == null || ipo.getSubscriptions().isEmpty()) {
                    ipo.setSubscriptions(new LinkedHashMap<>());
                }
                ipo.getSubscriptions().putIfAbsent("QIB", 0.0);
                ipo.getSubscriptions().putIfAbsent("Non-Institutional", 0.0);
                ipo.getSubscriptions().putIfAbsent("Retailer", 0.0);
                ipo.getSubscriptions().putIfAbsent("Total", 0.0);

            }
        }

    }
}
