package ecommerce.service.impl;

import ecommerce.courier.CourierProvider;
import ecommerce.entity.CourierAccount;
import ecommerce.repository.CourierAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Makes sure every registered courier has a courier_accounts row. Steadfast is the default on first run. */
@Component
@RequiredArgsConstructor
public class CourierAccountSeeder implements ApplicationRunner {

    private final List<CourierProvider> providers;
    private final CourierAccountRepository accounts;

    @Override
    public void run(ApplicationArguments args) {
        boolean hasDefault = accounts.findFirstByEnabledTrueAndDefaultProviderTrue().isPresent();
        for (CourierProvider p : providers) {
            if (accounts.findByProvider(p.key()).isPresent()) continue;
            CourierAccount a = new CourierAccount();
            a.setProvider(p.key());
            a.setDisplayName(p.displayName());
            // Only the original courier starts enabled; others are switched on from the admin screen.
            a.setEnabled("steadfast".equals(p.key()));
            if (!hasDefault && "steadfast".equals(p.key())) {
                a.setDefaultProvider(true);
                hasDefault = true;
            }
            accounts.save(a);
        }
    }
}
