package com.nfsmw.android;

import android.os.Bundle;

import org.libsdl.app.SDLActivity;

import java.io.File;

public final class GameActivity extends SDLActivity {
    private String gameRoot;
    private String userRoot;
    private String cacheRoot;

    @Override
    protected void onCreate(Bundle state) {
        File appRoot = new File(getFilesDir(), "nfsmw");
        gameRoot = new File(appRoot, "game_root").getAbsolutePath();
        userRoot = new File(appRoot, "user").getAbsolutePath();
        cacheRoot = new File(appRoot, "cache").getAbsolutePath();
        new File(userRoot).mkdirs();
        new File(cacheRoot).mkdirs();
        super.onCreate(state);
    }

    @Override
    protected String[] getArguments() {
        return new String[] {
                "--game_data_root=" + gameRoot,
                "--user_data_root=" + userRoot,
                "--cache_root=" + cacheRoot
        };
    }
}
