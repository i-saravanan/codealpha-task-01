import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class StudentGradeTracer extends JFrame {

    // --- Modern Theme Color Palette (Catppuccin Mocha inspired) ---
    static final Color BG_DARK = new Color(30, 30, 46);       // Crust/Base
    static final Color CARD_DARK = new Color(37, 37, 56);     // Surface0
    static final Color ACCENT = new Color(137, 180, 250);     // Blue
    static final Color ACCENT_HOVER = new Color(166, 209, 255);
    static final Color ACCENT_PRESSED = new Color(110, 150, 230);
    static final Color FG_LIGHT = new Color(205, 214, 244);   // Text
    static final Color FG_MUTED = new Color(166, 173, 200);   // Subtext0
    static final Color SUCCESS = new Color(166, 227, 161);    // Green
    static final Color DANGER = new Color(243, 139, 168);     // Red
    static final Color WARNING = new Color(249, 226, 175);    // Yellow
    static final Color BORDER_COLOR = new Color(49, 50, 68);  // Surface1

    static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 15);
    static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    // --- State Variables ---
    private final List<Student> students = new ArrayList<>();
    private final List<Student> filteredStudents = new ArrayList<>();
    private Student selectedStudent = null;

    // --- GUI Components ---
    private ModernTextField txtName;
    private ModernTextField txtGrades;
    private ModernTextField txtSearch;
    private ModernButton btnAdd;
    private ModernButton btnUpdate;
    private ModernButton btnDelete;
    private ModernButton btnClear;
    private JTable studentTable;
    private StudentTableModel tableModel;
    private JLabel lblStatus;

    // --- Stats Cards ---
    private StatCard cardAverage;
    private StatCard cardHighest;
    private StatCard cardLowest;
    private StatCard cardTotal;

    // --- Visual Panel ---
    private GradeChartPanel chartPanel;

    // ==========================================
    // Student Data Model
    // ==========================================
    public static class Student {
        private String name;
        private final List<Double> grades = new ArrayList<>();

        public Student(String name, List<Double> grades) {
            this.name = name;
            setGrades(grades);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Double> getGrades() {
            return grades;
        }

        public void setGrades(List<Double> grades) {
            this.grades.clear();
            if (grades != null) {
                this.grades.addAll(grades);
            }
        }

        public double getAverage() {
            if (grades.isEmpty()) return 0.0;
            double sum = 0;
            for (double g : grades) {
                sum += g;
            }
            return sum / grades.size();
        }

        public double getHighest() {
            if (grades.isEmpty()) return 0.0;
            double max = grades.get(0);
            for (double g : grades) {
                if (g > max) max = g;
            }
            return max;
        }

        public double getLowest() {
            if (grades.isEmpty()) return 0.0;
            double min = grades.get(0);
            for (double g : grades) {
                if (g < min) min = g;
            }
            return min;
        }

        public String getGradesString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < grades.size(); i++) {
                sb.append(String.format(Locale.US, "%.1f", grades.get(i)));
                if (i < grades.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }

        public char getLetterGrade() {
            double avg = getAverage();
            if (avg >= 90) return 'A';
            if (avg >= 80) return 'B';
            if (avg >= 70) return 'C';
            if (avg >= 60) return 'D';
            return 'F';
        }
    }

    // ==========================================
    // Custom Swing Components
    // ==========================================

    public static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private final Color backgroundColor;

        public RoundedPanel(int radius, Color bg) {
            this.cornerRadius = radius;
            this.backgroundColor = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension arcs = new Dimension(cornerRadius, cornerRadius);
            int width = getWidth();
            int height = getHeight();
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(backgroundColor);
            graphics.fillRoundRect(0, 0, width, height, arcs.width, arcs.height);
        }
    }

    public static class StatCard extends RoundedPanel {
        private final JLabel lblTitle;
        private final JLabel lblValue;
        private final JLabel lblFooter;

        public StatCard(String title, String value, String footer) {
            super(16, CARD_DARK);
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            lblTitle = new JLabel(title);
            lblTitle.setFont(FONT_SMALL);
            lblTitle.setForeground(FG_MUTED);
            gbc.gridy = 0;
            gbc.insets = new Insets(0, 0, 5, 0);
            add(lblTitle, gbc);

            lblValue = new JLabel(value);
            lblValue.setFont(new Font("SansSerif", Font.BOLD, 24));
            lblValue.setForeground(ACCENT);
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 5, 0);
            add(lblValue, gbc);

            lblFooter = new JLabel(footer);
            lblFooter.setFont(FONT_SMALL);
            lblFooter.setForeground(FG_MUTED);
            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 0, 0);
            add(lblFooter, gbc);
        }

        public void updateCard(String value, String footer, Color valueColor) {
            lblValue.setText(value);
            lblValue.setForeground(valueColor);
            lblFooter.setText(footer);
            revalidate();
            repaint();
        }
    }

    public static class GradeChartPanel extends RoundedPanel {
        private final Map<String, Integer> counts = new TreeMap<>();

        public GradeChartPanel() {
            super(16, CARD_DARK);
            resetCounts();
        }

        private void resetCounts() {
            counts.put("A", 0);
            counts.put("B", 0);
            counts.put("C", 0);
            counts.put("D", 0);
            counts.put("F", 0);
        }

        public void updateData(List<Student> activeStudents) {
            resetCounts();
            for (Student s : activeStudents) {
                String grade = String.valueOf(s.getLetterGrade());
                counts.put(grade, counts.getOrDefault(grade, 0) + 1);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Label Title
            g2d.setColor(FG_LIGHT);
            g2d.setFont(FONT_SUBTITLE);
            g2d.drawString("Grade Distribution", 20, 30);

            // Chart boundaries
            int chartX = 45;
            int chartY = 60;
            int chartW = width - 80;
            int chartH = height - 120;

            String[] letters = {"A", "B", "C", "D", "F"};
            Color[] colors = {SUCCESS, ACCENT, WARNING, new Color(250, 179, 135), DANGER};

            // Calculate Max Value for scaling
            int maxVal = 0;
            for (int val : counts.values()) {
                if (val > maxVal) maxVal = val;
            }
            if (maxVal == 0) maxVal = 5; // Default max grid height if empty

            // Draw Y-axis grid lines and labels
            g2d.setColor(BORDER_COLOR);
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i <= 4; i++) {
                int y = chartY + chartH - (i * chartH / 4);
                g2d.drawLine(chartX, y, chartX + chartW, y);
                g2d.setFont(FONT_SMALL);
                g2d.setColor(FG_MUTED);
                String gridLabel = String.valueOf((i * maxVal) / 4);
                g2d.drawString(gridLabel, chartX - 25, y + 4);
            }

            // Draw Bars
            int numBars = letters.length;
            int gap = 20;
            int barWidth = (chartW - (gap * (numBars - 1))) / numBars;

            for (int i = 0; i < numBars; i++) {
                String grade = letters[i];
                int val = counts.getOrDefault(grade, 0);
                int barHeight = (int) (((double) val / maxVal) * chartH);

                int bx = chartX + i * (barWidth + gap);
                int by = chartY + chartH - barHeight;

                if (barHeight > 0) {
                    g2d.setColor(colors[i]);
                    // Rounded bar top
                    g2d.fillRoundRect(bx, by, barWidth, barHeight, 10, 10);
                    // Make the bottom square so it sits flat on the baseline
                    g2d.fillRect(bx, chartY + chartH - 5, barWidth, 5);

                    // Count Label above the bar
                    g2d.setColor(FG_LIGHT);
                    g2d.setFont(FONT_SMALL);
                    String valStr = String.valueOf(val);
                    int valWidth = g2d.getFontMetrics().stringWidth(valStr);
                    g2d.drawString(valStr, bx + (barWidth - valWidth) / 2, by - 6);
                }

                // X-axis letter labels
                g2d.setColor(FG_MUTED);
                g2d.setFont(FONT_SUBTITLE);
                int labelWidth = g2d.getFontMetrics().stringWidth(grade);
                g2d.drawString(grade, bx + (barWidth - labelWidth) / 2, chartY + chartH + 25);
            }
        }
    }

    public static class ModernButton extends JButton {
        private Color normalColor = ACCENT;
        private Color hoverColor = ACCENT_HOVER;
        private Color pressedColor = ACCENT_PRESSED;
        private Color currentBgColor = normalColor;
        private int radius = 10;

        public ModernButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(BG_DARK);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        currentBgColor = hoverColor;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (isEnabled()) {
                        currentBgColor = normalColor;
                        repaint();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        currentBgColor = pressedColor;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isEnabled()) {
                        currentBgColor = hoverColor;
                        repaint();
                    }
                }
            });
        }

        public void setColors(Color normal, Color hover, Color pressed, Color fg) {
            this.normalColor = normal;
            this.hoverColor = hover;
            this.pressedColor = pressed;
            this.currentBgColor = normal;
            setForeground(fg);
            repaint();
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            this.currentBgColor = enabled ? normalColor : BORDER_COLOR;
            setForeground(enabled ? (normalColor == BORDER_COLOR ? FG_LIGHT : BG_DARK) : FG_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(currentBgColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
        }
    }

    public static class ModernTextField extends JTextField {
        private final String placeholder;
        private final Color bgNormal = CARD_DARK;
        private final Color borderFocusColor = ACCENT;
        private final Color borderNormalColor = BORDER_COLOR;
        private boolean isFocused = false;
        private final int radius = 10;

        public ModernTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setForeground(FG_LIGHT);
            setCaretColor(FG_LIGHT);
            setFont(FONT_BODY);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Paint background
            g2d.setColor(bgNormal);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            // Paint border
            g2d.setColor(isFocused ? borderFocusColor : borderNormalColor);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            super.paintComponent(g);

            // Paint placeholder
            if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
                g2d.setColor(FG_MUTED);
                g2d.setFont(FONT_BODY);
                FontMetrics fm = g2d.getFontMetrics();
                int x = getInsets().left;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(placeholder, x, y);
            }
        }
    }

    // ==========================================
    // Table Models & Custom Rendering
    // ==========================================

    private static class StudentTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Student Name", "Grades", "Average", "Min", "Max", "Grade"};
        private final List<Student> studentList;

        public StudentTableModel(List<Student> list) {
            this.studentList = list;
        }

        @Override
        public int getRowCount() {
            return studentList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row < 0 || row >= studentList.size()) return null;
            Student s = studentList.get(row);
            switch (col) {
                case 0: return s.getName();
                case 1: return s.getGradesString();
                case 2: return String.format(Locale.US, "%.2f", s.getAverage());
                case 3: return String.format(Locale.US, "%.1f", s.getLowest());
                case 4: return String.format(Locale.US, "%.1f", s.getHighest());
                case 5: return String.valueOf(s.getLetterGrade());
                default: return null;
            }
        }
    }

    public static class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(FONT_BODY);

            if (isSelected) {
                c.setBackground(new Color(49, 50, 68)); // CATPPUCCIN SURFACE0
                c.setForeground(ACCENT);
            } else {
                c.setBackground(CARD_DARK);
                c.setForeground(FG_LIGHT);
            }

            // Text alignment
            if (column == 0) {
                setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            // Text styling based on grade
            if (column == 5) {
                String gradeStr = (String) value;
                if ("A".equals(gradeStr)) {
                    c.setForeground(SUCCESS);
                } else if ("F".equals(gradeStr)) {
                    c.setForeground(DANGER);
                } else if ("D".equals(gradeStr)) {
                    c.setForeground(WARNING);
                }
            }

            // Cell border padding
            if (c instanceof JLabel) {
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            }

            return c;
        }
    }

    public static class CustomTableHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer defaultRenderer;

        public CustomTableHeaderRenderer(TableCellRenderer defaultRenderer) {
            this.defaultRenderer = defaultRenderer;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(new Font("SansSerif", Font.BOLD, 12));
            c.setBackground(BORDER_COLOR);
            c.setForeground(FG_LIGHT);

            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            }
            return c;
        }
    }

    // ==========================================
    // Main Frame Construction
    // ==========================================

    public StudentGradeTracer() {
        setTitle("Student Grade Tracer");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(15, 15));

        // Set layout gap border around window
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Initialize UI Elements
        initHeaderStatsPanel();
        initSidebarForm();
        initCenterTablePanel();
        initRightChartPanel();
        initStatusPanel();

        // Load empty dataset initial refresh
        refreshData();
    }

    // ------------------------------------------
    // Header Stats Panel
    // ------------------------------------------
    private void initHeaderStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 15));
        statsPanel.setOpaque(false);

        cardAverage = new StatCard("CLASS AVERAGE", "0.00", "0 students registered");
        cardHighest = new StatCard("HIGHEST GRADE", "N/A", "No data");
        cardLowest = new StatCard("LOWEST GRADE", "N/A", "No data");
        cardTotal = new StatCard("PASSING RATE", "0.0%", "0 students passing (>= 60%)");

        statsPanel.add(cardAverage);
        statsPanel.add(cardHighest);
        statsPanel.add(cardLowest);
        statsPanel.add(cardTotal);

        add(statsPanel, BorderLayout.NORTH);
    }

    // ------------------------------------------
    // Sidebar Form (Input Details)
    // ------------------------------------------
    private void initSidebarForm() {
        RoundedPanel formPanel = new RoundedPanel(16, CARD_DARK);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.setPreferredSize(new Dimension(320, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Title
        JLabel lblTitle = new JLabel("Manage Students");
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(FG_LIGHT);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(lblTitle, gbc);

        // Name Field Label
        JLabel lblName = new JLabel("Student Name");
        lblName.setFont(FONT_SUBTITLE);
        lblName.setForeground(FG_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 5, 0);
        formPanel.add(lblName, gbc);

        // Name Field
        txtName = new ModernTextField("e.g. Alice Smith");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        formPanel.add(txtName, gbc);

        // Grades Field Label
        JLabel lblGrades = new JLabel("Grades (comma separated)");
        lblGrades.setFont(FONT_SUBTITLE);
        lblGrades.setForeground(FG_MUTED);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 5, 0);
        formPanel.add(lblGrades, gbc);

        // Grades Field
        txtGrades = new ModernTextField("e.g. 85, 92.5, 78");
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 25, 0);
        formPanel.add(txtGrades, gbc);

        // Buttons Panel (2x2 Grid)
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        btnPanel.setOpaque(false);

        btnAdd = new ModernButton("Add Student");
        btnAdd.addActionListener(e -> addStudentAction());

        btnUpdate = new ModernButton("Update");
        btnUpdate.setEnabled(false);
        btnUpdate.setColors(BORDER_COLOR, new Color(69, 71, 90), BORDER_COLOR, FG_LIGHT);
        btnUpdate.addActionListener(e -> updateStudentAction());

        btnDelete = new ModernButton("Delete");
        btnDelete.setEnabled(false);
        btnDelete.setColors(DANGER.darker(), DANGER, DANGER.darker(), FG_LIGHT);
        btnDelete.addActionListener(e -> deleteStudentAction());

        btnClear = new ModernButton("Clear Form");
        btnClear.setColors(BORDER_COLOR, new Color(69, 71, 90), BORDER_COLOR, FG_LIGHT);
        btnClear.addActionListener(e -> clearFormFields());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(btnPanel, gbc);

        // Spacer to push persistence buttons to bottom
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        formPanel.add(Box.createVerticalGlue(), gbc);

        // File Operations Divider
        JSeparator divider = new JSeparator();
        divider.setForeground(BORDER_COLOR);
        divider.setBackground(BORDER_COLOR);
        gbc.gridy = 7;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(10, 0, 15, 0);
        formPanel.add(divider, gbc);

        // Import/Export CSV Panel
        JPanel filePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        filePanel.setOpaque(false);

        ModernButton btnImport = new ModernButton("Import CSV");
        btnImport.setColors(BORDER_COLOR, new Color(69, 71, 90), BORDER_COLOR, FG_LIGHT);
        btnImport.addActionListener(e -> importCSVAction());

        ModernButton btnExport = new ModernButton("Export CSV");
        btnExport.setColors(BORDER_COLOR, new Color(69, 71, 90), BORDER_COLOR, FG_LIGHT);
        btnExport.addActionListener(e -> exportCSVAction());

        filePanel.add(btnImport);
        filePanel.add(btnExport);

        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 10, 0);
        formPanel.add(filePanel, gbc);

        // Reset Database Button
        ModernButton btnReset = new ModernButton("Reset Data");
        btnReset.setColors(DANGER.darker(), DANGER, DANGER.darker(), FG_LIGHT);
        btnReset.addActionListener(e -> resetDataAction());
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(btnReset, gbc);

        add(formPanel, BorderLayout.WEST);
    }

    // ------------------------------------------
    // Center Panel (Search & Table)
    // ------------------------------------------
    private void initCenterTablePanel() {
        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setOpaque(false);

        // Search Bar Panel
        RoundedPanel searchPanel = new RoundedPanel(12, CARD_DARK);
        searchPanel.setLayout(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        txtSearch = new ModernTextField("Search students by name...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterStudents(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterStudents(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterStudents(); }
        });
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        container.add(searchPanel, BorderLayout.NORTH);

        // Table
        tableModel = new StudentTableModel(filteredStudents);
        studentTable = new JTable(tableModel);
        studentTable.setBackground(CARD_DARK);
        studentTable.setForeground(FG_LIGHT);
        studentTable.setRowHeight(38);
        studentTable.setGridColor(BORDER_COLOR);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.getTableHeader().setReorderingAllowed(false);

        // Custom renderers
        studentTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
        studentTable.getTableHeader().setDefaultRenderer(new CustomTableHeaderRenderer(studentTable.getTableHeader().getDefaultRenderer()));

        // Scrollpane
        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.getViewport().setBackground(CARD_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setOpaque(false);

        // Handle Row Selection
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = studentTable.getSelectedRow();
                if (row >= 0 && row < filteredStudents.size()) {
                    selectedStudent = filteredStudents.get(row);
                    txtName.setText(selectedStudent.getName());
                    txtGrades.setText(selectedStudent.getGradesString());
                    btnAdd.setEnabled(false);
                    btnUpdate.setEnabled(true);
                    btnDelete.setEnabled(true);
                }
            }
        });

        container.add(scrollPane, BorderLayout.CENTER);
        add(container, BorderLayout.CENTER);
    }

    // ------------------------------------------
    // Right Chart Panel
    // ------------------------------------------
    private void initRightChartPanel() {
        chartPanel = new GradeChartPanel();
        chartPanel.setPreferredSize(new Dimension(380, 0));
        add(chartPanel, BorderLayout.EAST);
    }

    // ------------------------------------------
    // Status Bar
    // ------------------------------------------
    private void initStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        lblStatus = new JLabel("System Ready.");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(FG_MUTED);

        statusPanel.add(lblStatus, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    // ==========================================
    // Application Actions & Logic
    // ==========================================

    private void refreshData() {
        filterStudents();
        updateStats();
        chartPanel.updateData(filteredStudents);
    }

    private void filterStudents() {
        filteredStudents.clear();
        String query = txtSearch.getText().trim().toLowerCase();
        // Clear default placeholder string if matching
        if (query.equals("search students by name...")) {
            query = "";
        }

        for (Student s : students) {
            if (query.isEmpty() || s.getName().toLowerCase().contains(query)) {
                filteredStudents.add(s);
            }
        }
        tableModel.fireTableDataChanged();
    }

    private void updateStats() {
        int total = filteredStudents.size();
        if (total == 0) {
            cardAverage.updateCard("0.00", "0 students registered", ACCENT);
            cardHighest.updateCard("N/A", "No data available", FG_MUTED);
            cardLowest.updateCard("N/A", "No data available", FG_MUTED);
            cardTotal.updateCard("0.0%", "0 passing students", FG_MUTED);
            return;
        }

        double sum = 0.0;
        double max = -1.0;
        double min = 101.0;
        Student maxStud = null;
        Student minStud = null;
        int passCount = 0;

        for (Student s : filteredStudents) {
            double avg = s.getAverage();
            sum += avg;

            if (avg >= 60.0) {
                passCount++;
            }

            if (avg > max) {
                max = avg;
                maxStud = s;
            }
            if (avg < min) {
                min = avg;
                minStud = s;
            }
        }

        double classAvg = sum / total;
        double passRate = ((double) passCount / total) * 100.0;

        // Choose class color for average
        Color avgColor = SUCCESS;
        if (classAvg < 60.0) avgColor = DANGER;
        else if (classAvg < 80.0) avgColor = WARNING;

        cardAverage.updateCard(
                String.format(Locale.US, "%.2f", classAvg),
                String.format("%d active students", total),
                avgColor
        );

        if (maxStud != null) {
            cardHighest.updateCard(
                    String.format(Locale.US, "%.1f", max),
                    maxStud.getName(),
                    SUCCESS
            );
        } else {
            cardHighest.updateCard("N/A", "No data", FG_MUTED);
        }

        if (minStud != null) {
            cardLowest.updateCard(
                    String.format(Locale.US, "%.1f", min),
                    minStud.getName(),
                    min < 60 ? DANGER : WARNING
            );
        } else {
            cardLowest.updateCard("N/A", "No data", FG_MUTED);
        }

        Color passColor = passRate >= 75.0 ? SUCCESS : (passRate >= 50.0 ? WARNING : DANGER);
        cardTotal.updateCard(
                String.format(Locale.US, "%.1f%%", passRate),
                String.format("%d passing (>= 60)", passCount),
                passColor
        );
    }

    private void setStatus(String message, Color color) {
        lblStatus.setText(message);
        lblStatus.setForeground(color);
    }

    private void clearFormFields() {
        txtName.setText("");
        txtGrades.setText("");
        selectedStudent = null;
        studentTable.clearSelection();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        setStatus("Form cleared.", FG_MUTED);
    }

    private List<Double> parseGrades(String gradesStr) throws IllegalArgumentException {
        List<Double> list = new ArrayList<>();
        if (gradesStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Grade field cannot be empty.");
        }
        String[] tokens = gradesStr.split("[,;\\s]+");
        for (String t : tokens) {
            if (t.trim().isEmpty()) continue;
            try {
                double g = Double.parseDouble(t.trim());
                if (g < 0 || g > 100) {
                    throw new IllegalArgumentException("Grades must be between 0 and 100.");
                }
                list.add(g);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid grade value: " + t);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("No valid grades entered.");
        }
        return list;
    }

    private void addStudentAction() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            setStatus("Error: Student name cannot be empty.", DANGER);
            return;
        }

        // Prevent duplicate names
        for (Student s : students) {
            if (s.getName().equalsIgnoreCase(name)) {
                setStatus("Error: Student '" + name + "' already exists.", DANGER);
                return;
            }
        }

        try {
            List<Double> grades = parseGrades(txtGrades.getText());
            Student s = new Student(name, grades);
            students.add(s);
            refreshData();
            clearFormFields();
            setStatus("Student '" + name + "' added successfully.", SUCCESS);
        } catch (IllegalArgumentException ex) {
            setStatus("Error: " + ex.getMessage(), DANGER);
        }
    }

    private void updateStudentAction() {
        if (selectedStudent == null) return;
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            setStatus("Error: Student name cannot be empty.", DANGER);
            return;
        }

        // Prevent duplicate names excluding self
        for (Student s : students) {
            if (s != selectedStudent && s.getName().equalsIgnoreCase(name)) {
                setStatus("Error: Student '" + name + "' already exists.", DANGER);
                return;
            }
        }

        try {
            List<Double> grades = parseGrades(txtGrades.getText());
            String oldName = selectedStudent.getName();
            selectedStudent.setName(name);
            selectedStudent.setGrades(grades);
            refreshData();
            clearFormFields();
            setStatus("Student '" + oldName + "' updated to '" + name + "' successfully.", SUCCESS);
        } catch (IllegalArgumentException ex) {
            setStatus("Error: " + ex.getMessage(), DANGER);
        }
    }

    private void deleteStudentAction() {
        if (selectedStudent == null) return;
        String name = selectedStudent.getName();
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete student '" + name + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            students.remove(selectedStudent);
            refreshData();
            clearFormFields();
            setStatus("Student '" + name + "' deleted.", SUCCESS);
        }
    }

    private void resetDataAction() {
        if (students.isEmpty()) return;
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to erase all student data?",
                "Reset Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            students.clear();
            refreshData();
            clearFormFields();
            setStatus("All student records cleared.", DANGER);
        }
    }

    private void importCSVAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Student Data (CSV)");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            int importedCount = 0;
            int errorCount = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.toLowerCase().startsWith("student,") || line.toLowerCase().startsWith("name,")) {
                        continue; // Skip headers or empty lines
                    }
                    try {
                        String[] parts = line.split(",", 2);
                        if (parts.length < 2) continue;
                        String name = parts[0].trim();
                        if (name.isEmpty()) continue;

                        // Parse grades inside parts[1] (could be semicolon-separated or whitespace)
                        List<Double> gradesList = parseGrades(parts[1]);

                        // Check if student exists. If so, replace grades. If not, add new
                        Student existing = null;
                        for (Student s : students) {
                            if (s.getName().equalsIgnoreCase(name)) {
                                existing = s;
                                break;
                            }
                        }
                        if (existing != null) {
                            existing.setGrades(gradesList);
                        } else {
                            students.add(new Student(name, gradesList));
                        }
                        importedCount++;
                    } catch (Exception ex) {
                        errorCount++;
                    }
                }
                refreshData();
                setStatus("Imported " + importedCount + " students. Errors: " + errorCount, SUCCESS);
            } catch (IOException e) {
                setStatus("Error reading file: " + e.getMessage(), DANGER);
            }
        }
    }

    private void exportCSVAction() {
        if (students.isEmpty()) {
            setStatus("No data to export.", WARNING);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("student_grades.csv"));
        chooser.setDialogTitle("Export Student Data (CSV)");
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("Student Name,Grades");
                for (Student s : students) {
                    // Save grades separated by semicolons
                    pw.println(s.getName() + "," + s.getGradesString().replace(", ", ";"));
                }
                setStatus("Data exported successfully to " + file.getName(), SUCCESS);
            } catch (IOException e) {
                setStatus("Error saving file: " + e.getMessage(), DANGER);
            }
        }
    }

    // ==========================================
    // Application Entry Point
    // ==========================================
    public static void main(String[] args) {
        // Set system properties for HighDPI rendering on Windows
        System.setProperty("sun.java2d.uiScale", "1.0");

        try {
            // Apply a default clean cross-platform Look and Feel
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            StudentGradeTracer app = new StudentGradeTracer();
            app.setVisible(true);
        });
    }
}
