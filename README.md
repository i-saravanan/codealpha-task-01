# Student Grade Tracer

A premium, modern desktop Java application built using Java Swing. It features a custom dark-theme user interface inspired by the *Catppuccin* color palette, with advanced grading statistics, custom-rendered dynamic data visualizations, and full CSV file persistence.

---

## Key Features

- **Dynamic Stats Dashboard**:
  - **Class Average**: Color-coded dynamically based on the average score (Green: Good $\ge 80$, Yellow: Passing $60-80$, Red: Failing $< 60$).
  - **Highest Grade**: Instantly displays the highest average score and the name of the student who achieved it.
  - **Lowest Grade**: Instantly displays the lowest average score and the name of the student.
  - **Passing Rate**: Shows the percentage of students passing ($\ge 60$) along with exact count ratios.

- **Data Management Form**:
  - Allows inputs of Student Name and multiple grades (separated by commas, semicolons, or spaces).
  - Handles real-time input validation (restricts grades between $0$ and $100$, prevents duplicates, and throws user-friendly status bar warnings).
  - Clean CRUD actions: **Add Student**, **Update**, **Delete**, and **Clear Form**.

- **Interactive Table & Filtering**:
  - Display names, input grades, calculated averages, minimums, maximums, and final letter grades (A, B, C, D, F).
  - Real-time search bar to search/filter student records dynamically as you type.

- **Custom-drawn Visual Analytics**:
  - A beautiful, custom-rendered grade distribution bar chart depicting letter grade frequency.
  - Fully responsive layout that scales and redraws dynamically upon dataset changes.

- **Persistence Integration**:
  - **Import CSV / Export CSV**: Easy backup and restore of student grades to and from `.csv` files.
  - **Reset Data**: Safely wipe the current session database with confirmation checks.

---

## UI Color Theme Details
This application utilizes a modern, sleek charcoal/indigo theme layout:
- **Background**: `#1E1E2E`
- **Cards/Panels**: `#252538`
- **Primary Accent**: `#89B4FA` (Blue)
- **Success / Passing**: `#A6E3A1` (Green)
- **Warning**: `#F9E2AF` (Yellow)
- **Danger / Failing**: `#F38BA8` (Red)

---

## How to Run the Application

### Prerequisites
- **Java SE Development Kit (JDK)**: Version 11 or higher (fully tested and compiled on JDK 26).

### 1. Run Instantly (No Compile Step)
If you are using Java 11+, you can execute the single-file source code directly without creating intermediate class files:
```bash
java StudentGradeTracer.java
```

### 2. Compile and Run (Traditional)
```bash
javac StudentGradeTracer.java
java StudentGradeTracer
```

---

## CSV File Format Spec
When importing or exporting data via the CSV utility, the structure follows a standard format:

```csv
Student Name,Grades
Alice Smith,95.0;92.5;88.0
Bob Jones,75.0;60.0
Charlie Brown,55.0
```
- **Student Name**: Plain text name.
- **Grades**: Multi-grade lists separated by semicolons (`;`). Single grades are parsed directly.

---

## License
This project is open-source and available under the MIT License.
