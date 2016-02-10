package cc.redpen.intellij;

import cc.redpen.config.Symbol;
import cc.redpen.config.SymbolTable;
import cc.redpen.config.SymbolType;
import cc.redpen.config.ValidatorConfiguration;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class RedPenSettingsPane {
  RedPenProvider redPenProvider = RedPenProvider.getInstance();
  private JPanel root;
  private JTabbedPane tabbedPane;
  JTable validators;
  JTable symbols;
  JComboBox<String> language;

  public JPanel getPane() {
    addLanguages();
    addValidators();
    addSymbols();
    return root;
  }

  void addLanguages() {
    redPenProvider.getAvailableConfigs().keySet().forEach(k -> language.addItem(k));
  }

  private void addSymbols() {
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

    SymbolTable symbolTable = redPenProvider.getConfig().getSymbolTable();
    for (SymbolType key : symbolTable.getNames()) {
      Symbol symbol = symbolTable.getSymbol(key);
      model.addRow(new Object[] {symbol.getType().toString(), symbol.getValue(),
        new String(symbol.getInvalidChars()), symbol.isNeedBeforeSpace(), symbol.isNeedAfterSpace()});
    }

    symbols.doLayout();
  }

  private void addValidators() {
    DefaultTableModel model = createValidatorsModel();
    validators.setModel(model);
    validators.setRowHeight((int)(validators.getFont().getSize() * 1.5));

    model.addColumn("");
    model.addColumn("Name");
    model.addColumn("Properties (comma-separated)");

    validators.getColumnModel().getColumn(0).setMaxWidth(20);

    for (ValidatorConfiguration validator : redPenProvider.getInitialConfig().getValidatorConfigs()) {
      boolean enabled = redPenProvider.getConfig().getValidatorConfigs().stream().anyMatch(v -> v.getConfigurationName().equals(validator.getConfigurationName()));
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
         ValidatorConfiguration validator = redPenProvider.getInitialConfig().getValidatorConfigs().get(i);
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
