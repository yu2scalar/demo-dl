package com.example.demo_dl.util;

public class VersionUtil {

    /**
     * Convert version string to versioned class name suffix
     * Examples:
     *   "1.0.0" -> "V1_0_0"
     *   "1.2.3" -> "V1_2_3"
     *   "10.10.10" -> "V10_10_10"
     */
    public static String formatVersionSuffix(String version) {
        if (version == null || version.isEmpty()) {
            return "V1_0_0";  // Default version
        }
        return "V" + version.replace(".", "_");
    }

    /**
     * Generate versioned name (used for both class names and file names)
     * Examples:
     *   ("UserUpdater", "1.0.0") -> "UserUpdaterV1_0_0"
     *   ("OrderReader", "2.3.4") -> "OrderReaderV2_3_4"
     */
    public static String getVersionedName(String baseName, String version) {
        return baseName + formatVersionSuffix(version);
    }

    /**
     * @deprecated Use getVersionedName() instead. Kept for backward compatibility.
     */
    @Deprecated
    public static String getVersionedClassName(String baseName, String version) {
        return getVersionedName(baseName, version);
    }

    /**
     * @deprecated Use getVersionedName() instead. Kept for backward compatibility.
     */
    @Deprecated
    public static String getVersionedFileName(String baseName, String version) {
        return getVersionedName(baseName, version);
    }

    /**
     * Extract base name from versioned name
     * Supports both formats for backward compatibility:
     *   "UserUpdaterV1_0_0" -> "UserUpdater"
     *   "UserUpdater_V1_0_0" -> "UserUpdater" (legacy format)
     */
    public static String extractBaseName(String versionedName) {
        // Try new format first (no underscore before V)
        int versionIndex = versionedName.indexOf("V");
        if (versionIndex > 0) {
            // Check if this is actually the version marker (followed by digits)
            if (versionIndex < versionedName.length() - 1 &&
                Character.isDigit(versionedName.charAt(versionIndex + 1))) {
                return versionedName.substring(0, versionIndex);
            }
        }

        // Fall back to legacy format (underscore before V)
        versionIndex = versionedName.indexOf("_V");
        if (versionIndex > 0) {
            return versionedName.substring(0, versionIndex);
        }

        return versionedName;
    }

    /**
     * Extract version from versioned name
     * Supports both formats for backward compatibility:
     *   "UserUpdaterV1_0_0" -> "1.0.0"
     *   "UserUpdater_V1_0_0" -> "1.0.0" (legacy format)
     */
    public static String extractVersion(String versionedName) {
        // Try new format first (no underscore before V)
        int versionIndex = versionedName.indexOf("V");
        if (versionIndex > 0 && versionIndex < versionedName.length() - 1) {
            if (Character.isDigit(versionedName.charAt(versionIndex + 1))) {
                String versionSuffix = versionedName.substring(versionIndex + 1);
                return versionSuffix.replace("_", ".");
            }
        }

        // Fall back to legacy format (underscore before V)
        versionIndex = versionedName.indexOf("_V");
        if (versionIndex > 0) {
            String versionSuffix = versionedName.substring(versionIndex + 2);
            return versionSuffix.replace("_", ".");
        }

        return "1.0.0";  // Default
    }
}
