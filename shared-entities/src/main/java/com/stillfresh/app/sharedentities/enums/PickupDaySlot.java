package com.stillfresh.app.sharedentities.enums;

/**
 * Derived (non-persisted) grouping for pickup day relative to "today".
 * Typically used by the UI to display sections like "Collect today" / "Collect tomorrow".
 */
public enum PickupDaySlot {
    TODAY,
    TOMORROW,
    FUTURE,
    PAST
}


