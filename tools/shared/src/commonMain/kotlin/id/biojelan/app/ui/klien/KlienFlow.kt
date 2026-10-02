package id.biojelan.app.ui.klien

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
internal object KlienMainRoute

@Serializable
internal data class AgenDetailRoute(val agenId: String)

/** Alur pengguna berperan Klien: tab utama + detail Agen. */
@Composable
fun KlienFlow() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = KlienMainRoute) {
        composable<KlienMainRoute> {
            KlienMainScreen(onOpenAgen = { nav.navigate(AgenDetailRoute(it)) })
        }
        composable<AgenDetailRoute> { entry ->
            val route = entry.toRoute<AgenDetailRoute>()
            AgenDetailScreen(agenId = route.agenId, onBack = { nav.popBackStack() })
        }
    }
}
