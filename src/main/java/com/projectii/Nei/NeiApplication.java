package com.projectii.Nei;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Theme("nei")
@Push
@JsModule("./mine.js")
@Meta(name = "theme-color", content = "#eef0ea")
public class NeiApplication implements AppShellConfigurator {


	@Override
	public void configurePage(AppShellSettings settings) {
		settings.addFavIcon("icon", "META-INF/resources/icons/cat.png", "256x256");
		settings.addLink("shortcut icon", "META-INF/resources/icons/cat.png");
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(NeiApplication.class)
				.headless(false)
				.run(args);

		int port = ctx.getEnvironment().getProperty("local.server.port", Integer.class);
		DesktopWindow.open("http://127.0.0.1:" + port, ctx);
	}
}
