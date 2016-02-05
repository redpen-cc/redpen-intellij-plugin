package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RedPenSettingsPane {
  private JPanel root;
  JTable validators;
  private Configuration config;

  public RedPenSettingsPane(Configuration config) {
    this.config = config;
  }

  public JPanel getPane() {
    DefaultTableModel model = model();
    validators.setModel(model);
    validators.setRowHeight((int)(validators.getFont().getSize() * 1.5));

    model.addColumn("");
    model.addColumn("Name");
    model.addColumn("Properties");

    validators.getColumnModel().getColumn(0).setMaxWidth(20);

    for (ValidatorConfiguration validatorConfig : config.getValidatorConfigs()) {
      model.addRow(new Object[] {true, validatorConfig.getConfigurationName(), attributes(validatorConfig)});
    }

    validators.doLayout();
    return root;
  }

  private String attributes(ValidatorConfiguration validatorConfig) {
    String result = validatorConfig.getAttributes().toString();
    return result.substring(1, result.length() - 1);
  }

  @NotNull DefaultTableModel model() {
    return new DefaultTableModel() {
      @Override public Class<?> getColumnClass(int i) {
        return i == 0 ? Boolean.class : String.class;
      }

      @Override public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
  }
}
