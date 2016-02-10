package cc.redpen.intellij;

import cc.redpen.config.ValidatorConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RedPenSettingsPane {
  RedPenProvider redPenProvider = RedPenProvider.getInstance();
  private JPanel root;
  JTable validators;

  public JPanel getPane() {
    DefaultTableModel model = createModel();
    validators.setModel(model);
    validators.setRowHeight((int)(validators.getFont().getSize() * 1.5));

    model.addColumn("");
    model.addColumn("Name");
    model.addColumn("Properties");

    validators.getColumnModel().getColumn(0).setMaxWidth(20);

    for (ValidatorConfiguration validator : redPenProvider.getInitialConfig().getValidatorConfigs()) {
      boolean enabled = redPenProvider.getConfig().getValidatorConfigs().stream().anyMatch(v -> v.getConfigurationName().equals(validator.getConfigurationName()));
      model.addRow(new Object[] {enabled, validator.getConfigurationName(), attributes(validator)});
    }

    validators.doLayout();
    return root;
  }

  public List<ValidatorConfiguration> getActiveValidators() {
    List<ValidatorConfiguration> result = new ArrayList<>();
    TableModel model = validators.getModel();
    for (int i = 0; i < model.getRowCount(); i++) {
       if ((boolean)model.getValueAt(i, 0)) {
         ValidatorConfiguration validator = redPenProvider.getInitialConfig().getValidatorConfigs().get(i);
         validator.getAttributes().clear();
         String attributes = (String)model.getValueAt(i, 2);
         Stream.of(attributes.trim().split("\\s*,\\s*")).forEach(s -> {
           String[] attr = s.split("=");
           validator.addAttribute(attr[0], attr[1]);
         });
         result.add(validator);
       }
    }
    return result;
  }

  private String attributes(ValidatorConfiguration validatorConfig) {
    String result = validatorConfig.getAttributes().toString();
    return result.substring(1, result.length() - 1);
  }

  @NotNull DefaultTableModel createModel() {
    return new DefaultTableModel() {
      @Override public Class<?> getColumnClass(int i) {
        return i == 0 ? Boolean.class : String.class;
      }

      @Override public boolean isCellEditable(int row, int column) {
        return column != 1;
      }
    };
  }
}
