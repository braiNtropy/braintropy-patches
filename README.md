# 👋🧩 braiNtropy Patches

Custom patches for apps I use, for use with Morphe.

## ❓ About

This repository contains patches maintained by [braiNtropy](https://github.com/braiNtropy).

## 🩹 Patches list

<!-- PATCHES_START -->
> **[v2.7.0-dev.2](https://github.com/braiNtropy/braintropy-patches/releases/tag/v2.7.0-dev.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;6 patches total

| App | 💊&nbsp;Patch | Function | ✅&nbsp;Tested | 👤&nbsp;User-reported |
|-----|--------|----------|---------|----------------|
| 📦 Waking Up<br>`org.wakingup.android` | Disable update prompt | Suppresses the "Update Required" dialog shown on launch when the server reports a newer preferred build, and spoofs the app-version/app-build request headers so the server (which rejects outdated builds with a 404) keeps serving the app. Does not affect the version shown in the app. | 3.27.0 | 3.27.1 |
|  | Unlock Premium | Unlocks all premium content and removes subscription banners. | 3.27.0 | 3.27.1 |
| 📦 Hume<br>`com.elink.fittrackhealth.pro` | Unlock Premium | Unlocks Hume Plus by feeding fake Play Billing purchases to the purchase stream the app's Dart subscription repository listens to. | 9.29.2 | — |
| 📦 RP Hypertrophy<br>`com.rp.hypertrophy` | Unlock Premium | Enables app features locked behind the subscription paywall. | 1.9.0 | — |

**🌐 Universal patches**

| 💊&nbsp;Patch | Function |
|--------|----------|
| Disable Expo OTA updates | Disables expo-updates: the app no longer contacts the Expo update server (u.expo.dev) on launch and can never download or run over-the-air JavaScript updates published by the developer. Only applies to apps built with Expo. |
| Disable Pairip license check | Disables the Google Play license check (Pairip) that redirects installs not purchased through the Play Store to the buy page. The check never runs, so this also works on degoogled devices without Play services. Does not bypass Play Integrity attestation. |
<!-- PATCHES_END -->

#### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=braiNtropy/braintropy-patches

Or manually add this repository URL as a patch source in Morphe: https://github.com/braiNtropy/braintropy-patches

### 🛠️ Building

After granting the GitHub CLI token `read:packages` access, build on Windows with:

```powershell
.\build-patches.ps1
```

The patch bundle is generated under `patches/build/libs/`. See the
[Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for the complete workflow.

## 📜 License

braiNtropy Patches are licensed under the [GNU General Public License v3.0](LICENSE).

The `Unlock Premium` patch for RP Hypertrophy is derived from
[hoo-dles/morphe-patches](https://github.com/hoo-dles/morphe-patches) (GPL-3.0),
with thanks to hoo-dles for the original endpoint analysis.
