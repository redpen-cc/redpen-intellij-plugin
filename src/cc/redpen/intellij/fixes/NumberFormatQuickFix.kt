package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

open class NumberFormatQuickFix(var config: Configuration, text: String) : BaseQuickFix(text) {
    override fun fixedText(): String {
        try {
            val validatorConfig = config.validatorConfigs.find { it.configurationName == "NumberFormat" }!!
            val eu = validatorConfig.attributes["decimal_delimiter_is_comma"] == "true"

            val format = NumberFormat.getNumberInstance(if (eu) Locale.GERMANY else Locale.US)
            val number = BigDecimal(text.replace(',', '.'))
            format.minimumFractionDigits = number.scale()
            val result = format.format(number)
            return if (config.lang == "ja") result.replace('.', '・').replace(',', '、') else result
        }
        catch (e: NumberFormatException) {
            return text
        }
    }
}
