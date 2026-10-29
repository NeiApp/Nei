package com.projectii.Nei.ui.review;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@CssImport("./styles/components.css")
@CssImport("./styles/review.css")
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

        Div page = new Div();
        page.addClassName("nei-review");
        page.add(buildHead(r, week));
        page.add(buildPulse(r));
        page.add(buildSplit(r, week));
        return page;
    }

    // ---- header: title + range + week/month toggle ----
    private Component buildHead(ReviewData r, boolean week) {
        Div titles = new Div();
        titles.addClassName("nei-review-titles");
        H1 h1 = new H1(r.title());
        Span range = new Span(r.range());
        range.addClassName("nei-range");
        titles.add(h1);
        titles.add(range);

        Button wk = new Button();
        wk.setText("Week");
        wk.addClassName("nei-seg");
        if (week) wk.addClassName("on");
        wk.addClickListener(e -> { reviewMode = "week"; refresh(); });

        Button mo = new Button();
        mo.setText("Month");
        mo.addClassName("nei-seg");
        if (!week) mo.addClassName("on");
        mo.addClickListener(e -> { reviewMode = "month"; refresh(); });

        Div segs = new Div(wk, mo);
        segs.addClassName("nei-segs");

        Div head = new Div(titles, segs);
        head.addClassName("nei-review-head");
        return head;
    }

    // ---- pulse strip: ring + the four numbers, one quiet row ----
    private Component buildPulse(ReviewData r) {
        int pct = r.total() == 0 ? 0 : r.done() * 100 / r.total();

        Span pctText = new Span(pct + "%");
        Div ring = new Div(pctText);
        ring.addClassName("nei-ring");
        ring.getStyle().set("--p", String.valueOf(pct));

        Div facts = new Div();
        facts.addClassName("nei-pulse-facts");
        facts.add(pulseFact("Done", String.valueOf(r.done())));
        facts.add(pulseFact("Skipped", String.valueOf(r.skipped())));
        facts.add(pulseFact("Streak", r.streak() + " days"));
        facts.add(pulseFact("Best day", r.best()));

        Div pulse = new Div(ring, facts);
        pulse.addClassName("nei-pulse");
        return pulse;
    }

    private Component pulseFact(String label, String value) {
        Span l = new Span(label);
        l.addClassName("nei-fact-label");
        Span v = new Span(value);
        v.addClassName("nei-fact-value");
        Div f = new Div(l, v);
        f.addClassName("nei-fact");
        return f;
    }

    // ---- split: left story, right evidence ----
    private Component buildSplit(ReviewData r, boolean week) {
        Div story = new Div();
        story.addClassName("nei-review-story");
        story.add(buildHero(r));
        story.add(buildTryNext(r));

        Div data = new Div();
        data.addClassName("nei-review-data");
        data.add(buildChart(r, week));
        data.add(buildLists(r));

        Div split = new Div(story, data);
        split.addClassName("nei-review-split");
        return split;
    }

    private Component buildHero(ReviewData r) {
        Span label = new Span("✦ Nei's read");
        label.addClassName("nei-summary-label");

        Div text = new Div(r.summary());
        text.addClassName("nei-hero-text");

        Div sign = new Div("Written by Nei from your tasks and notes");
        sign.addClassName("nei-signoff");

        Div hero = new Div(label, text, sign);
        hero.addClassName("nei-hero");
        return hero;
    }

    private Component buildTryNext(ReviewData r) {
        Span label = new Span("Try next");
        label.addClassName("nei-try-label");
        Div text = new Div(r.tryNext());
        text.addClassName("nei-try-text");
        Div block = new Div(label, text);
        block.addClassName("nei-try-next");
        return block;
    }

    private Component buildChart(ReviewData r, boolean week) {
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

        H4 title = new H4(week ? "Tasks finished per day" : "Tasks finished per week");
        title.addClassName("nei-data-title");
        Div chart = new Div(title, bars);
        chart.addClassName("nei-review-chart");
        return chart;
    }

    private Component buildLists(ReviewData r) {
        Div wrap = new Div();
        wrap.addClassName("nei-review-lists");
        wrap.add(listBlock("What went well", r.wins(), "good"));
        wrap.add(listBlock("Worth watching", r.watch(), "warn"));
        return wrap;
    }

    private Component listBlock(String title, List<String> items, String tone) {
        H4 t = new H4(title);
        t.addClassName("nei-list-title");
        Div block = new Div();
        block.addClassName("nei-list-block");
        block.addClassName(tone);
        block.add(t);
        if (items == null || items.isEmpty()) {
            Div none = new Div("Nothing to note this period.");
            none.addClassName("nei-review-item");
            none.addClassName("muted");
            block.add(none);
        } else {
            for (String i : items) {
                Div row = new Div(i);
                row.addClassName("nei-review-item");
                row.addClassName(tone);
                block.add(row);
            }
        }
        return block;
    }
}