package com.projectii.Nei.ui.schedule;

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

@Route(value = "schedule", layout = MainLayout.class)
@PageTitle("Schedule | Nei")
public class ScheduleView extends NeiView {
    private final TaskUiService tasks;
    private final UiState state;

    public ScheduleView(TaskUiService tasks, UiState state) {
        this.tasks = tasks;
        this.state = state;
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildSchedule());
    }

    private Component buildSchedule() {
        Div page = new Div();
        page.addClassName("nei-schedule");

        Div titles = new Div(new H1("Schedule"),
                new Paragraph("Drag the hand to arrange. Pull from the right dot to another task to thread them. Double-click a thread to cut it."));
        Button play = new Button(state.running == null ? "Play" : "Restart", new Icon(VaadinIcon.PLAY), e -> startFlow());
        play.addClassName("nei-btn-primary");
        Button stop = new Button("Stop", new Icon(VaadinIcon.STOP), e -> { state.running = null; refresh(); });
        stop.addClassName("nei-btn-ghost");
        Div bar = new Div(titles);
        bar.addClassName("nei-schedule-bar");
        if (state.running != null) {
            Span now = new Span("Now: " + state.running.title + (state.running.next != null ? "  \u2192  next: " + state.running.next.title : ""));
            now.addClassName("nei-now");
            bar.add(now);
        }
        Div btns = new Div(play, stop);
        btns.addClassName("nei-flow-btns");
        bar.add(btns);

        FlowCanvas canvas = new FlowCanvas();
        canvas.add(new Html("<svg class='nei-threads'></svg>"));
        int i = 0;
        for (TaskItem t : tasks.all()) {
            if (t.x < 0) { t.x = 40 + (i % 4) * 250; t.y = 60 + (i / 4) * 150; tasks.save(t); }
            i++;
            canvas.add(flowNode(t));
        }
        canvas.init(linksJson());
        page.add(bar, canvas);
        return page;
    }

    private Component flowNode(TaskItem t) {
        Div n = new Div();
        n.addClassName("nei-node");
        n.addClassName(t.done ? "done" : t == state.running ? "active" : "idle");
        n.getElement().setAttribute("data-id", t.id);
        n.getStyle().set("left", t.x + "px").set("top", t.y + "px");

        Span in = new Span();
        in.addClassNames("nei-port", "in");
        Span out = new Span();
        out.addClassNames("nei-port", "out");
        Div grip = new Div(new Icon(VaadinIcon.HAND));
        grip.addClassName("nei-grip");
        grip.getElement().setAttribute("title", "Drag to arrange");

        Div title = new Div(t.title);
        title.addClassName("nei-node-title");
        Div meta = new Div(t.time.isBlank() ? "Anytime" : t.time);
        meta.addClassName("nei-node-meta");
        Div body = new Div(title, meta);
        body.addClassName("nei-node-body");

        n.add(in, grip, body, out);
        if (t == state.running) {
            Button fin = new Button("Finished, next \u2192", e -> finishCurrent());
            fin.addClassName("nei-btn-primary");
            n.add(fin);
        }
        return n;
    }

    private String linksJson() {
        StringBuilder sb = new StringBuilder("[");
        for (TaskItem t : tasks.all()) {
            if (t.next == null) continue;
            String st = t.done ? "done" : t == state.running ? "active" : "idle";
            if (sb.length() > 1) sb.append(",");
            sb.append("{\"from\":\"").append(t.id).append("\",\"to\":\"").append(t.next.id)
                    .append("\",\"state\":\"").append(st).append("\"}");
        }
        return sb.append("]").toString();
    }

    private void startFlow() {
        state.running = tasks.all().stream()
                .filter(t -> !t.done && tasks.all().stream().noneMatch(o -> o.next == t))
                .findFirst().orElse(null);
        if (state.running == null) Notification.show("Nothing to run. Add tasks or thread some together.");
        refresh();
    }

    private void finishCurrent() {
        if (state.running == null) return;
        state.running.done = true;
        tasks.save(state.running); // MOCK: taskService.completeATaskHandler(id)
        TaskItem nxt = state.running.next;
        while (nxt != null && nxt.done) nxt = nxt.next;
        state.running = nxt;
        Notification.show(state.running == null ? "All done. Nice work." : "Up next: " + state.running.title);
        refresh();
    }

    /** Canvas with drag + thread logic on the client; positions and links sync back to the server. */
    class FlowCanvas extends Div {
        private static final String SCRIPT = """
            const c = $0;
            const links = JSON.parse($1);
            const svg = c.querySelector('svg.nei-threads');
            const NS = 'http://www.w3.org/2000/svg';
            const node = id => c.querySelector('.nei-node[data-id="' + id + '"]');
            const outP = n => ({x: n.offsetLeft + n.offsetWidth, y: n.offsetTop + n.offsetHeight / 2});
            const inP = n => ({x: n.offsetLeft, y: n.offsetTop + n.offsetHeight / 2});
            const curve = (a, b) => {
              const dx = Math.max(60, Math.abs(b.x - a.x) / 2);
              return 'M' + a.x + ' ' + a.y + ' C' + (a.x + dx) + ' ' + a.y + ' ' + (b.x - dx) + ' ' + b.y + ' ' + b.x + ' ' + b.y;
            };
            const draw = () => {
              svg.innerHTML = '';
              links.forEach(l => {
                const a = node(l.from), b = node(l.to);
                if (!a || !b) return;
                const p = document.createElementNS(NS, 'path');
                p.setAttribute('d', curve(outP(a), inP(b)));
                p.setAttribute('class', 'nei-thread ' + l.state);
                p.addEventListener('dblclick', () => c.$server.unlink(l.from, l.to));
                svg.appendChild(p);
              });
            };
            c.querySelectorAll('.nei-node').forEach(n => {
              const grip = n.querySelector('.nei-grip');
              grip.addEventListener('pointerdown', e => {
                e.preventDefault();
                grip.setPointerCapture(e.pointerId);
                n.classList.add('dragging');
                const sx = e.clientX, sy = e.clientY, ox = n.offsetLeft, oy = n.offsetTop;
                const move = ev => {
                  n.style.left = Math.max(0, ox + ev.clientX - sx) + 'px';
                  n.style.top = Math.max(0, oy + ev.clientY - sy) + 'px';
                  draw();
                };
                const up = () => {
                  grip.removeEventListener('pointermove', move);
                  grip.removeEventListener('pointerup', up);
                  n.classList.remove('dragging');
                  c.$server.move(n.dataset.id, n.offsetLeft, n.offsetTop);
                };
                grip.addEventListener('pointermove', move);
                grip.addEventListener('pointerup', up);
              });
              const port = n.querySelector('.nei-port.out');
              port.addEventListener('pointerdown', e => {
                e.preventDefault();
                e.stopPropagation();
                port.setPointerCapture(e.pointerId);
                const t = document.createElementNS(NS, 'path');
                t.setAttribute('class', 'nei-thread temp');
                svg.appendChild(t);
                const move = ev => {
                  const r = c.getBoundingClientRect();
                  t.setAttribute('d', curve(outP(n), {x: ev.clientX - r.left + c.scrollLeft, y: ev.clientY - r.top + c.scrollTop}));
                };
                const up = ev => {
                  port.removeEventListener('pointermove', move);
                  port.removeEventListener('pointerup', up);
                  t.remove();
                  const el = document.elementFromPoint(ev.clientX, ev.clientY);
                  const to = el && el.closest('.nei-node');
                  if (to && to !== n) c.$server.link(n.dataset.id, to.dataset.id);
                };
                port.addEventListener('pointermove', move);
                port.addEventListener('pointerup', up);
              });
            });
            requestAnimationFrame(draw);
            """;

        FlowCanvas() { addClassName("nei-canvas"); }

        void init(String linksJson) { getElement().executeJs(SCRIPT, getElement(), linksJson); }

        @ClientCallable
        public void move(String id, double x, double y) {
            tasks.move(id, x, y);
        }

        @ClientCallable
        public void link(String fromId, String toId) {
            if (!tasks.link(fromId, toId)) Notification.show("That would make a loop");
            refresh();
        }

        @ClientCallable
        public void unlink(String fromId, String toId) {
            tasks.unlink(fromId, toId);
            refresh();
        }
    }
}
