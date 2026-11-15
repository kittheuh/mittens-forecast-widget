package io.github.kittheuh.mforecast.util;

public class EnumUtil {
    public static <T extends Enum<T>> T getEnum(Class<T> clazz, String name) {
        if (clazz == null || !clazz.isEnum() || name == null || name.isBlank()) return null;

        try {
            return Enum.valueOf(clazz, name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }
}
