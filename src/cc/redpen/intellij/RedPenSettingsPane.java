package cc.redpen.intellij;

import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

public class RedPenSettingsPane {
  private JPanel root;
  JTable validators;
  private Configuration config;

  public RedPenSettingsPane(Configuration config) {
    this.config = config;
  }

  public JPanel getPane() {
    DefaultTableModel model = createModel();
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

  public List<ValidatorConfiguration> getActiveValidators() {
    List<ValidatorConfiguration> result = new ArrayList<>();
    TableModel model = validators.getModel();
    for (int i = 0; i < model.getRowCount(); i++) {
       if ((boolean)model.getValueAt(i, 0)) result.add(config.getValidatorConfigs().get(i));
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
        return column == 0;
      }
    };
  }
}
