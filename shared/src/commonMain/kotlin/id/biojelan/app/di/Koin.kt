package id.biojelan.app.di

import com.russhwolf.settings.Settings
import id.biojelan.app.core.AppConfig
import id.biojelan.app.data.local.SessionStore
import id.biojelan.app.data.mock.MockApiClient
import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.repository.AuthRepository
import id.biojelan.app.data.repository.ApiPriceProvider
import id.biojelan.app.data.repository.PickupRepository
import id.biojelan.app.data.repository.PriceProvider
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.DriverTransactionRepository
import id.biojelan.app.data.repository.TransactionRepository
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.ui.RootViewModel
import id.biojelan.app.ui.account.AccountViewModel
import id.biojelan.app.ui.agen.AgenViewModel
import id.biojelan.app.ui.driver.DriverViewModel
import id.biojelan.app.ui.auth.AuthViewModel
import id.biojelan.app.ui.guest.GuestViewModel
import id.biojelan.app.ui.klien.KlienViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

private val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }
    }
    single {
        HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = AppConfig.CONNECT_TIMEOUT_MS
                requestTimeoutMillis = AppConfig.REQUEST_TIMEOUT_MS
            }
        }
    }
    single<Settings> { Settings() }
    single { SessionStore(get()) }
    single { SessionManager(get(), get()) }
    single { MockApiClient(get()) }
    single { ApiClient(get(), get(), get(), get()) }
    single<PriceProvider> { ApiPriceProvider(get(), get()) }
    single { AuthRepository(get(), get(), get()) }
    single { UserRepository(get(), get(), get()) }
    single { TransactionRepository(get(), get()) }
    single { DriverTransactionRepository(get(), get()) }
    single { PickupRepository(get(), get()) }
}

private val viewModelModule = module {
    factoryOf(::RootViewModel)
    factoryOf(::AuthViewModel)
    factoryOf(::KlienViewModel)
    factoryOf(::GuestViewModel)
    factoryOf(::AgenViewModel)
    factoryOf(::DriverViewModel)
    factoryOf(::AccountViewModel)
}

fun initKoin() {
    startKoin {
        modules(dataModule, viewModelModule)
    }
}
