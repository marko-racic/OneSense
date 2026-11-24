# OneSense ne koristi refleksiju ni biblioteke koje traže posebna pravila —
# dovoljne su default rules iz proguard-android-optimize.txt.
# Držimo imena klasa samo za čitljivije stack trace-ove (CrashReport ih prikazuje).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
