package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class RedPenSettingsPane {
  private JPanel root;
  private JTable validators;
  private Configuration config;

  public RedPenSettingsPane(Configuration config) {
    this.config = config;
  }

  public JPanel getPane() {
    DefaultTableModel model = new DefaultTableModel() {
      @Override public Class<?> getColumnClass(int i) {
        return i == 0 ? Boolean.class : String.class;
      }

      @Override public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    validators.setModel(model);

    model.addColumn("");
    model.addColumn("Name");
    model.addColumn("Properties");

    validators.getColumnModel().getColumn(0).setPreferredWidth(20);

    for (ValidatorConfiguration validatorConfig : config.getValidatorConfigs()) {
      model.addRow(new Object[] {true, validatorConfig.getValidatorClassName(), validatorConfig.getAttributes().toString()});
    }

    validators.doLayout();
    return root;
  }
}
