# wenku8-reader keeps minify disabled for now; rules here are for future enablement.
# okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
# room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
