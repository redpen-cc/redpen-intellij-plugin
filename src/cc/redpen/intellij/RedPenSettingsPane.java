package cc.redpen.intellij;

import cc.redpen.RedPenException;
import cc.redpen.config.*;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.swing.JFileChooser.APPROVE_OPTION;

public class RedPenSettingsPane {
  RedPenProvider redPenProvider;
  Configuration config;
  JPanel root;
  private JTabbedPane tabbedPane;
  JTable validators;
  JTable symbols;
  JComboBox<String> language;
  JCheckBox autodetectLanguage;
  JButton exportButton;
  JButton importButton;
  JFileChooser fileChooser = new JFileChooser();
  ConfigurationExporter configurationExporter = new ConfigurationExporter();
  ConfigurationLoader configurationLoader = new ConfigurationLoader();

  public RedPenSettingsPane(RedPenProvider redPenProvider) {
    this.redPenProvider = redPenProvider;
    config = redPenProvider.getActiveConfig().clone();
    fileChooser.setFileFilter(new FileNameExtensionFilter("RedPen Configuration", "xml"));
  }

  public JPanel getPane() {
    initLanguages();
    initTabs();
    initButtons();
    return root;
  }

  void initButtons() {
    exportButton.addActionListener(a -> exportConfig());
    importButton.addActionListener(a -> importConfig());
  }

  void importConfig() {
    try {
      if (fileChooser.showOpenDialog(root) != APPROVE_OPTION) return;
      config = configurationLoader.load(fileChooser.getSelectedFile());
      selectLanguage();
      initTabs();
    }
    catch (RedPenException e) {
      Messages.showMessageDialog("Cannot load: " + e.getMessage(), "RedPen", Messages.getErrorIcon());
    }
  }

  void selectLanguage() {
    language.setSelectedItem(config.getKey());
    if (!config.getKey().equals(language.getSelectedItem())) {
      redPenProvider.addConfig(config);
      language.addItem(config.getKey());
      language.setSelectedItem(config.getKey());
    }
  }

  void exportConfig() {
    try {
      if (fileChooser.showSaveDialog(root) != APPROVE_OPTION) return;
      apply(config);
      configurationExporter.export(config, new FileOutputStream(fileChooser.getSelectedFile()));
    }
    catch (IOException e) {
      Messages.showMessageDialog("Cannot write to file: " + e.getMessage(), "RedPen", Messages.getErrorIcon());
    }
  }

  void initLanguages() {
    redPenProvider.getConfigs().keySet().forEach(k -> language.addItem(k));
    autodetectLanguage.setSelected(redPenProvider.isAutodetect());
    language.setSelectedItem(config.getKey());
    language.addActionListener(a -> {
      config = redPenProvider.getConfig((String)language.getSelectedItem());
      initTabs();
    });
  }

  void initTabs() {
    initValidators();
    initSymbols();
  }

  void initSymbols() {
    symbols.removeAll();
    DefaultTableModel model = createSymbolsModel();
    symbols.setModel(model);
    symbols.setRowHeight((int)(validators.getFont().getSize() * 1.5));

    model.addColumn("Symbols");
    model.addColumn("Value");
    model.addColumn("Invalid chars");
    model.addColumn("Space before");
    model.addColumn("Space after");

    symbols.getColumnModel().getColumn(0).setMinWidth(250);
    symbols.setDefaultEditor(Character.class, new SingleCharEditor());

    SymbolTable symbolTable = config.getSymbolTable();
    for (SymbolType key : symbolTable.getNames()) {
      Symbol symbol = symbolTable.getSymbol(key);
      model.addRow(new Object[] {symbol.getType().toString(), symbol.getValue(),
        new String(symbol.getInvalidChars()), symbol.isNeedBeforeSpace(), symbol.isNeedAfterSpace()});
    }

    symbols.doLayout();
  }

  void initValidators() {
    validators.removeAll();
    DefaultTableModel model = createValidatorsModel();
    validators.setModel(model);
    validators.setRowHeight((int)(validators.getFont().getSize() * 1.5));

    model.addColumn("");
    model.addColumn("Name");
    model.addColumn("Properties (comma-separated)");

    validators.getColumnModel().getColumn(0).setMaxWidth(20);

    for (ValidatorConfiguration validator : redPenProvider.getInitialConfig(config.getKey()).getValidatorConfigs()) {
      boolean enabled = config.getValidatorConfigs().stream().anyMatch(v -> v.getConfigurationName().equals(validator.getConfigurationName()));
      model.addRow(new Object[] {enabled, validator.getConfigurationName(), attributes(validator)});
    }

    validators.doLayout();
  }

  public List<ValidatorConfiguration> getActiveValidators() {
    List<ValidatorConfiguration> result = new ArrayList<>();
    if (validators.isEditing()) validators.getCellEditor().stopCellEditing();
    TableModel model = validators.getModel();
    for (int i = 0; i < model.getRowCount(); i++) {
       if ((boolean)model.getValueAt(i, 0)) {
         ValidatorConfiguration validator = redPenProvider.getInitialConfig(config.getKey()).getValidatorConfigs().get(i);
         validator.getAttributes().clear();
         String attributes = (String)model.getValueAt(i, 2);
         Stream.of(attributes.trim().split("\\s*,\\s*")).filter(s -> !s.isEmpty()).forEach(s -> {
           String[] attr = s.split("=", 2);
           if (attr.length < 2 || attr[0].isEmpty())
             showPropertyError(validator, s);
           else
             validator.addAttribute(attr[0], attr[1]);
         });
         result.add(validator);
       }
    }
    return result;
  }

  void showPropertyError(ValidatorConfiguration validator, String s) {
    Messages.showMessageDialog("Validator property must be in key=value format: " + s, validator.getConfigurationName(), Messages.getErrorIcon());
  }

  public List<Symbol> getSymbols() {
    if (symbols.isEditing()) symbols.getCellEditor().stopCellEditing();

    TableModel model = symbols.getModel();
    return range(0, model.getRowCount()).mapToObj(i -> new Symbol(
      SymbolType.valueOf((String)model.getValueAt(i, 0)), String.valueOf(model.getValueAt(i, 1)).charAt(0), (String)model.getValueAt(i, 2),
      (boolean)model.getValueAt(i, 3), (boolean)model.getValueAt(i, 4)
    )).collect(toList());
  }

  private String attributes(ValidatorConfiguration validatorConfig) {
    String result = validatorConfig.getAttributes().toString();
    return result.substring(1, result.length() - 1);
  }

  void applyValidatorsChanges(Configuration config) {
    List<ValidatorConfiguration> validators = config.getValidatorConfigs();
    List<ValidatorConfiguration> remainingValidators = getActiveValidators();
    validators.clear();
    validators.addAll(remainingValidators);
  }

  void applySymbolsChanges(Configuration config) {
    SymbolTable symbolTable = config.getSymbolTable();
    getSymbols().stream().forEach(symbolTable::overrideSymbol);
  }

  void apply(Configuration config) {
    applyValidatorsChanges(config);
    applySymbolsChanges(config);
  }

  DefaultTableModel createValidatorsModel() {
    return new DefaultTableModel() {
      @Override public Class<?> getColumnClass(int i) {
        return i == 0 ? Boolean.class : String.class;
      }

      @Override public boolean isCellEditable(int row, int column) {
        return column != 1;
      }
    };
  }

  DefaultTableModel createSymbolsModel() {
    return new DefaultTableModel() {
      @Override public Class<?> getColumnClass(int i) {
        return i == 1 ? Character.class : i == 3 || i == 4 ? Boolean.class : String.class;
      }

      @Override public boolean isCellEditable(int row, int column) {
        return column != 0;
      }
    };
  }
}
