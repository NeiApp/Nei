package com.projectii.Nei.ui.review;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@Route(value = "review", layout = MainLayout.class)
@PageTitle("Review | Nei")
public class ReviewView extends NeiView {
    private final ReviewUiService reviews;
    private String reviewMode = "week"; // week | month

    public ReviewView(ReviewUiService reviews) {
        this.reviews = reviews;
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildReview());
    }

    private Component buildReview() {
        boolean week = reviewMode.equals("week");
        ReviewData r = reviews.review(week);

        Button wk = new Button("Week", e -> { reviewMode = "week"; refresh(); });
        Button mo = new Button("Month", e -> { reviewMode = "month"; refresh(); });
        wk.addClassNames("nei-seg", week ? "on" : "off");
        mo.addClassNames("nei-seg", week ? "off" : "on");
        Div segs = new Div(wk, mo);
        segs.addClassName("nei-segs");
        Span range = new Span(r.range());
        range.addClassName("nei-range");
        Div titles = new Div(new H1(r.title()), range);
        Div head = new Div(titles, segs);
        head.addClassName("nei-review-head");

        // hero: the AI's read of the period
        Div label = new Div("\u2726 Nei's read");
        label.addClassName("nei-summary-label");
        Div text = new Div(r.summary());
        text.addClassName("nei-hero-text");
        Div sign = new Div("Written by Nei from your tasks and notes");
        sign.addClassName("nei-signoff");
        Div hero = new Div(label, text, sign);
        hero.addClassName("nei-hero");

        // completion ring + small stats
        int pct = r.total() == 0 ? 0 : r.done() * 100 / r.total();
        Span pctText = new Span(pct + "%");
        Div ring = new Div(pctText);
        ring.addClassName("nei-ring");
        ring.getStyle().set("--p", String.valueOf(pct));
        Div facts = new Div(fact(r.done() + " done"), fact(r.skipped() + " skipped"),
                fact(r.streak() + " day streak"), fact("Best: " + r.best()));
        facts.addClassName("nei-facts");
        Div ringCard = new Div(ring, facts);
        ringCard.addClassName("nei-card");
        ringCard.addClassName("nei-ring-card");

        Div top = new Div(hero, ringCard);
        top.addClassName("nei-review-top");

        // bar chart
        int max = 1;
        for (int c : r.counts()) max = Math.max(max, c);
        Div bars = new Div();
        bars.addClassName("nei-bars");
        for (int i = 0; i < r.counts().length; i++) {
            Div bar = new Div();
            bar.addClassName("nei-bar");
            if (r.counts()[i] == max) bar.addClassName("top");
            bar.getStyle().set("height", (r.counts()[i] * 100 / max) + "%");
            Span n = new Span(String.valueOf(r.counts()[i]));
            n.addClassName("nei-bar-n");
            Div col = new Div(n, bar, new Span(r.labels()[i]));
            col.addClassName("nei-bar-col");
            bars.add(col);
        }
        Div chart = new Div(new H4(week ? "Tasks finished per day" : "Tasks finished per week"), bars);
        chart.addClassName("nei-card");

        Div notes3 = new Div(listCard("What went well", r.wins(), "good"), listCard("Worth watching", r.watch(), "warn"),
                listCard("Try next", List.of(r.tryNext()), "next"));
        notes3.addClassName("nei-review-cols");

        Div page = new Div(head, top, chart, notes3);
        page.addClassName("nei-review");
        return page;
    }

    private Span fact(String t) { Span s = new Span(t); s.addClassName("nei-fact"); return s; }

    private Component listCard(String title, List<String> items, String kind) {
        Div card = new Div(new H4(title));
        card.addClassNames("nei-card", "nei-list-card", kind);
        for (String i : items) {
            Div row = new Div(i);
            row.addClassName("nei-list-row");
            card.add(row);
        }
        return card;
    }
}
