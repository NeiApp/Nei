package com.projectii.Nei;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Theme("nei")
@Push
public class NeiApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false"); // needed for Desktop.browse() and the system tray
		SpringApplication.run(NeiApplication.class, args);
	}
}
