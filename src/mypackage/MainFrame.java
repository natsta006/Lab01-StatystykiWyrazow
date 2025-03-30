package mypackage;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;








public class MainFrame {
    private JFrame frame;
    private static final String DIR_PATH = "files";
    private final int liczbaWyrazowStatystyki;
    private final AtomicBoolean fajrant;
    private final int liczbaProducentow;
    private final int liczbaKonsumentow;
    private final ExecutorService executor;
    private final List<Future<?>> producentFuture;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainFrame window = new MainFrame();
                    window.frame.pack();
                    window.frame.setAlwaysOnTop(true);
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MainFrame() {
        liczbaWyrazowStatystyki = 10;
        fajrant = new AtomicBoolean(false);
        liczbaProducentow = 1;
        liczbaKonsumentow = 2;
        executor = Executors.newFixedThreadPool(liczbaProducentow + liczbaKonsumentow);
        producentFuture = new ArrayList<>();
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executor.shutdownNow();
            }
        });
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.NORTH);
        JButton btnStop = new JButton("Stop");
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fajrant.set(true);
                for (Future<?> f : producentFuture) {
                    f.cancel(true);
                }
            }
        });
        JButton btnStart = new JButton("Start");
        btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getMultiThreadedStatistics();
            }
        });
        JButton btnZamknij = new JButton("Zamknij");
        btnZamknij.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                executor.shutdownNow();
                frame.dispose();
            }
        });
        panel.add(btnStart);
        panel.add(btnStop);
        panel.add(btnZamknij);
    }


    private void getMultiThreadedStatistics() {
        for (Future<?> f : producentFuture) {
            if (!f.isDone()) {
                JOptionPane.showMessageDialog(frame, "Nie można uruchomić nowego zadania!" +
                        "Przynajmniej jeden producent nadal działa!", "OSTRZEŻENIE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        fajrant.set(false);
        producentFuture.clear();
        final BlockingQueue<Optional<Path>> kolejka = new LinkedBlockingQueue<>(liczbaKonsumentow);
        final int przerwa = 60;

        Runnable producent = () -> {
            final String name = Thread.currentThread().getName();
            String info = String.format("PRODUCENT %s URUCHOMIONY ...", name);
            System.out.println(info);

            while (!Thread.currentThread().isInterrupted()) {
                if(fajrant.get()) {
                    for (int i = 0; i < liczbaKonsumentow; i++) { // Iteracja pętli tyle razy, ile jest konsumentów
                        try {
                            kolejka.put(Optional.empty()); // Wstawienie do kolejki pustego obiektu, informującego o końcu pracy
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Jeśli wątek zostanie przerwany, ustawienie flagi i przerwanie działania
                            return;
                        }
                    }
                    break;

                } else {
                    try {
                        Path dir = Paths.get("files"); // Ścieżka do katalogu, który będzie przeszukiwany

                        // Przechodzenie przez strukturę katalogów
                        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                                // Sprawdzenie, czy plik ma rozszerzenie .txt
                                if (path.toString().endsWith(".txt")) {
                                    try {
                                        Optional<Path> optPath = Optional.ofNullable(path); // Opakowanie ścieżki w Optional
                                        kolejka.put(optPath); // Dodanie ścieżki pliku do kolejki
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt(); // Ustawienie flagi przerwania
                                        return FileVisitResult.TERMINATE; // Zatrzymanie przeszukiwania
                                    }
                                }
                                return FileVisitResult.CONTINUE; // Kontynuacja przeszukiwania kolejnych plików
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace(); // Obsługa błędu związanego z przechodzeniem przez katalogi
                    }
                }
                info = String.format("Producent %s ponownie sprawdzi katalogi za %d sekund", name, przerwa);
                System.out.println(info);
                try {
                    TimeUnit.SECONDS.sleep(przerwa);
                } catch (InterruptedException e) {
                    info = String.format("Przerwa producenta %s przerwana!", name);
                    System.out.println(info);
                    if(!fajrant.get()) Thread.currentThread().interrupt();
                }
            }
            info = String.format("PRODUCENT %s SKOŃCZYŁ PRACĘ", name);
            System.out.println(info);
        };
        Runnable konsument = () -> {
            final String name = Thread.currentThread().getName();
            String info = String.format("KONSUMENT %s URUCHOMIONY ...", name);
            System.out.println(info);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Optional<Path> optPath = kolejka.take(); // Pobranie ścieżki do pliku z kolejki
                    // Sprawdzenie czy ścieżka jest pusta (posion pill)
                    if (!optPath.isPresent()) {
                        // Jesli ścieżka jest pusta, kończymy działanie
                        break;
                    }
                    // Wywołanie metody do obliczania statystyk słów w pliku
                    Map<String, Long> statystyki = WordStatistics.getLinkedCountedWords(optPath.get(), liczbaWyrazowStatystyki);
                    // Synchronizacja wyjścia na konsolę, aby uniknąć jednoczesnego dostępu do konsoli przez dwa wątki
                    synchronized (System.out) {
                        // Wypisanie na konsolę statystyk plików
                        System.out.println("Statystyki dla pliku: " + optPath.get().toString());
                        statystyki.forEach((word, count) -> {
                            System.out.println("Słowo: " + word + " | Liczba wystąpień: " + count);
                        });
                        System.out.println();
                    }
                } catch (InterruptedException e) {
                    info = String.format("Oczekiwanie konsumenta %s na nowy element z kolejki przerwane!", name);
                    System.out.println(info);
                    Thread.currentThread().interrupt();
                }

            }
            info = String.format("KONSUMENT %s ZAKOŃCZYŁ PRACĘ", name);
            System.out.println(info);
        };
        for (int i = 0; i < liczbaProducentow; i++) {
            Future<?> pf = executor.submit(producent);
            producentFuture.add(pf);
        }
        for (int i = 0; i < liczbaKonsumentow; i++) {
            executor.execute(konsument);
        }
    }
}
