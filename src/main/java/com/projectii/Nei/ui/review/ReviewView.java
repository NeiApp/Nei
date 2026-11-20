package com.projectii.Nei.ui.review;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Locale;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

/**
 * Review page, laid out like a quiet technical report:
 * top rule with Week / Month tabs, left column = readouts + big number + Nei's read + notes,
 * right column = a spoke dial (one spoke per day or week).
 */
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
        page.addClassName("nei-rv");
        page.add(buildTop(week), buildLeft(r), buildRight(r, week));
        return page;
    }

    // ---------------- top rule: crumb + Week / Month ----------------

    private Component buildTop(boolean week) {
        Span crumb = new Span("Review");
        crumb.addClassName("nei-rv-crumb");

        Div tabs = new Div(tab("Week", "week", week), tab("Month", "month", !week));
        tabs.addClassName("nei-rv-tabs");

        Div top = new Div(crumb, tabs);
        top.addClassName("nei-rv-top");
        return top;
    }

    private Component tab(String label, String mode, boolean on) {
        Span t = new Span(label);
        t.addClassName("nei-rv-tab");
        if (on) t.addClassName("on");
        t.getElement().addEventListener("click", ev -> {
            reviewMode = mode;
            refresh();
        });
        return t;
    }

    // ---------------- left column ----------------

    private Component buildLeft(ReviewData r) {
        Div left = new Div();
        left.addClassName("nei-rv-left");
        left.add(buildName(r), buildReadouts(r), buildBig(r), buildRead(r), buildNotes(r), buildTry(r));
        return left;
    }

    private Component buildName(ReviewData r) {
        Span title = new Span(r.title());
        title.addClassName("nei-rv-title");
        Span range = new Span(r.range());
        range.addClassName("nei-rv-range");
        Div name = new Div(title, range);
        name.addClassName("nei-rv-name");
        return name;
    }

    private Component buildReadouts(ReviewData r) {
        String streak = r.streak() == 1 ? "1 day" : r.streak() + " days";
        Div row = new Div(
                readout("Done", String.valueOf(r.done())),
                readout("Skipped", String.valueOf(r.skipped())),
                readout("Streak", streak),
                readout("Best day", r.best()));
        row.addClassName("nei-rv-readouts");
        return row;
    }

    private Component readout(String label, String value) {
        Span l = new Span(label);
        l.addClassName("nei-rv-ro-l");
        Span v = new Span(value);
        v.addClassName("nei-rv-ro-v");
        Div d = new Div(l, v);
        d.addClassName("nei-rv-ro");
        return d;
    }

    /** The one big number: completion %, with a hairline progress track. */
    private Component buildBig(ReviewData r) {
        int pct = r.total() == 0 ? 0 : r.done() * 100 / r.total();

        Div mark = new Div();
        mark.addClassName("nei-rv-mark");

        Span num = new Span(String.valueOf(pct));
        Span sign = new Span("%");
        sign.addClassName("nei-rv-pct-sign");
        Div pctWrap = new Div(num, sign);
        pctWrap.addClassName("nei-rv-pct");

        Span note = new Span(r.done() + " of " + r.total() + " planned");
        note.addClassName("nei-rv-pct-note");

        Div row = new Div(mark, pctWrap, note);
        row.addClassName("nei-rv-bigrow");

        Div fill = new Div();
        fill.addClassName("nei-rv-fill");
        fill.getStyle().set("width", pct + "%");
        Div track = new Div(fill);
        track.addClassName("nei-rv-track");

        Div big = new Div(row, track);
        big.addClassName("nei-rv-big");
        return big;
    }

    private Component buildRead(ReviewData r) {
        Span tag = new Span("Nei's read");
        tag.addClassName("nei-rv-tag");
        Span text = new Span(r.summary());
        Div p = new Div(tag, text);
        p.addClassName("nei-rv-read");

        Div sign = new Div("Written by Nei from your tasks and notes");
        sign.addClassName("nei-rv-sign");

        Div wrap = new Div(p, sign);
        return wrap;
    }

    private Component buildNotes(ReviewData r) {
        Div notes = new Div(
                noteGroup("What went well", r.wins(), "good"),
                noteGroup("Worth watching", r.watch(), "warn"));
        notes.addClassName("nei-rv-notes");
        return notes;
    }

    private Component noteGroup(String label, List<String> items, String tone) {
        Span l = new Span(label);
        l.addClassName("nei-rv-label");
        Div group = new Div(l);

        if (items == null || items.isEmpty()) {
            Div none = new Div("Nothing to note this period.");
            none.addClassNames("nei-rv-note", "muted");
            group.add(none);
        } else {
            for (String i : items) {
                Div row = new Div(i);
                row.addClassNames("nei-rv-note", tone);
                group.add(row);
            }
        }
        return group;
    }

    private Component buildTry(ReviewData r) {
        Div box = new Div(new Icon(VaadinIcon.ARROW_RIGHT));
        box.addClassName("nei-rv-try-box");

        Span label = new Span("Try next");
        label.addClassName("nei-rv-label");
        Span text = new Span(r.tryNext());
        text.addClassName("nei-rv-try-text");
        Div col = new Div(label, text);
        col.addClassName("nei-rv-try-col");

        Div block = new Div(box, col);
        block.addClassName("nei-rv-try");
        return block;
    }

    // ---------------- right column: spoke dial ----------------

    private Component buildRight(ReviewData r, boolean week) {
        Span title = new Span(week ? "Tasks finished per day" : "Tasks finished per week");
        title.addClassName("nei-rv-chart-title");
        Span sub = new Span(week ? "One spoke per day" : "One spoke per week");
        sub.addClassName("nei-rv-chart-sub");
        Div head = new Div(title, sub);
        head.addClassName("nei-rv-chart-head");

        Div dial = new Div();
        dial.addClassName("nei-rv-dial");
        dial.getElement().setProperty("innerHTML", dialSvg(r)); // browser parses the SVG, so viewBox keeps its case

        Div right = new Div(head, dial);
        right.addClassName("nei-rv-right");
        return right;
    }

    /** One spoke per entry, length = tasks finished; the best one gets the accent. */
    private String dialSvg(ReviewData r) {
        int[] counts = r.counts();
        String[] labels = r.labels();
        int n = Math.max(counts.length, 1);
        int max = 1;
        for (int c : counts) max = Math.max(max, c);

        final double cx = 160, cy = 160, inner = 48, minLen = 6, span = 52, labelR = 142;

        StringBuilder s = new StringBuilder();
        s.append("<svg viewBox=\"0 0 320 320\" role=\"img\" aria-label=\"Tasks finished chart\">");
        s.append("<circle class=\"rv-ring\" cx=\"160\" cy=\"160\" r=\"42\"/>");
        s.append("<circle class=\"rv-ring dash\" cx=\"160\" cy=\"160\" r=\"128\"/>");

        for (int i = 0; i < counts.length; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / n;
            double cos = Math.cos(a), sin = Math.sin(a);
            double len = minLen + span * counts[i] / max;
            boolean top = counts[i] > 0 && counts[i] == max;
            String cls = top ? " top" : "";

            s.append("<line class=\"rv-spoke").append(cls).append("\" x1=\"").append(f(cx + inner * cos))
                    .append("\" y1=\"").append(f(cy + inner * sin))
                    .append("\" x2=\"").append(f(cx + (inner + len) * cos))
                    .append("\" y2=\"").append(f(cy + (inner + len) * sin)).append("\"/>");

            double nr = inner + len + 12;
            s.append("<text class=\"rv-num").append(cls).append("\" text-anchor=\"middle\" dominant-baseline=\"central\" x=\"")
                    .append(f(cx + nr * cos)).append("\" y=\"").append(f(cy + nr * sin)).append("\">")
                    .append(counts[i]).append("</text>");

            String label = i < labels.length ? labels[i] : "";
            s.append("<text class=\"rv-lab\" text-anchor=\"middle\" dominant-baseline=\"central\" x=\"")
                    .append(f(cx + labelR * cos)).append("\" y=\"").append(f(cy + labelR * sin)).append("\">")
                    .append(esc(label)).append("</text>");
        }

        s.append("<text class=\"rv-center\" text-anchor=\"middle\" x=\"160\" y=\"166\">").append(r.done()).append("</text>");
        s.append("<text class=\"rv-center-sub\" text-anchor=\"middle\" x=\"160\" y=\"184\">finished</text>");
        s.append("</svg>");
        return s.toString();
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}