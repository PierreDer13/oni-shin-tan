package fr.onishintan.ui.lan;

/**
 * A game code instead of a network address: the host's 4 address numbers written in 7 easy signs, like "K7QX-2MB".
 * No 0 / O or 1 / I, so it can be read out loud without mistakes.
 */
public final class LanCode {
    /** 32 signs: each one holds 5 bits, so 7 of them hold the 32 bits of an address. */
    private static final String SIGNS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 7;

    private LanCode() { }

    /** The code of an address like "192.168.1.12". */
    public static String of(String address) {
        long number = 0;
        for (String part : address.split("\\.")) {
            number = number * 256 + Integer.parseInt(part);
        }
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            code.insert(0, SIGNS.charAt((int) (number % 32)));
            number /= 32;
        }
        return code.substring(0, 4) + "-" + code.substring(4);
    }

    /** The address behind a code (case, spaces and dashes don't matter); an address typed as is stays itself; null if it is neither. */
    public static String address(String typed) {
        String text = typed.trim();
        if (text.contains(".")) {
            return text;   // an address typed directly (testing on one computer: 127.0.0.1)
        }
        String code = text.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (code.length() != LENGTH) {
            return null;
        }
        long number = 0;
        for (char sign : code.toCharArray()) {
            int value = SIGNS.indexOf(sign);
            if (value < 0) {
                return null;
            }
            number = number * 32 + value;
        }
        return (number >> 24 & 255) + "." + (number >> 16 & 255) + "." + (number >> 8 & 255) + "." + (number & 255);
    }
}
