package com.projectii.Nei;

import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopWindow {

    public static void open(String url, ConfigurableApplicationContext ctx) {
        new Thread(() -> {
            try {
                Path profile = Path.of(System.getProperty("user.home"), ".nei", "edge-profile");
                Files.createDirectories(profile);

                Process process = new ProcessBuilder(
                        findEdge(),
                        "--app=" + url,
                        "--user-data-dir=" + profile,
                        "--no-first-run",
                        "--window-size=1100,620")
                        .inheritIO()
                        .start();

                process.waitFor();   // returns when the user closes the window
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ctx.close();
                System.exit(0);
            }
        }, "edge-launcher").start();
    }

    private static String findEdge() {
        String[] paths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"
        };
        for (String p : paths) {
            if (new File(p).exists()) return p;
        }
        return "msedge";
    }
}
