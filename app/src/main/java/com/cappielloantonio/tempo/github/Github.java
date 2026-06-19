package com.cappielloantonio.tempo.github;

public class Github {
    private static final String OWNER = "eddyizm";
    private static final String REPO = "Tempus";

    public String getUrl() {
        return "https://api.github.com/";
    }

    public static String getOwner() {
        return OWNER;
    }

    public static String getRepo() {
        return REPO;
    }
}
