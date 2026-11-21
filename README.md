# B-Nox

B-Nox is an Android companion app that provides a Secure Folder-style interface backed by an Android Managed Profile (work profile).

## Current implementation
- Managed Profile provisioning through Android's supported provisioning flow.
- Profile Owner receiver and policy setup.
- In-profile PIN gate and app drawer.
- Local folders for organizing apps in the B-Nox interface.
- GitHub Actions debug APK build.

## Security model
Apps installed in the managed profile run under Android's separate profile/user sandbox. This is not Samsung Knox and does not claim hardware-backed Knox guarantees. Cross-profile sharing, clipboard, network access and OEM policy behavior remain subject to Android and device configuration.

## Build
Open with Android Studio or run:
```bash
./gradlew assembleDebug
```

The APK is built by GitHub Actions on pushes and pull requests.
