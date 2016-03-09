package cc.redpen.intellij.fixes

import cc.redpen.config.Configuration
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

open class NumberFormatQuickFix(val config: Configuration, text: String) : BaseQuickFix(text) {
    override fun fixedText(): String {
        val config = config.validatorConfigs.find { it.configurationName == "NumberFormat" }!!
        val eu = config.attributes["decimal_delimiter_is_comma"] == "true"

        val format = NumberFormat.getNumberInstance(if (eu) Locale.GERMANY else Locale.US)
        val number = BigDecimal(text)
        format.minimumFractionDigits = number.scale()
        return format.format(number)
    }
}
