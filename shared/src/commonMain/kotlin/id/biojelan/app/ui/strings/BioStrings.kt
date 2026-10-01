package id.biojelan.app.ui.strings

import androidx.compose.runtime.Immutable

/**
 * Semua string UI BioJelan, dikelompokkan per fitur.
 *
 * Implementasi: [IdStrings] (Indonesia), [EnStrings] (English).
 * Diakses lewat `BioText.current.xxx` di Composable, atau `LocalBioStrings.current` langsung.
 */
@Immutable
interface BioStrings {

    // ============================================================ Common
    val appName: String
    val reload: String
    val back: String
    val save: String
    val cancel: String
    val accept: String
    val close: String
    val total: String
    val or: String

    // ============================================================ Auth
    val welcomeTitle: String
    val welcomeSubtitle: String
    val createAccountTitle: String
    val createAccountSubtitle: String
    val signIn: String
    val signUp: String
    val forgotPasswordLink: String
    val forgotPasswordTitle: String
    val forgotPasswordHeadline: String
    val forgotPasswordBody: String
    val sendResetLink: String
    val resetSentNote: String
    val guestEntry: String
    val retryButton: String

    // ---- Auth fields
    val fieldFullName: String
    val fieldEmail: String
    val fieldPassword: String
    val fieldRepeatPassword: String
    val placeholderName: String
    val placeholderEmail: String
    val placeholderPasswordMin: String
    val placeholderPasswordYours: String
    val placeholderRepeatPassword: String

    // ============================================================ Navigation tabs
    val tabHome: String
    val tabFindAgent: String
    val tabTransactions: String
    val tabStock: String
    val tabHistory: String
    val tabProfile: String

    // ============================================================ Klien Home
    val priceCaption: String
    val quickActionFindAgent: String
    val quickActionHistory: String
    val quickActionMyId: String
    val sectionAgenBioJelan: String
    val viewAll: String
    val noAgentsRegistered: String
    val confirmTransaction: String
    val open: String
    val closed: String
    fun pendingTxBody(agenName: String, volume: String): String

    // ============================================================ Agen Home
    val priceCaptionAgen: String
    val todayTransactions: String
    val collectedToday: String
    val valueToday: String
    val currentStockLabel: String
    val readyForPickup: String
    fun remainingToThreshold(remaining: String): String
    val newTransaction: String
    val recentActivity: String
    val seeAll: String
    val noTransactionsYet: String
    val noTransactionsAgenHint: String

    // ============================================================ Agen Stock
    val stockTitle: String
    fun thresholdCaption(threshold: String): String
    val aboveThresholdNote: String
    val stockThresholdInfo: String
    val stockMovement: String
    val noStockMovement: String
    val stockIncoming: String
    val stockWaiting: String
    val stockCancelled: String
    fun transactionDash(name: String): String

    // ============================================================ Agen Pickup
    val pickupSectionTitle: String
    val pickupNoneScheduled: String
    val pickupStatusAssigned: String
    val pickupStatusOtw: String
    val pickupStatusCompleted: String
    val pickupStatusCancelled: String
    val pickupNoteAssigned: String
    val pickupNoteOtw: String
    val pickupNoteCompleted: String
    fun pickupUpdatedAt(time: String): String

    // ============================================================ Agen Transactions
    val transactionsTitle: String
    val transactionDetailTitle: String
    val todayLabel: String
    val volumeToday: String
    val noTransactionsTitle: String
    val noTransactionsHint: String
    val transactionButton: String
    val pendingAgenNote: String
    val cancelledAgenNote: String

    // ---- Transaction detail labels
    val labelTransactionId: String
    val labelDate: String
    val labelAgent: String
    val labelClient: String
    val labelClientId: String
    val labelVolume: String
    val labelPricePerLiter: String
    val labelTotal: String
    val labelStatus: String

    // ---- New transaction sheet
    val newTransactionTitle: String
    val newTransactionNote: String
    val fieldClientId: String
    val fieldClientName: String
    val fieldVolume: String
    val fieldPricePerLiter: String
    val placeholderClientId: String
    val placeholderClientName: String
    val placeholderVolume: String
    val hintReferencePriceKilang: String
    val submitToClient: String

    // ---- New transaction validation
    val errorClientIdRequired: String
    val errorClientNameRequired: String
    val errorVolumeRequired: String
    val errorVolumeTooLarge: String

    // ============================================================ Klien History
    val historyTitle: String
    val transactionsCount: String
    val oilSold: String
    val totalReceived: String
    val noHistoryTitle: String
    val noHistoryHint: String
    val pendingKlienNote: String
    val cancelNotUndoable: String
    val yesCancel: String
    val reject: String
    val yesReject: String
    val rejectNotUndoable: String
    val cancelRequestTitle: String
    fun cancelRequestBody(agenName: String, volume: String): String
    val acceptCancel: String
    val rejectCancel: String
    val cancelRequestedKlienNote: String
    val requestCancel: String
    val yesRequestCancel: String
    val cancelRequestedAgenNote: String
    val rejectedAgenNote: String
    val fieldClientContact: String
    val placeholderClientContact: String
    val errorClientContactRequired: String
    val roleDriver: String
    val labelIdDriver: String
    val driverRequestTitle: String
    fun driverRequestBody(volume: String): String
    fun driverCancelBody(volume: String): String
    val driverNewTransactionTitle: String
    val driverNewTransactionNote: String
    val fieldAgenContact: String
    val placeholderAgenContact: String
    val errorAgenContactRequired: String
    val fieldTransactionNote: String
    val placeholderTransactionNote: String
    val submitToAgen: String
    val driverNoTransactionsHint: String
    val labelNote: String
    val pendingDriverNote: String
    val rejectedDriverNote: String
    val cancelRequestedDriverNote: String
    val cancelledDriverNote: String
    val driverStockSection: String
    val driverStockRow: String
    val stockOutgoing: String
    val driverPickupSection: String
    val driverPickupNone: String
    val driverPickupAssigned: String
    val driverPickupNoteAssigned: String
    val driverPickupOtw: String
    val driverPickupNoteOtw: String
    val driverPickupCompleted: String
    val driverPickupNoteCompleted: String
    val driverPickupCancelled: String
    val driverPickupStart: String
    val driverPickupFinish: String
    val driverPickupArrived: String
    val driverPickupNoteArrived: String
    val driverPickupArrive: String
    val pickupStatusArrived: String
    val pickupNoteArrived: String
    val routeToAgen: String
    val segmentClient: String
    val segmentDriver: String
    val agenDriverNoTransactionsHint: String
    val labelDriver: String
    val pendingDriverAgenNote: String
    val cancelRequestedDriverAgenNote: String
    val rejectedDriverAgenNote: String
    val cancelledDriverAgenNote: String
    val checkingClient: String
    fun clientFound(name: String): String
    val clientNotFound: String

    // ============================================================ Guest
    val guestBannerTitle: String
    val guestBannerBody: String
    val guestPriceCaption: String
    val guestUpsellTitle: String
    val guestUpsellBody: String
    val registerNow: String
    val maybeLater: String

    // ============================================================ Profile
    val profileTitle: String
    val roleAgent: String
    val roleClient: String
    val sectionAccountData: String
    val sectionAgentData: String
    val sectionSettings: String
    val labelPhone: String
    val labelIdAgent: String
    val labelIdClient: String
    val labelAddress: String
    val labelOperatingHours: String
    val labelBankAccount: String
    val labelStockThreshold: String
    val notFilled: String
    val setByKilang: String
    val editProfile: String
    val changePassword: String
    val logout: String
    val deleteAccount: String

    // ---- Edit profile
    val editProfileTitle: String
    val fieldName: String
    val fieldPhone: String
    val fieldAddress: String
    val fieldBankName: String
    val fieldAccountNumber: String
    val fieldOpenTime: String
    val fieldCloseTime: String
    val fieldOpenDays: String
    val saveChanges: String
    val errorNameRequired: String
    val errorTimeFormat: String
    val placeholderPhone: String
    val placeholderAddress: String
    val placeholderBankName: String
    val placeholderOpenTime: String
    val placeholderCloseTime: String

    // ---- Change password
    val changePasswordTitle: String
    val fieldCurrentPassword: String
    val fieldNewPassword: String
    val fieldRepeatNewPassword: String
    val hintMinChars: String
    val errorCurrentPasswordRequired: String
    val errorMinChars: String
    val errorConfirmMismatch: String
    val savePassword: String

    // ---- Delete account
    val deleteAccountTitle: String
    val deleteAccountWarning: String
    val deleteAccountConfirmLabel: String
    val deleteAccountConfirmWord: String
    val deleteMyAccount: String

    // ============================================================ Transaction status labels
    fun txStatusLabel(status: String, isKlien: Boolean): String
    val statusCancelled: String

    // ============================================================ Greeting
    fun greeting(hour: Int): String

    // ============================================================ Date/Time
    fun relativeToday(time: String): String
    fun relativeYesterday(time: String): String
    val everyDay: String
    val hoursNotSet: String
    val timezone: String

    // ---- Months (abbreviated)
    val monthNames: List<String>

    // ============================================================ Errors (ApiClient)
    val networkError: String
    val sessionExpired: String
    fun serverError(code: Int): String
    val parseError: String

    // ============================================================ ID overlay
    val myIdTitle: String
    val defaultNickname: String
}
