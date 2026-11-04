import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * FILE 2 of 2: GRAPHICS (Java Swing window).
 * Only builds the screen. All the real work is done by StudentCurriculumManager.java.
 */
public class CurriculumGUI extends JFrame {

    // ---------- colors and fonts (change here to restyle everything) ----------
    static final Color BLUE = new Color(30, 64, 120);
    static final Color STRIPE = new Color(244, 247, 252);
    static final Color SELECTED = new Color(190, 214, 247);
    static final Color RED = new Color(190, 0, 0);
    static final Color GREEN = new Color(0, 120, 0);
    static final Color ORANGE = new Color(200, 105, 0);
    static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);

    final StudentCurriculumManager data;

    // table models
    final DefaultTableModel subjectModel = newModel("Code", "Name", "Sem", "CH", "Max", "Marks", "Grade");
    final DefaultTableModel assignmentModel = newModel("Title", "Subject", "Status");
    final DefaultTableModel deadlineModel = newModel("Title", "Subject", "Due Date", "Status");

    // subject drop-downs (refilled whenever subjects change)
    final JComboBox<String> assignmentSubjectBox = new JComboBox<>();
    final JComboBox<String> deadlineSubjectBox = new JComboBox<>();

    // CGPA tab and status bar
    final JTextArea cgpaArea = new JTextArea();
    final JLabel cgpaLabel = new JLabel("CGPA: -", SwingConstants.CENTER);
    final JLabel statusBar = new JLabel(" ");

    JTabbedPane tabs;

    // ===== START (called from main) =====
    static void start(StudentCurriculumManager data, String loadError) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // default look and feel is fine
        }
        // one font for all common components
        String[] keys = {"Label.font", "Button.font", "TextField.font", "ComboBox.font",
                "TabbedPane.font", "TitledBorder.font", "Table.font", "OptionPane.messageFont",
                "OptionPane.buttonFont"};
        for (String key : keys) {
            UIManager.put(key, NORMAL);
        }
        SwingUtilities.invokeLater(() -> {
            CurriculumGUI window = new CurriculumGUI(data);
            window.setVisible(true);
            if (loadError != null) window.warn(loadError);
        });
    }

    // ===== WINDOW =====
    CurriculumGUI(StudentCurriculumManager data) {
        super("Student Curriculum Manager - BBSUTSD Khairpur Mirs");
        this.data = data;

        tabs = new JTabbedPane();
        tabs.addTab("1. Subjects", buildSubjectsPanel());
        tabs.addTab("2. Assignments", buildAssignmentsPanel());
        tabs.addTab("3. Deadlines", buildDeadlinesPanel());
        tabs.addTab("4. CGPA", buildCgpaPanel());
        // recalculate automatically when the CGPA tab is opened
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 3) showCgpa();
        });

        add(buildBanner(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        statusBar.setBorder(new EmptyBorder(5, 12, 5, 12));
        add(statusBar, BorderLayout.SOUTH);
        refreshAll();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);
    }

    /** Blue title strip at the top. */
    JPanel buildBanner() {
        JLabel title = new JLabel("Student Curriculum Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("BBSUTSD Khairpur Mirs  -  Subjects, Assignments, Deadlines and CGPA");
        sub.setFont(NORMAL);
        sub.setForeground(new Color(200, 215, 240));

        JPanel banner = new JPanel(new GridLayout(2, 1));
        banner.setBackground(BLUE);
        banner.setBorder(new EmptyBorder(10, 16, 10, 16));
        banner.add(title);
        banner.add(sub);
        return banner;
    }

    // ===== TAB 1: SUBJECTS =====
    JPanel buildSubjectsPanel() {
        JPanel panel = newTabPanel();

        JTextField nameField = new JTextField(14);
        JTextField codeField = new JTextField(7);
        JComboBox<Integer> semBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});
        JComboBox<Integer> chBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        chBox.setSelectedItem(3);
        JComboBox<Integer> maxBox = new JComboBox<>(new Integer[]{100, 50});
        JButton addBtn = new JButton("Add Subject");

        JPanel form = newForm("New Subject");
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Code:"));
        form.add(codeField);
        form.add(new JLabel("Semester:"));
        form.add(semBox);
        form.add(new JLabel("Credit Hours:"));
        form.add(chBox);
        form.add(new JLabel("Total Marks:"));
        form.add(maxBox);
        form.add(addBtn);

        JTable table = newTable(subjectModel, 6, 2);
        JButton marksBtn = new JButton("Enter / Update Marks");
        JButton deleteBtn = new JButton("Delete Selected");
        JPanel buttons = newButtonRow(marksBtn, deleteBtn);

        addBtn.addActionListener(e -> {
            String error = data.addSubject(nameField.getText(), codeField.getText(),
                    (Integer) semBox.getSelectedItem(),
                    (Integer) chBox.getSelectedItem(),
                    (Integer) maxBox.getSelectedItem());
            if (error != null) {
                warn(error);
                return;
            }
            nameField.setText("");
            codeField.setText("");
            saveAndRefresh();
        });

        marksBtn.addActionListener(e -> {
            int row = pickRow(table, "Select a subject from the table first.");
            if (row < 0) return;
            StudentCurriculumManager.Subject s = data.subjects.get(row);
            String old = "";
            if (s.marks != null) old = StudentCurriculumManager.fmt(s.marks);
            String input = JOptionPane.showInputDialog(this,
                    "Marks obtained in " + s.name + " (out of " + s.maxMarks + "):", old);
            if (input == null) return;   // cancelled
            String error = data.setMarks(s, input);
            if (error != null) {
                warn(error);
                return;
            }
            saveAndRefresh();
        });

        deleteBtn.addActionListener(e -> {
            int row = pickRow(table, "Select a subject from the table first.");
            if (row < 0) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete " + data.subjects.get(row).name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                data.subjects.remove(row);
                saveAndRefresh();
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ===== TAB 2: ASSIGNMENTS =====
    JPanel buildAssignmentsPanel() {
        JPanel panel = newTabPanel();

        JTextField titleField = new JTextField(22);
        JButton addBtn = new JButton("Add Assignment");

        JPanel form = newForm("New Assignment");
        form.add(new JLabel("Subject:"));
        form.add(assignmentSubjectBox);
        form.add(new JLabel("Title:"));
        form.add(titleField);
        form.add(addBtn);

        JTable table = newTable(assignmentModel, 2, 2);
        JButton toggleBtn = new JButton("Mark Done / Pending");
        JButton deleteBtn = new JButton("Delete Selected");
        JPanel buttons = newButtonRow(toggleBtn, deleteBtn);

        addBtn.addActionListener(e -> {
            String error = data.addAssignment((String) assignmentSubjectBox.getSelectedItem(),
                    titleField.getText());
            if (error != null) {
                warn(error);
                return;
            }
            titleField.setText("");
            saveAndRefresh();
        });

        toggleBtn.addActionListener(e -> {
            int row = pickRow(table, "Select an assignment first.");
            if (row < 0) return;
            StudentCurriculumManager.Assignment a = data.assignments.get(row);
            a.done = !a.done;
            saveAndRefresh();
        });

        deleteBtn.addActionListener(e -> {
            int row = pickRow(table, "Select an assignment first.");
            if (row < 0) return;
            data.assignments.remove(row);
            saveAndRefresh();
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ===== TAB 3: DEADLINES =====
    JPanel buildDeadlinesPanel() {
        JPanel panel = newTabPanel();

        JTextField titleField = new JTextField(18);
        JTextField dateField = new JTextField(9);
        dateField.setToolTipText("Format: DD-MM-YYYY");
        JButton addBtn = new JButton("Add Deadline");

        JPanel form = newForm("New Deadline");
        form.add(new JLabel("Subject:"));
        form.add(deadlineSubjectBox);
        form.add(new JLabel("Title:"));
        form.add(titleField);
        form.add(new JLabel("Due (DD-MM-YYYY):"));
        form.add(dateField);
        form.add(addBtn);

        JTable table = newTable(deadlineModel, 3, 2);
        JButton doneBtn = new JButton("Mark Completed");
        JButton deleteBtn = new JButton("Delete Selected");
        JPanel buttons = newButtonRow(doneBtn, deleteBtn);

        addBtn.addActionListener(e -> {
            String error = data.addDeadline((String) deadlineSubjectBox.getSelectedItem(),
                    titleField.getText(), dateField.getText());
            if (error != null) {
                warn(error);
                return;
            }
            titleField.setText("");
            dateField.setText("");
            saveAndRefresh();
        });

        doneBtn.addActionListener(e -> {
            int row = pickRow(table, "Select a deadline first.");
            if (row < 0) return;
            data.deadlines.get(row).done = true;
            saveAndRefresh();
        });

        deleteBtn.addActionListener(e -> {
            int row = pickRow(table, "Select a deadline first.");
            if (row < 0) return;
            data.deadlines.remove(row);
            saveAndRefresh();
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ===== TAB 4: CGPA =====
    JPanel buildCgpaPanel() {
        JPanel panel = newTabPanel();

        cgpaLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        cgpaLabel.setForeground(BLUE);
        cgpaArea.setEditable(false);
        cgpaArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        cgpaArea.setMargin(new Insets(8, 8, 8, 8));

        JButton calcBtn = new JButton("Calculate CGPA");
        calcBtn.addActionListener(e -> showCgpa());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(6, 6, 10, 6));
        top.add(cgpaLabel, BorderLayout.CENTER);
        top.add(calcBtn, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(cgpaArea), BorderLayout.CENTER);
        return panel;
    }

    void showCgpa() {
        double cgpa = data.getCgpa();
        if (cgpa < 0) {
            cgpaLabel.setText("CGPA: -");
        } else {
            cgpaLabel.setText(String.format("CGPA: %.2f", cgpa));
        }
        cgpaArea.setText(data.buildCgpaReport());
        cgpaArea.setCaretPosition(0);
    }

    // ===== REFRESH / SAVE =====
    void saveAndRefresh() {
        String error = data.save();
        if (error != null) warn(error);
        refreshAll();
    }

    void refreshAll() {
        data.sortDeadlines();

        subjectModel.setRowCount(0);
        for (StudentCurriculumManager.Subject s : data.subjects) {
            String marksText = "-";
            String gradeText = "-";
            if (s.marks != null) {
                marksText = StudentCurriculumManager.fmt(s.marks);
                gradeText = StudentCurriculumManager.letterOf(s.marks, s.maxMarks);
            }
            subjectModel.addRow(new Object[]{s.code, s.name, s.semester, s.creditHours,
                    s.maxMarks, marksText, gradeText});
        }

        assignmentModel.setRowCount(0);
        for (StudentCurriculumManager.Assignment a : data.assignments) {
            String status = "PENDING";
            if (a.done) status = "DONE";
            assignmentModel.addRow(new Object[]{a.title, a.subject, status});
        }

        deadlineModel.setRowCount(0);
        for (StudentCurriculumManager.Deadline d : data.deadlines) {
            deadlineModel.addRow(new Object[]{d.title, d.subject,
                    d.due.format(StudentCurriculumManager.DATE_FMT),
                    StudentCurriculumManager.deadlineStatus(d)});
        }

        fillSubjectBox(assignmentSubjectBox);
        fillSubjectBox(deadlineSubjectBox);
        statusBar.setText(data.summaryText());
        if (tabs.getSelectedIndex() == 3) showCgpa();
    }

    void fillSubjectBox(JComboBox<String> box) {
        Object selected = box.getSelectedItem();
        box.removeAllItems();
        for (StudentCurriculumManager.Subject s : data.subjects) {
            box.addItem(s.name);
        }
        if (selected != null) box.setSelectedItem(selected);
    }

    // ===== LOOK AND FEEL HELPERS =====
    static DefaultTableModel newModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;   // tables are read-only, edits go through buttons
            }
        };
    }

    JPanel newTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return panel;
    }

    JPanel newForm(String title) {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder(title));
        return form;
    }

    JPanel newButtonRow(JButton first, JButton second) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(first);
        buttons.add(second);
        return buttons;
    }

    /** statusCol = column that gets colored text, centerFrom = first centered column. */
    JTable newTable(DefaultTableModel model, int statusCol, int centerFrom) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new RowRenderer(statusCol, centerFrom));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /** Returns the selected table row, or -1 after showing a warning. */
    int pickRow(JTable table, String message) {
        int row = table.getSelectedRow();
        if (row < 0) warn(message);
        return row;
    }

    void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    // ===== TABLE RENDERERS (striped rows + colored status text) =====
    static class RowRenderer extends DefaultTableCellRenderer {
        int statusCol, centerFrom;

        RowRenderer(int statusCol, int centerFrom) {
            this.statusCol = statusCol;
            this.centerFrom = centerFrom;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setFont(NORMAL);
            setForeground(Color.BLACK);
            if (col >= centerFrom) setHorizontalAlignment(CENTER);
            else setHorizontalAlignment(LEFT);

            if (selected) setBackground(SELECTED);
            else if (row % 2 == 0) setBackground(Color.WHITE);
            else setBackground(STRIPE);

            if (col == statusCol) {
                String text = String.valueOf(value);
                setFont(BOLD);
                if (text.startsWith("OVERDUE") || text.equals("F")) setForeground(RED);
                else if (text.equals("DUE TODAY") || text.equals("PENDING")) setForeground(ORANGE);
                else if (text.equals("DONE")) setForeground(GREEN);
                else if (text.equals("Completed")) setForeground(Color.GRAY);
            }
            return this;
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            setBackground(BLUE);
            setForeground(Color.WHITE);
            setFont(BOLD);
            setBorder(new EmptyBorder(6, 8, 6, 8));
            return this;
        }
    }
}
