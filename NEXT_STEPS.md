# Nei v2 (structured): read this first

This is a copy of your Nei_v1 project. The UI is split into one class per screen, with routes and service
interfaces. Your backend is untouched except for the small fixes listed below.
The full plan is in the roadmap document ("Nei: structure and backend roadmap").

**Not compiled.** I could not build it where I made it. Open it in IntelliJ, run `mvn compile`, and send me any red lines.

## Run it
(Vaadin generates `package.json`, `vite.*.ts`, `tsconfig.json` and `types.d.ts` itself on the first run. They are not in this folder on purpose, and the first start needs internet once to fetch the frontend packages.)

1. Open the folder in IntelliJ and reload Maven.
2. Run `NeiApplication`. Open http://localhost:4000 (it goes to the welcome screen, then /today).
3. Delete `src/main/frontend/generated` if Vite complains, and restart.

## New UI layout (`com.projectii.Nei.ui`)
| Package | What is in it |
|---|---|
| `app` | `MainLayout` (icon rail, welcome-gate guard), `RootRedirect` ("/" goes to Today) |
| `today` `calendar` `notes` `schedule` `review` `settings` `welcome` | one `*View` per screen, each with its own route |
| `service` | **The contract.** `TaskUiService`, `NoteUiService`, `SettingsUiService`, `ReviewUiService`. Each method says what the backend has today. |
| `service/mock` | In-memory versions of those interfaces. This is the only place with fake data. |
| `model` | UI view-models (`TaskItem`, `NoteItem`, `ReviewData`, settings). Fields marked NEW do not exist on the backend yet. |
| `shared` | `NeiView` base class, `TaskDialogs`, `Palette`, `Appearance`, `UiState` |

Routes: `/today`, `/calendar`, `/notes`, `/schedule`, `/review`, `/settings`, `/welcome`.

## How to connect the backend, one screen at a time
1. Pick an interface in `ui/service` (start with `TaskUiService`).
2. Write a real class that implements it using your repositories, and map entities to the `model` classes.
3. Delete (or un-annotate) the matching `Mock*UiService`. The views do not change.
4. Remember `save()` matters: the mock hands out live objects, a real service will not.

## Backend fixes already applied
- `modal` package renamed to `model`.
- `DayLogController`: `dayService` is now `final` (it was never injected).
- `WeeklyReviewController`: real `@RequestMapping("/api/v1/weekly")` and matching path variable name.
- `DayServiceImpl`: `@Transactional` on the two methods that read lazy data.
- `TaskController`: the alarm endpoint returns its body.
- `NeiApplication`: `java.awt.headless=false` (needed for opening the browser and the system tray).
- YAML: `spring.application.name` indentation, lowercase `vaadin`, valid `application-production.yml`.
- `.gitignore` added.

## Still yours to do (search for `TODO(roadmap`)
- **#1** `application.yml`: switch from in-memory H2 to a file database.
- **#5** `WeekService.generateWeeklyReview`: runs every minute; run it when a week ends and set `WeekStatus.COMPLETED`.
- **#6** `TaskServiceImpl.checkAlarms`: only prints; add `nextFireAt` and a `NotificationSender`.
- **#7** `TaskServiceImpl.createTask`: always uses today; add date, list and description.

## Not changed on purpose
`Weeks`/`WeekDay` are still required parents of tasks (roadmap section 4 suggests dropping them: your call).
`nei.css` is still one file, and the Schedule JavaScript is still inside `ScheduleView.FlowCanvas`.
Packaging output (`Rei/`), `node_modules`, `target` and Vaadin's generated files (`package.json`, `package-lock.json`,
`tsconfig.json`, `types.d.ts`, `vite.generated.ts`) were left out. Vaadin recreates them on the first run and `.gitignore` ignores them.
`vite.config.ts` is kept: it is the one file meant for your own Vite settings.
