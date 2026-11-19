package com.eventsphere.config;

import com.eventsphere.model.EventCategory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Lets URLs and forms use any capitalisation ("seminar", "Seminar", "SEMINAR") or the display
 * name ("Tech Talk"). Unknown values become null, which means "no filter", instead of an error.
 */
@Component
public class EventCategoryConverter implements Converter<String, EventCategory> {

    @Override
    public EventCategory convert(String source) {
        String value = source.trim();
        if (value.isEmpty()) {
            return null;
        }
        return Arrays.stream(EventCategory.values())
                .filter(c -> c.name().equalsIgnoreCase(value) || c.getDisplayName().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
