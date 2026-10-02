package id.biojelan.mobile

/** Penanda sederhana app sedang terlihat atau tidak; saat terlihat, toast di dalam app sudah cukup. */
object AppForeground {
    @Volatile
    var visible: Boolean = false
}
