package com.tmilar.labelsimplification;

import com.tmilar.labelsimplification.model.Extractor;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LabelSimplificationCli {

  private static final Logger logger = LogManager.getLogger(LabelSimplificationCli.class);
  private static final String CSV_SEPARATOR = ",";

  public static void main(String[] args) throws IOException {
    String labelsInputCsvPath = "/data/input_items.csv";
    String labelStrColName = "Description";
    String labelCatColName = "Category";
    String rulesCsvPath = "/data/input_rules.csv";
    String outputCsvPath = "./out/items_simplified.csv";
    String stopwordsCsvPath = "/data/stop_words.csv";

    // initialize labelSimplificationService with extraction rules + stop words
    List<Extractor> extractionRules = readExtractionRulesFromCsv(rulesCsvPath, CSV_SEPARATOR);
    Map<String, List<String>> categoryStopWords = readStopWordsFromCsv(stopwordsCsvPath,
        CSV_SEPARATOR);

    LabelSimplificationService labelSimplificationService = new LabelSimplificationService();
    labelSimplificationService.load(extractionRules, categoryStopWords);
    logger.info("Loaded {} label rules from '{}'", extractionRules.size(), rulesCsvPath);

    // initialize input labels
    List<Label> labels = readLabelsFromCsv(
        labelsInputCsvPath, CSV_SEPARATOR, labelStrColName, labelCatColName);

    Map<String, Set<String>> categoryMappings = labelSimplificationService.getCategoryMappings();

    logger.info("Processing {} labels from '{}'...", labels.size(), labelsInputCsvPath);

    // output (one export per category)
    categoryMappings.forEach((category, categoryHeader) -> {
      List<SimplifiedLabel> simplifiedLabels = labels.stream()
          .filter(label -> Objects.equals(label.getCategory(), category))
          .map(labelSimplificationService::simplifyLabel)
          .collect(Collectors.toList());

      if (simplifiedLabels.isEmpty()) {
        logger.info("Skip save export for category '{}' "
            + "(no labels simplified belong to this category)", category);
        return;
      }

      String categoryCsvPath = outputCsvPath.replace(".csv", String.format("_%s.csv", category));

      try {
        writeResultToCsv(simplifiedLabels, categoryHeader, categoryCsvPath, CSV_SEPARATOR);
      } catch (IOException e) {
        logger.error("Could not save '{}' labels '{}' category to csv '{}'",
            simplifiedLabels, category, categoryCsvPath, e);
        return;
      }
      logger.info("Saved category '{}' {} results to: '{}'",
          category, simplifiedLabels.size(), categoryCsvPath);
    });
  }

  private static Map<String, List<String>> readStopWordsFromCsv(String csvPath, String csvSeparator)
      throws IOException {
    InputStream csvResource = LabelSimplificationCli.class.getResourceAsStream(csvPath);

    Reader bufferedReader = new BufferedReader(new InputStreamReader(csvResource));

    CSVParser csvParser = new CSVParser(bufferedReader,
        CSVFormat.DEFAULT.withDelimiter(csvSeparator.toCharArray()[0]));

    // Get all rows as list.
    List<CSVRecord> records = csvParser.getRecords();

    // get labels from each row.
    Map<String, List<String>> catStopwords = new LinkedHashMap<>();

    for (CSVRecord r : records) {
      if (r.getRecordNumber() == 1) {
        continue; // skip header row
      }
      String key = r.get(0);
      String stopword = r.get(1);
      if (!catStopwords.containsKey(key)) {
        catStopwords.put(key, new ArrayList<>());
      }
      catStopwords.get(key).add(stopword);
    }

    csvParser.close();
    bufferedReader.close();

    return catStopwords;
  }

  /**
   * Get transaction labels from CSV file, based on defined structures.
   *
   * @param csvPath    - path of the csv file.
   * @param labelField - actual label field name to read.
   * @return the labels string list
   */
  private static List<Label> readLabelsFromCsv(String csvPath, String csvSeparator,
      String labelField, String categoryField)
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
    Integer categoryFieldColIndex = headerRow.indexOf(categoryField);

    // get labels from each row.
    List<Label> labels = records
        .stream()
        .skip(1)
        .map(record -> new Label(record.get(labelFieldColIndex), record.get(categoryFieldColIndex)))
        .collect(Collectors.toList());

    csvParser.close();
    bufferedReader.close();

    return labels;
  }

  /**
   * Get transaction labels from CSV file, based on defined structures.
   *
   * @param csvPath - path of the csv file.
   * @return the labels string list
   */
  private static List<Extractor> readExtractionRulesFromCsv(String csvPath, String csvSeparator)
      throws IOException {

    InputStream csvResource = LabelSimplificationCli.class.getResourceAsStream(csvPath);

    Reader bufferedReader = new BufferedReader(new InputStreamReader(csvResource));

    CSVParser csvParser = new CSVParser(bufferedReader,
        CSVFormat.DEFAULT.withDelimiter(csvSeparator.toCharArray()[0]));

    // Get all rows as list.
    List<CSVRecord> records = csvParser.getRecords();

    // get labels from each row.
    List<Extractor> extractors = records
        .stream()
        .skip(1)
        .map(r -> new Extractor(
            r.get(1),
            r.get(2),
            r.get(3),
            r.get(5),
            Integer.valueOf(r.get(4)),
            r.get(0))
        )
        .collect(Collectors.toList());

    csvParser.close();
    bufferedReader.close();

    return extractors;
  }

  /**
   * Write simplified labels result to an output CSV file.
   *
   * @param simplifiedLabels - the labels simplified
   * @param outputCsvPath    - csv path to write to
   * @param csvSeparator     - separator to use
   * @throws IOException - exception if path doesn't exist or inaccessible
   */
  private static void writeResultToCsv(List<SimplifiedLabel> simplifiedLabels,
      Set<String> categoryHeader, String outputCsvPath, String csvSeparator) throws IOException {

    List<String> header = new LinkedList<>(Arrays.asList("OriginalItem", "CatalogItem"));
    header.addAll(categoryHeader);

    try (
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputCsvPath));

        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader(header.toArray(new String[0]))
            .withDelimiter(csvSeparator.charAt(0)));
    ) {

      for (SimplifiedLabel label : simplifiedLabels) {
        List<String> record = new LinkedList<>(
            Arrays.asList(label.getLabel(), label.getSimplifiedLabel()));
        Map<String, String> extractedValuesMap = label.getExtractedValuesMap();
        categoryHeader.forEach(catHeader -> record.add(extractedValuesMap.get(catHeader)));

        csvPrinter.printRecord(record);
      }

      csvPrinter.flush();
    }
  }
}
