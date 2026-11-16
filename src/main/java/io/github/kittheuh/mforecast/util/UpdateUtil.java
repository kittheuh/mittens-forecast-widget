package io.github.kittheuh.mforecast.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class UpdateUtil {
    private static final String LATEST_API_URL = "https://api.github.com/repos/kittheuh/mittens-forecast-widget/releases/latest";

    public static Version checkForUpdates() {
        JsonElement element = Util.tryGetPage(LATEST_API_URL);
        if (element == null || !element.isJsonObject()) return null;

        JsonObject object = element.getAsJsonObject();
        if (!object.has("tag_name")) return null;

        return toVersion(object.get("tag_name").getAsString());
    }

    public record Version(int major, int minor, int bugCheck) implements Comparable<Version> {
        public boolean isNewer(Version older) {
            return compareTo(older) > 0;
        }

        public boolean isOlder(Version older) {
            return compareTo(older) < 0;
        }

        @Override
        public int compareTo(Version version) {
            if (major == version.major) {
                if (minor == version.minor) {
                    if (bugCheck == version.bugCheck) return 0;
                    return Integer.compare(bugCheck, version.bugCheck);
                }
                return Integer.compare(minor, version.minor);
            }
            return Integer.compare(major, version.major);
        }

        @Override
        public String toString() {
            return "%d.%d.%d".formatted(major, minor, bugCheck);
        }
    }

    public static Version toVersion(String version) {
        int major, minor, bugCheck;

        String[] split = version.trim().split("\\.", 3);
        if (split.length != 3) return null;

        try {
            if (split[0].startsWith("v")) split[0] = split[0].substring(1);
            major = Integer.parseInt(split[0]);
            minor = Integer.parseInt(split[1]);
            bugCheck = Integer.parseInt(split[2]);
            return new Version(major, minor, bugCheck);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
