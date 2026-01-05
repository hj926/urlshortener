package com.huangjie.url_shortener.util;

import java.util.regex.Pattern;

public final class ShortCodeValidator {

    // 1~16 位 Base62（0-9A-Za-z）
    private static final Pattern P = Pattern.compile("^[0-9A-Za-z]{1,16}$");

    private ShortCodeValidator() {}

    public static boolean isValid(String shortCode) {
        return shortCode != null && P.matcher(shortCode).matches();
    }
}
