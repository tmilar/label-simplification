package com.tmilar.labelsimplification;

import com.tmilar.labelsimplification.model.Label;
import com.tmilar.labelsimplification.model.SimplifiedLabel;
import com.tmilar.labelsimplification.service.LabelSimplificationService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LabelSimplificationCli {

  private static final Logger logger = LogManager.getLogger(LabelSimplificationCli.class);

  public static void main(String[] args) throws IOException {
    String inputCsvPath = "/data/input_items.csv";
    String csvLabelColName = "Description";
    String csvSeparator = ",";
    String outputCsvPath = "./out/items_simplified.csv";

    // initialize 
    String[][] extractionsData = getExtractionsData();
    LabelSimplificationService labelSimplificationService = new LabelSimplificationService();
    labelSimplificationService.load(extractionsData);

    // process labels
    List<Label> labels = readLabelsFromCsv(inputCsvPath, csvSeparator, csvLabelColName);

    List<SimplifiedLabel> simplifiedLabels = labels.stream()
        .map(label -> new SimplifiedLabel(
            label, labelSimplificationService.simplifyLabel(label.getLabel())))
        .collect(Collectors.toList());

    // output
    writeResultToCsv(simplifiedLabels, outputCsvPath, csvSeparator);

    logger.info("Saved {} results to: '{}'", simplifiedLabels.size(), outputCsvPath);
  }

  /**
   * Get transaction labels from CSV file, based on defined structures.
   *
   * @param csvPath    - path of the csv file.
   * @param labelField - actual label field name to read.
   * @return the labels string list
   */
  private static List<Label> readLabelsFromCsv(String csvPath, String csvSeparator,
      String labelField)
      throws IOException {

    InputStream csvResource = LabelSimplificationCli.class.getResourceAsStream(csvPath);

    Reader bufferedReader = new BufferedReader(new InputStreamReader(csvResource));

    CSVParser csvParser = new CSVParser(bufferedReader,
        CSVFormat.DEFAULT.withDelimiter(csvSeparator.toCharArray()[0]));

    // Get all rows as list.
    List<CSVRecord> records = csvParser.getRecords();

    // get header row, to find the label field col index.
    CSVRecord header = records.get(0);
    List<String> headerRow = new ArrayList<>();
    header.iterator().forEachRemaining(headerRow::add);

    Integer labelFieldColIndex = headerRow.indexOf(labelField);

    // get labels from each row.
    List<Label> labels = records
        .stream()
        .skip(1)
        .map(record -> record.get(labelFieldColIndex))
        .map(Label::new)
        .collect(Collectors.toList());

    csvParser.close();
    bufferedReader.close();

    return labels;
  }

  private static String[][] getExtractionsData() {
    // TODO this is a hardcoded sample. Fetch actual data from a valid input.
    return new String[][]{
        // keyName, extractedValue, matcher, parentKeyName, parentValue
        {"Juego", "Magic", "Mtg|Mag", null, null},
        {"Juego", "Pokemon", "Pok|Pkm", null, null},
        {"Coleccion", "Sun & Moon", "SM1|Sun & Moon", "Juego", "Pokemon"},
        {"Coleccion", "Sun & Moon: Guardians Rising", "SM2|Guardians Rising", "Juego", "Pokemon"},
        {"Coleccion", "Theros", "Theros", "Juego", "Magic"},
        {"TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "Juego", "Pokemon"},
        {"TipoProducto", "Booster Box", "Booster Box|Booster Display Box", "Juego", "Magic"},
        {"Idioma", "Español", "SP]Spanish]Esp", null, null},
        {"Idioma", "Ingles", "", null, null}
    };
  }

  /**
   * Write simplified labels result to an output CSV file.
   *
   * @param simplifiedLabels - the labels simplified
   * @param outputCsvPath    - csv path to write to
   * @param csvSeparator     - separator to use
   * @throws IOException - exception if path doesn't exist or inaccessible
   */
  private static void writeResultToCsv(List<SimplifiedLabel> simplifiedLabels, String outputCsvPath,
      String csvSeparator) throws IOException {

    try (
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputCsvPath));

        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader("Original", "Simplified")
            .withDelimiter(csvSeparator.charAt(0)));
    ) {

      for (SimplifiedLabel label : simplifiedLabels) {
        csvPrinter.printRecord(label.getLabel(), label.getSimplifiedLabel());
      }

      csvPrinter.flush();
    }
  }
}
