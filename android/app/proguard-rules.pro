# Defaults come from proguard-android-optimize.txt. The app reads settings with org.json and enum valueOf (kept by the
# default enum rule), and its services, tile and activities are referenced from the manifest (kept by AAPT rules), so
# no extra keep rules are needed. Add one here if a release build misbehaves where the debug build does not.
