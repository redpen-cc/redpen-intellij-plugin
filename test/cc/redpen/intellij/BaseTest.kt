package cc.redpen.intellij

import cc.redpen.config.Configuration
import cc.redpen.config.Symbol
import cc.redpen.config.ValidatorConfiguration
import com.nhaarman.mockito_kotlin.whenever
import org.mockito.Mockito.mock

abstract class BaseTest {
    protected fun configWithValidators(validatorConfigs: List<ValidatorConfiguration>): Configuration {
        val builder = Configuration.ConfigurationBuilder()
        validatorConfigs.forEach { builder.addValidatorConfig(it) }
        return builder.build()
    }

    protected fun configWithSymbols(symbols: List<Symbol>): Configuration {
        val builder = Configuration.ConfigurationBuilder()
        symbols.forEach { builder.addSymbol(it) }
        return builder.build()
    }

    protected fun cloneableConfig(key: String): Configuration {
        val config = config(key)
        val configClone = config(key)
        whenever(config.clone()).thenReturn(configClone)
        return config
    }

    protected fun config(key: String): Configuration {
        val config = mock(Configuration::class.java)
        whenever(config.key).thenReturn(key)
        return config
    }
}
