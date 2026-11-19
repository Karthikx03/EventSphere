package com.eventsphere.config;

import com.eventsphere.model.EventCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every Thymeleaf page can use. */
@ControllerAdvice(basePackages = "com.eventsphere.web")
public class GlobalModelAttributes {

    /** Indian Rupee sign. Written as a Java escape so file encodings can never garble it. */
    private static final String DEFAULT_CURRENCY = "\u20B9";

    private final String currencySymbol;
    private final boolean showDemoAccounts;

    /**
     * Override with app.currency-symbol, e.g. app.currency-symbol=$ (plain ASCII is safest in .properties files).
     */
    public GlobalModelAttributes(@Value("${app.currency-symbol:}") String currencySymbol,
                                 @Value("${app.show-demo-accounts:false}") boolean showDemoAccounts) {
        this.currencySymbol = currencySymbol.isBlank() ? DEFAULT_CURRENCY : currencySymbol;
        this.showDemoAccounts = showDemoAccounts;
    }

    /** Whether the login page lists the demo logins. Off by default so a public site never shows them. */
    @ModelAttribute("showDemoAccounts")
    public boolean showDemoAccounts() {
        return showDemoAccounts;
    }

    @ModelAttribute("currency")
    public String currency() {
        return currencySymbol;
    }

    @ModelAttribute("categories")
    public EventCategory[] categories() {
        return EventCategory.values();
    }
}
