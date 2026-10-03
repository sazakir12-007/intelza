# Intelza — Requirements (version 1.0)

Confirmed on 2 October 2026.

## Purpose

Intelza helps primary school teachers check how well students understood a topic right
after teaching it, in any subject.

Only the teacher uses the app, on one Android phone. Each student gets a printed card with
their own AprilTag. The teacher shows a question and students turn their card so their
chosen letter is on top. The teacher sweeps the camera across the room, the app records
every answer at once, and then shows who understood and who didn't.

## Functional requirements

### A. Classes and students
- **A1** Create, edit and archive classes (e.g. "Grade 3 – B").
- **A2** Add students with a name and roll number. Each gets a card number that is
  auto-assigned, editable and unique within the class.
- **A3** Import a student list from a CSV file.
- **A4** Mark students absent for a session; they're left out of that session's results.

### B. Answer cards
- **B1** Each card has one large AprilTag (tag36h11 family), the letters A–D on its four
  edges, the card number and, optionally, the student's name.
- **B2** Print cards to PDF, either 2 per page (standard) or 1 per page (large, for big
  rooms), on A4 or Letter paper. Print directly or share the PDF.
- **B3** The letters are printed small so neighbours can't easily copy answers.
- **B4** Cards without names can be reused across classes, because the card number maps to
  each class's roster.

### C. Question bank
- **C1** Subjects: Maths, English, Science and Social Studies are pre-filled, and teachers
  can add their own.
- **C2** Topics under each subject, with an optional grade.
- **C3** Question types: multiple choice (2–4 options), True/False (A = True, B = False),
  and poll/self-check (no correct answer).
- **C4** Each question has text, option texts, the correct answer and an optional picture
  from the camera or gallery.
- **C5** Edit, reorder, duplicate and delete questions.

### D. Running an evaluation
- **D1** Start a session by picking a class and a topic, or start a quick session with
  nothing prepared.
- **D2** For each question: present it, tap Scan, stop, and see the results.
- **D3** The scanner reads every visible card at once. It accepts only cards that belong
  to the class, and confirms a reading only when it stays stable for a few frames.
- **D4** Live overlay: a ✓ on each scanned card, a counter such as "18 / 25 answered", and
  the names still missing. Names and answers stay hidden on the overlay by default
  (toggle), so it's safe if the screen is shown on a TV.
- **D5** If a student changes their card, the latest reading wins. The teacher can also
  enter or correct an answer by hand.
- **D6** Results for each question: a bar chart of A–D, the % correct, and who chose what.
- **D7** Quick question: pick the number of options (or True/False) and scan, then tap the
  correct answer or mark the question as a poll.
- **D8** Re-scan or skip any question.
- **D9** The session is saved after every question and can be resumed if the app closes.
- **D10** Works with the phone in portrait or landscape. Optional beep or vibration when a
  card is recorded.

### E. Presenter screen
- **E1** A full-screen view in large text showing, in turn: the question, its picture and
  options; a live "18 of 25 answered" count with a ✓ next to each student already scanned
  (never their answers); and the results chart with the correct answer once the teacher
  taps Reveal.
- **E2** If a TV or projector is connected as a second display (HDMI adapter, or a wireless
  display the phone supports), the presenter view appears there automatically while the
  phone keeps the controls. Otherwise the teacher can mirror the phone's screen.

### F. Results and reports
- **F1** Session summary: class average, each student's score, and each question's
  % correct.
- **F2** Flags: students below 50% are marked "needs help", and questions below 60% correct
  are marked "re-teach". Both thresholds can be changed.
- **F3** Student profile: scores over time and the average per subject.
- **F4** Topic report across classes and sessions.
- **F5** Export reports as PDF and CSV and share them through Android's share sheet.
- **F6** Scoring: an unanswered question counts as not correct, absent students are
  excluded, and polls are never scored.

### G. Data, privacy and settings
- **G1** All data is stored on the phone, with no account or sign-in.
- **G2** The app asks only for camera permission and doesn't request internet permission.
  Student data leaves the phone only in files the teacher chooses to share. Camera frames
  are analysed in memory and never saved.
- **G3** Backup and restore of the whole database, pictures included, as one file.
- **G4** Settings: the two thresholds, card size and paper size, scan sound or vibration,
  and showing names while scanning.

## Non-functional requirements
- **N1** Android 8.0 (API 26) and later phones, including budget devices.
- **N2** Target: a class of 40 scanned in a few seconds from the front of a typical
  classroom (about 6 m) with standard cards.
- **N3** English interface with all text in resource files so other languages can be
  added later. Questions can be typed in any language.
- **N4** No ads, tracking or analytics.

## Additions after confirmation (3 October 2026)
- **H1** Navigation through a sliding side menu instead of a bottom bar. The menu lists
  every screen as a tree: classes (with their cards, evaluation and report), the question
  bank (subjects and their topics), reports, tools and settings.
- **H2** Compact layout: denser lists, smaller type and spacing.
- **H3** Theme option: system, light or dark, and a choice of accent colours (including
  wallpaper colours on Android 12+). Also switchable from the menu.
- **H4** Sample data for trying the app: Grade 2 and Grade 3 test classes with 10
  students each, and for each of the 4 subjects and each grade, two topics of 10
  multiple-choice questions (160 in all). Added and removed from Settings.
- **H5** Branding: the INTELZA name with the tagline "Understanding Beyond Answers" on the
  home screen, the presenter screen, PDF reports and Settings → About.

## Out of scope for 1.0
Cloud sync, multiple teachers, a head-teacher dashboard, iOS, OMR answer sheets, parent
reports, a translated interface, and importing questions from CSV.

## Milestones
1. Project setup and the AprilTag scanner reading card rotation
2. Classes, students and card printing
3. Question bank
4. Sessions, results, quick question and the presenter screen
5. Reports, export, backup and settings
6. Polish and testing on a real phone
