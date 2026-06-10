/*
 * Task Manager - Swing Desktop App
 *
 * How to run in VS Code:
 * 1. Save this file as src/TaskManager.java.
 * 2. Install the "Extension Pack for Java" in VS Code if you have not already.
 * 3. Open this project folder in VS Code.
 * 4. Click "Run" above the main method, or use:
 *      javac src/TaskManager.java
 *      java -cp src TaskManager
 *
 * Tasks are saved automatically to tasks.txt in the project folder.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

public class TaskManager extends JFrame {
    private static final Path TASK_FILE = Path.of("tasks.txt");
    private static final String DONE_PREFIX = "[DONE] ";

    private static final String APP_TITLE = "Task Manager";
    private static final String FONT_FAMILY = "Segoe UI";

    private static final Color APP_BACKGROUND = new Color(245, 247, 250);
    private static final Color BORDER_COLOR = new Color(215, 221, 230);
    private static final Color TEXT_COLOR = new Color(35, 43, 53);
    private static final Color MUTED_TEXT_COLOR = new Color(70, 78, 90);
    private static final Color COMPLETED_TEXT_COLOR = new Color(95, 125, 105);
    private static final Color COMPLETED_BACKGROUND = new Color(232, 246, 238);

    private static final Dimension WINDOW_MINIMUM_SIZE = new Dimension(680, 520);
    private static final Dimension BUTTON_SIZE = new Dimension(140, 48);
    private static final EmptyBorder PAGE_PADDING = new EmptyBorder(16, 16, 16, 16);
    private static final EmptyBorder BUTTON_PADDING = new EmptyBorder(12, 14, 12, 14);

    private final DefaultListModel<Task> taskModel = new DefaultListModel<>();
    private final JList<Task> taskList = new JList<>(taskModel);
    private final JTextField taskInput = new JTextField();
    private final JLabel statusLabel = new JLabel();

    public TaskManager() {
        super(APP_TITLE);

        configureLookAndFeel();
        configureWindow();
        buildInterface();
        loadTasks();
        refreshStatus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskManager().setVisible(true));
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println("Could not apply system look and feel: " + exception.getMessage());
        }
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(WINDOW_MINIMUM_SIZE);
        setLocationRelativeTo(null);
    }

    private void buildInterface() {
        JPanel rootPanel = createRootPanel();
        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        rootPanel.add(createTaskScrollPane(), BorderLayout.CENTER);
        rootPanel.add(createInputPanel(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
        pack();
    }

    private JPanel createRootPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(PAGE_PADDING);
        panel.setBackground(APP_BACKGROUND);
        return panel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(createTitleLabel(), BorderLayout.WEST);
        panel.add(createStatusPanel(), BorderLayout.EAST);
        return panel;
    }

    private JLabel createTitleLabel() {
        JLabel label = new JLabel(APP_TITLE);
        label.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        label.setForeground(TEXT_COLOR);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        return label;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        panel.setOpaque(false);

        statusLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        statusLabel.setForeground(MUTED_TEXT_COLOR);
        panel.add(statusLabel);

        return panel;
    }

    private JScrollPane createTaskScrollPane() {
        configureTaskList();

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        return scrollPane;
    }

    private void configureTaskList() {
        taskList.setFont(new Font(FONT_FAMILY, Font.PLAIN, 16));
        taskList.setFixedCellHeight(36);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setBorder(new EmptyBorder(6, 6, 6, 6));
        taskList.setCellRenderer(new TaskCellRenderer());
        taskList.addMouseListener(createTaskMouseListener());
        taskList.addKeyListener(createTaskKeyListener());
    }

    private MouseAdapter createTaskMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    editSelectedTask();
                }
            }
        };
    }

    private KeyAdapter createTaskKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedTask();
                } else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    toggleSelectedTaskCompletion();
                }
            }
        };
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.add(createTaskInput(), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JTextField createTaskInput() {
        taskInput.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        taskInput.setToolTipText("Enter a new task");
        taskInput.addActionListener(this::handleAddTask);
        return taskInput;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 10));
        panel.setOpaque(false);

        for (ButtonSpec button : createButtonSpecs()) {
            panel.add(createButton(button));
        }

        panel.add(createSpacerButton());
        return panel;
    }

    private List<ButtonSpec> createButtonSpecs() {
        return List.of(
                new ButtonSpec("Add Task", new Color(0, 102, 204), this::handleAddTask),
                new ButtonSpec("Edit Task", new Color(75, 85, 99), event -> editSelectedTask()),
                new ButtonSpec("Toggle Done", new Color(22, 128, 86), event -> toggleSelectedTaskCompletion()),
                new ButtonSpec("Clear Done", new Color(197, 96, 20), event -> clearCompletedTasks()),
                new ButtonSpec("Move Up", new Color(106, 76, 170), event -> moveSelectedTask(-1)),
                new ButtonSpec("Move Down", new Color(106, 76, 170), event -> moveSelectedTask(1)),
                new ButtonSpec("Delete Task", new Color(190, 50, 50), event -> deleteSelectedTask()));
    }

    private JButton createButton(ButtonSpec spec) {
        JButton button = new JButton(spec.text());
        Color background = spec.background();

        button.setUI(new BasicButtonUI());
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setMargin(new Insets(12, 14, 12, 14));
        button.setPreferredSize(BUTTON_SIZE);
        button.setBorder(createButtonBorder(background));
        button.addMouseListener(createButtonHoverListener(button, background));
        button.addActionListener(spec.action());

        return button;
    }

    private javax.swing.border.Border createButtonBorder(Color background) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 2),
                BUTTON_PADDING);
    }

    private MouseAdapter createButtonHoverListener(JButton button, Color normalBackground) {
        Color hoverBackground = normalBackground.brighter();

        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(hoverBackground);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(normalBackground);
            }
        };
    }

    private JButton createSpacerButton() {
        JButton button = new JButton();
        button.setEnabled(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        return button;
    }

    private void handleAddTask(ActionEvent event) {
        addTask(taskInput.getText());
    }

    private void addTask(String taskText) {
        String cleanTaskText = taskText.trim();

        if (cleanTaskText.isEmpty()) {
            showWarning("Please enter a task before adding it.");
            taskInput.requestFocusInWindow();
            return;
        }

        taskModel.addElement(new Task(cleanTaskText, false));
        taskInput.setText("");
        taskInput.requestFocusInWindow();
        saveAndRefresh();
    }

    private void editSelectedTask() {
        int selectedIndex = getSelectedTaskIndex("Please select a task to edit.");
        if (selectedIndex == -1) {
            return;
        }

        Task selectedTask = taskModel.get(selectedIndex);
        String updatedText = promptForTaskText(selectedTask.text());

        if (updatedText == null) {
            return;
        }

        taskModel.set(selectedIndex, selectedTask.withText(updatedText));
        taskList.setSelectedIndex(selectedIndex);
        saveAndRefresh();
    }

    private String promptForTaskText(String currentText) {
        String updatedText = JOptionPane.showInputDialog(this, "Edit task:", currentText);
        if (updatedText == null) {
            return null;
        }

        updatedText = updatedText.trim();
        if (updatedText.isEmpty()) {
            showWarning("A task cannot be empty.");
            return null;
        }

        return updatedText;
    }

    private void deleteSelectedTask() {
        int selectedIndex = getSelectedTaskIndex("Please select a task to delete.");
        if (selectedIndex == -1 || !confirm("Delete the selected task?", "Confirm Delete")) {
            return;
        }

        taskModel.remove(selectedIndex);
        saveAndRefresh();
    }

    private void toggleSelectedTaskCompletion() {
        int selectedIndex = getSelectedTaskIndex("Please select a task to update.");
        if (selectedIndex == -1) {
            return;
        }

        Task selectedTask = taskModel.get(selectedIndex);
        taskModel.set(selectedIndex, selectedTask.toggleCompleted());
        taskList.setSelectedIndex(selectedIndex);
        saveAndRefresh();
    }

    private void moveSelectedTask(int direction) {
        int selectedIndex = getSelectedTaskIndex("Please select a task to move.");
        if (selectedIndex == -1) {
            return;
        }

        int newIndex = selectedIndex + direction;
        if (!isValidTaskIndex(newIndex)) {
            return;
        }

        Task selectedTask = taskModel.remove(selectedIndex);
        taskModel.add(newIndex, selectedTask);
        taskList.setSelectedIndex(newIndex);
        saveTasks();
    }

    private void clearCompletedTasks() {
        if (taskModel.isEmpty()) {
            showWarning("There are no tasks to clear.");
            return;
        }

        if (countCompletedTasks() == 0) {
            showWarning("There are no completed tasks to clear.");
            return;
        }

        if (!confirm("Clear all completed tasks?", "Confirm Clear")) {
            return;
        }

        removeCompletedTasks();
        saveAndRefresh();
    }

    private void removeCompletedTasks() {
        for (int i = taskModel.size() - 1; i >= 0; i--) {
            if (taskModel.get(i).completed()) {
                taskModel.remove(i);
            }
        }
    }

    private int getSelectedTaskIndex(String warningMessage) {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex == -1) {
            showWarning(warningMessage);
        }
        return selectedIndex;
    }

    private boolean isValidTaskIndex(int index) {
        return index >= 0 && index < taskModel.size();
    }

    private boolean confirm(String message, String title) {
        int choice = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    private void loadTasks() {
        if (!Files.exists(TASK_FILE)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(TASK_FILE, StandardCharsets.UTF_8)) {
                Task.fromFileLine(line).ifPresent(taskModel::addElement);
            }
        } catch (IOException exception) {
            showError("Could not load saved tasks: " + exception.getMessage());
        }
    }

    private void saveAndRefresh() {
        saveTasks();
        refreshStatus();
    }

    private void saveTasks() {
        try {
            Files.write(TASK_FILE, getTaskFileLines(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            showError("Could not save tasks: " + exception.getMessage());
        }
    }

    private List<String> getTaskFileLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < taskModel.size(); i++) {
            lines.add(taskModel.get(i).toFileLine());
        }
        return lines;
    }

    private void refreshStatus() {
        int totalTasks = taskModel.size();
        int completedTasks = countCompletedTasks();
        int activeTasks = totalTasks - completedTasks;

        statusLabel.setText(String.format(
                "%d total  |  %d active  |  %d done",
                totalTasks,
                activeTasks,
                completedTasks));
    }

    private int countCompletedTasks() {
        int completedTasks = 0;

        for (int i = 0; i < taskModel.size(); i++) {
            if (taskModel.get(i).completed()) {
                completedTasks++;
            }
        }

        return completedTasks;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, APP_TITLE, JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "File Error", JOptionPane.ERROR_MESSAGE);
    }

    private record ButtonSpec(
            String text,
            Color background,
            java.awt.event.ActionListener action) {
    }

    private record Task(String text, boolean completed) {
        private static java.util.Optional<Task> fromFileLine(String line) {
            String cleanLine = line.trim();

            if (cleanLine.isEmpty()) {
                return java.util.Optional.empty();
            }

            if (cleanLine.startsWith(DONE_PREFIX)) {
                return java.util.Optional.of(new Task(cleanLine.substring(DONE_PREFIX.length()), true));
            }

            return java.util.Optional.of(new Task(cleanLine, false));
        }

        private Task withText(String newText) {
            return new Task(newText, completed);
        }

        private Task toggleCompleted() {
            return new Task(text, !completed);
        }

        private String toFileLine() {
            return completed ? DONE_PREFIX + text : text;
        }

        @Override
        public String toString() {
            return toFileLine();
        }
    }

    private static class TaskCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            Task task = (Task) value;
            label.setText(task.toFileLine());
            label.setBorder(new EmptyBorder(4, 8, 4, 8));
            label.setFont(new Font(FONT_FAMILY, task.completed() ? Font.ITALIC : Font.PLAIN, 15));

            if (!isSelected) {
                label.setForeground(task.completed() ? COMPLETED_TEXT_COLOR : TEXT_COLOR);
                label.setBackground(task.completed() ? COMPLETED_BACKGROUND : Color.WHITE);
            }

            return label;
        }
    }
}
