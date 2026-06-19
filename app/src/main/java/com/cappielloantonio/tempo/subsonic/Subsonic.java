package com.cappielloantonio.tempo.subsonic;

import com.cappielloantonio.tempo.subsonic.base.Version;

import java.util.HashMap;
import java.util.Map;

public class Subsonic {
    private static final Version API_MAX_VERSION = Version.of("1.15.0");

    private final Version apiVersion = API_MAX_VERSION;
    private final SubsonicPreferences preferences;

    public Subsonic(SubsonicPreferences preferences) {
        this.preferences = preferences;
    }

    public Version getApiVersion() {
        return apiVersion;
    }

    public String getUrl() {
        String serverUrl = preferences.getServerUrl();
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            return "http://localhost/rest/";
        }
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://" + serverUrl;
        }
        String url = serverUrl + "/rest/";
        return url.replace("//rest", "/rest");
    }

    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("u", preferences.getUsername());

        if (preferences.getAuthentication().getPassword() != null)
            params.put("p", preferences.getAuthentication().getPassword());
        if (preferences.getAuthentication().getSalt() != null)
            params.put("s", preferences.getAuthentication().getSalt());
        if (preferences.getAuthentication().getToken() != null)
            params.put("t", preferences.getAuthentication().getToken());

        params.put("v", getApiVersion().getVersionString());
        params.put("c", preferences.getClientName());
        params.put("f", "json");

        return params;
    }
}
