package com.angelrojo.webapp;

import java.util.Locale;

public class Countries {

    public static String getCountry(final String isoCode, final Locale lang) {
        return new Locale("", isoCode).getDisplayCountry(lang);
    }
}
