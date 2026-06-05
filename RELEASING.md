# Releasing Holocron

Holocron ships as a **signed release APK** attached to a GitHub Release. The same signing
key is used for every build so installed copies update **in place** (no uninstall/data loss).

## Versioning

Date-based `YYYY.M.PATCH` (no leading zero on the month, e.g. `2026.6.0`). The release
workflow auto-computes the next patch for the current month, or you can pass an explicit
version. The Android `versionCode` is derived from the version monotonically, so newer
releases always sort above older ones.

## One-time setup

### 1. Create a release keystore (once, keep it forever)

```bash
keytool -genkeypair -v \
  -keystore holocron.jks \
  -alias holocron \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASSWORD> -keypass <KEY_PASSWORD> \
  -dname "CN=Holocron, O=Holocron, C=IL"
```

> ⚠️ Back this file up. If you lose it you can no longer ship updates that install over an
> existing copy — users would have to uninstall first. The CI key **must** stay identical to
> the local key.

### 2. Add GitHub Actions secrets

Repo → Settings → Secrets and variables → Actions → **New repository secret**:

| Secret | Value |
|---|---|
| `HOLOCRON_KEYSTORE_BASE64` | `base64 -w0 holocron.jks` (the keystore, base64-encoded) |
| `HOLOCRON_KEYSTORE_PASSWORD` | the store password |
| `HOLOCRON_KEY_ALIAS` | `holocron` (the alias above) |
| `HOLOCRON_KEY_PASSWORD` | the key password |

On macOS use `base64 -i holocron.jks | tr -d '\n'`.

### 3. (Optional) Sign local builds with the same key

So local `debug` builds are signature-compatible with CI releases (update in place), create a
git-ignored `keystore.properties` in the repo root:

```properties
storeFile=/absolute/path/to/holocron.jks
storePassword=<STORE_PASSWORD>
keyAlias=holocron
keyPassword=<KEY_PASSWORD>
```

`app/build.gradle.kts` reads env vars first (CI), then this file (local), and falls back to
the default debug key when neither is present. With the key present, `debug` is signed with
the release key too.

## Cutting a release

1. GitHub → **Actions** → **Release** → **Run workflow**.
2. Optionally type a version; leave blank to auto-compute `YYYY.M.PATCH`.
3. The workflow builds the signed release APK, verifies the signing cert, tags the commit,
   and publishes a GitHub Release with `holocron-<version>.apk` attached.

## ⚠️ First install after switching to the signed key

The builds you've sideloaded so far were signed with the throwaway **debug** key. The first
release-key APK has a different signature, so Android will refuse to install it over the old
one (`App not installed` / signature mismatch). **Uninstall the current Holocron once**, then
install the release APK. Every release after that updates in place.
