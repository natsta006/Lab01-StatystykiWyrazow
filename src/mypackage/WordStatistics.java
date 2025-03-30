package mypackage;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WordStatistics {

    public static Map<String, Long> getLinkedCountedWords(Path path, int wordsLimit) {
        // Otwarcie pliku do odczytu
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            // Podział pliku na linie
            return reader.lines()
                    // Podział linii na słowa, znaki białe jako separator
                    .flatMap(line -> Arrays.stream(line.split("\\s+")))
                    // Usunięcie wszytskich znaków specjalnych z każdego słowa
                    .map(word -> word.replaceAll("[^a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{1}", ""))
                    // Filtrowanie słów, które mają conajmniej 3 znaki
                    .filter(word -> word.matches("[a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{3,}"))
                    // Zamiana słów na małe litery
                    .map(String::toLowerCase)
                    // Grupowanie i liczenie wystąpień słów
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    // Konwersja mapy na strumień
                    .entrySet().stream()
                    // Sortowanie słów malejąco po liczbie wystąpień
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    // Ograniczenie ilości słów do wybranej liczby (10 w tym przypadku) najczęściej występujących
                    .limit(wordsLimit)
                    // Przekształcenie strumienia na mapę
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, // Kluczem jest słowo
                            Map.Entry::getValue, // Wartością jest liczba wystąpień
                            // Obsługa błędu w przypadku duplikacji klucza
                            (k,v) -> { throw new IllegalStateException(String.format("Błąd! Duplikat klucza %s.", k)); },
                            LinkedHashMap::new));
        // Obsługa błędów związanych z wejściem/wyjściem
        }catch (IOException e) {throw new RuntimeException(e);}
    }

}
