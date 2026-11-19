package com.eventsphere.config;

import com.eventsphere.model.EventCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every Thymeleaf page can use. */
@ControllerAdvice(basePackages = "com.eventsphere.web")
public class GlobalModelAttributes {

    private final String currencySymbol;

    public GlobalModelAttributes(@Value("${app.currency-symbol:Rs.}") String currencySymbol) {
        this.currencySymbol = currencySymbol;
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
