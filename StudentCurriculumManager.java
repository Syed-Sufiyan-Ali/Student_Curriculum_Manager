import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * FILE 1 of 2: MAIN + LOGIC (no Swing code here). The window is in CurriculumGUI.java.
 * Run:  javac StudentCurriculumManager.java CurriculumGUI.java
 *       java StudentCurriculumManager
 * Data is saved in curriculum_data.txt. Methods that can fail return an error
 * message (String), or null if everything went fine.
 */
public class StudentCurriculumManager {

    // ===== DATA CLASSES =====
    static class Subject {
        String name, code;
        int semester, creditHours, maxMarks;   // maxMarks = 100 or 50
        Double marks;                          // null = not entered yet

        Subject(String name, String code, int semester, int creditHours, int maxMarks, Double marks) {
            this.name = name;
            this.code = code;
            this.semester = semester;
            this.creditHours = creditHours;
            this.maxMarks = maxMarks;
            this.marks = marks;
        }
    }

    static class Assignment {
        String title, subject;
        boolean done;

        Assignment(String title, String subject, boolean done) {
            this.title = title;
            this.subject = subject;
            this.done = done;
        }
    }

    static class Deadline {
        String title, subject;
        LocalDate due;
        boolean done;

        Deadline(String title, String subject, LocalDate due, boolean done) {
            this.title = title;
            this.subject = subject;
            this.due = due;
            this.done = done;
        }
    }

    // ===== BBSUTSD GRADING POLICY (Absolute Grading)  Minimum marks for A+, A, B+, B, C+, C. Below C = F (0.00) =====
    static final int[] LIMITS_100 = {85, 75, 66, 60, 55, 50};
    static final int[] LIMITS_50  = {42, 37, 33, 30, 27, 25};
    static final String[] LETTERS = {"A+", "A", "B+", "B", "C+", "C"};
    static final double[] POINTS  = {4.00, 3.75, 3.50, 3.00, 2.50, 2.00};

    /** Returns index into LETTERS/POINTS, or -1 for F. */
    static int gradeIndex(double marks, int maxMarks) {
        int[] limits = (maxMarks == 100) ? LIMITS_100 : LIMITS_50;
        for (int i = 0; i < limits.length; i++) {
            if (marks >= limits[i]) return i;
        }
        return -1;
    }

    static String letterOf(double marks, int maxMarks) {
        int i = gradeIndex(marks, maxMarks);
        if (i < 0) return "F";
        return LETTERS[i];
    }

    static double pointOf(double marks, int maxMarks) {
        int i = gradeIndex(marks, maxMarks);
        if (i < 0) return 0.0;
        return POINTS[i];
    }

    // ===== STATE =====
    static final Path DATA_FILE = Path.of("curriculum_data.txt");
    static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    final List<Subject> subjects = new ArrayList<>();
    final List<Assignment> assignments = new ArrayList<>();
    final List<Deadline> deadlines = new ArrayList<>();

    // ===== 1. SUBJECTS =====
    String addSubject(String name, String code, int semester, int creditHours, int maxMarks) {
        name = clean(name);
        code = clean(code);
        if (name.isEmpty() || code.isEmpty()) {
            return "Please enter subject name and course code.";
        }
        for (Subject s : subjects) {
            if (s.name.equalsIgnoreCase(name)) return "A subject with this name already exists.";
            if (s.code.equalsIgnoreCase(code)) return "A subject with this code already exists.";
        }
        subjects.add(new Subject(name, code, semester, creditHours, maxMarks, null));
        return null;
    }

    String setMarks(Subject s, String input) {
        try {
            double m = Double.parseDouble(input.trim());
            if (m < 0 || m > s.maxMarks) throw new NumberFormatException();
            s.marks = m;
            return null;
        } catch (NumberFormatException ex) {
            return "Enter a valid number between 0 and " + s.maxMarks + ".";
        }
    }

    // ===== 2. ASSIGNMENTS =====
    String addAssignment(String subject, String title) {
        title = clean(title);
        if (subject == null) return "Add a subject first (Subjects tab).";
        if (title.isEmpty()) return "Please enter assignment title.";
        assignments.add(new Assignment(title, subject, false));
        return null;
    }

    // ===== 3. DEADLINES =====
    String addDeadline(String subject, String title, String dateText) {
        title = clean(title);
        if (subject == null) return "Add a subject first (Subjects tab).";
        if (title.isEmpty()) return "Please enter deadline title.";
        try {
            LocalDate due = LocalDate.parse(dateText.trim(), DATE_FMT);
            deadlines.add(new Deadline(title, subject, due, false));
            return null;
        } catch (DateTimeParseException ex) {
            return "Invalid date. Use DD-MM-YYYY, e.g. 25-12-2026.";
        }
    }

    void sortDeadlines() {
        deadlines.sort(Comparator.comparing(d -> d.due));   // nearest first
    }

    static String deadlineStatus(Deadline d) {
        if (d.done) return "Completed";
        long left = ChronoUnit.DAYS.between(LocalDate.now(), d.due);
        if (left < 0) return "OVERDUE by " + (-left) + " day(s)";
        if (left == 0) return "DUE TODAY";
        return left + " day(s) left";
    }

    // ===== 4. CGPA   (GPA = sum(grade point x credit hours) / sum(credit hours)) =====

    /** Returns the overall CGPA, or -1 if no marks are entered yet. */
    double getCgpa() {
        double totalPoints = 0;
        int totalCh = 0;
        for (Subject s : subjects) {
            if (s.marks != null) {
                totalPoints += pointOf(s.marks, s.maxMarks) * s.creditHours;
                totalCh += s.creditHours;
            }
        }
        if (totalCh == 0) return -1;
        return totalPoints / totalCh;
    }

    /** Text report shown in the CGPA tab (semester by semester). */
    String buildCgpaReport() {
        Map<Integer, List<Subject>> bySemester = new TreeMap<>();
        for (Subject s : subjects) {
            if (s.marks != null) {
                if (!bySemester.containsKey(s.semester)) {
                    bySemester.put(s.semester, new ArrayList<>());
                }
                bySemester.get(s.semester).add(s);
            }
        }
        if (bySemester.isEmpty()) {
            return "No marks entered yet.\nAdd marks from the Subjects tab.";
        }

        StringBuilder sb = new StringBuilder();
        double totalPoints = 0;
        int totalCh = 0;
        boolean hasFail = false;

        for (Map.Entry<Integer, List<Subject>> entry : bySemester.entrySet()) {
            sb.append("--- Semester ").append(entry.getKey()).append(" ---\n");
            double semPoints = 0;
            int semCh = 0;
            for (Subject s : entry.getValue()) {
                String letter = letterOf(s.marks, s.maxMarks);
                double gp = pointOf(s.marks, s.maxMarks);
                if (letter.equals("F")) hasFail = true;
                sb.append(String.format("%-9s %-24s %6s/%-4d %-3s GP %.2f x CH %d%n",
                        s.code, s.name, fmt(s.marks), s.maxMarks, letter, gp, s.creditHours));
                semPoints += gp * s.creditHours;
                semCh += s.creditHours;
            }
            sb.append(String.format("  Semester %d GPA = %.2f  (%d credit hours)%n%n",
                    entry.getKey(), semPoints / semCh, semCh));
            totalPoints += semPoints;
            totalCh += semCh;
        }

        sb.append(String.format("OVERALL CGPA = %.2f  (total credit hours: %d)%n", totalPoints / totalCh, totalCh));
        if (hasFail) sb.append("Note: failed (F) subjects are counted with 0.00 grade points.\n");
        return sb.toString();
    }

    /** One-line summary for the status bar at the bottom of the window. */
    String summaryText() {
        int pendingAssignments = 0;
        for (Assignment a : assignments) {
            if (!a.done) pendingAssignments++;
        }
        int pendingDeadlines = 0;
        int overdue = 0;
        for (Deadline d : deadlines) {
            if (!d.done) {
                pendingDeadlines++;
                if (d.due.isBefore(LocalDate.now())) overdue++;
            }
        }
        return "Subjects: " + subjects.size()
                + "   |   Pending assignments: " + pendingAssignments
                + "   |   Pending deadlines: " + pendingDeadlines + " (" + overdue + " overdue)";
    }

    // ===== SAVE / LOAD  File format (one record per line, fields separated by | ):  S|name|code|semester|ch|max|marks(or -)  A|title|subject|done  D|title|subject|yyyy-mm-dd|done =====
    String save() {
        List<String> lines = new ArrayList<>();
        for (Subject s : subjects) {
            String marksText = "-";
            if (s.marks != null) marksText = String.valueOf(s.marks);
            lines.add(String.join("|", "S", s.name, s.code, String.valueOf(s.semester),
                    String.valueOf(s.creditHours), String.valueOf(s.maxMarks), marksText));
        }
        for (Assignment a : assignments) {
            lines.add(String.join("|", "A", a.title, a.subject, String.valueOf(a.done)));
        }
        for (Deadline d : deadlines) {
            lines.add(String.join("|", "D", d.title, d.subject, d.due.toString(), String.valueOf(d.done)));
        }
        try {
            Files.write(DATA_FILE, lines);
            return null;
        } catch (IOException ex) {
            return "Could not save data: " + ex.getMessage();
        }
    }

    String load() {
        if (!Files.exists(DATA_FILE)) return null;
        try {
            for (String line : Files.readAllLines(DATA_FILE)) {
                if (line.isBlank()) continue;
                String[] p = line.split("\\|", -1);
                if (p[0].equals("S")) {
                    Double marks = null;
                    if (!p[6].equals("-")) marks = Double.valueOf(p[6]);
                    subjects.add(new Subject(p[1], p[2], Integer.parseInt(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5]), marks));
                } else if (p[0].equals("A")) {
                    assignments.add(new Assignment(p[1], p[2], Boolean.parseBoolean(p[3])));
                } else if (p[0].equals("D")) {
                    deadlines.add(new Deadline(p[1], p[2], LocalDate.parse(p[3]),
                            Boolean.parseBoolean(p[4])));
                }
            }
            return null;
        } catch (Exception ex) {
            return "Saved data file could not be read completely: " + ex.getMessage();
        }
    }

    // ===== SMALL HELPERS =====

    /** '|' and line breaks would break the save file format. */
    static String clean(String text) {
        return text.replace("|", "/").replace("\n", " ").trim();
    }

    /** 80.0 -> "80", 72.5 -> "72.5" */
    static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    // ===== MAIN =====
    public static void main(String[] args) {
        StudentCurriculumManager data = new StudentCurriculumManager();
        String loadError = data.load();
        CurriculumGUI.start(data, loadError);
    }
}
