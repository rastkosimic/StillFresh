package com.stillfresh.app.sharedentities.enums;

public enum Currency {
    EUR("Euro", "€", "European Union", "EUR"),
    GBP("British Pound", "£", "United Kingdom", "GBP"),
    CHF("Swiss Franc", "CHF", "Switzerland, Liechtenstein", "CHF"),
    SEK("Swedish Krona", "kr", "Sweden", "SEK"),
    NOK("Norwegian Krone", "kr", "Norway", "NOK"),
    DKK("Danish Krone", "kr", "Denmark", "DKK"),
    PLN("Polish Złoty", "zł", "Poland", "PLN"),
    HUF("Hungarian Forint", "Ft", "Hungary", "HUF"),
    CZK("Czech Koruna", "Kč", "Czech Republic", "CZK"),
    RON("Romanian Leu", "lei", "Romania", "RON"),
    BGN("Bulgarian Lev", "лв", "Bulgaria", "BGN"),
    ISK("Icelandic Króna", "kr", "Iceland", "ISK"),
    RUB("Russian Ruble", "₽", "Russia", "RUB"),
    UAH("Ukrainian Hryvnia", "₴", "Ukraine", "UAH"),
    TRY("Turkish Lira", "₺", "Turkey", "TRY"),
    MDL("Moldovan Leu", "L", "Moldova", "MDL"),
    ALL("Albanian Lek", "L", "Albania", "ALL"),
    MKD("Macedonian Denar", "ден", "North Macedonia", "MKD"),
    RSD("Serbian Dinar", "дин", "Serbia", "RSD"),
    GEL("Georgian Lari", "₾", "Georgia", "GEL");

    private final String currencyName;
    private final String symbol;
    private final String country;
    private final String isoCode;  // ✅ New field for ISO 4217 currency code

    Currency(String currencyName, String symbol, String country, String isoCode) {
        this.currencyName = currencyName;
        this.symbol = symbol;
        this.country = country;
        this.isoCode = isoCode;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCountry() {
        return country;
    }

    public String getIsoCode() {
        return isoCode;
    }

    @Override
    public String toString() {
        return this.name() + " (" + currencyName + ", " + symbol + ", " + country + ", " + isoCode + ")";
    }
}
