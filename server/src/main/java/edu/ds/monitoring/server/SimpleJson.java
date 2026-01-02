package edu.ds.monitoring.server;

public final class SimpleJson {
    private SimpleJson() {}

    // extrait la valeur string d'une clé JSON: "agentId":"agent-001"
    public static String getString(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        i += needle.length();

        // skip spaces
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

        if (i < json.length() && json.charAt(i) == '"') {
            int start = i + 1;
            int end = json.indexOf('"', start);
            if (end < 0) return null;
            return json.substring(start, end);
        }
        return null;
    }
    public static Double getDouble(String json, String key) {
           return getNumber(json, key);
        }

    public static Long getLong(String json, String key) {
           Double d = getNumber(json, key);
           return d == null ? null : d.longValue();
}


    // extrait un nombre: "cpuPct":12.3  ou "timestamp":1735
    public static Double getNumber(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        i += needle.length();

        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

        int start = i;
        while (i < json.length()) {
            char c = json.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' ) {
                i++;
            } else {
                break;
            }
        }
        if (start == i) return null;
        return Double.valueOf(json.substring(start, i));
    }
}
