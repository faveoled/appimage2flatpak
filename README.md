# Use
```
node main.js --help
Usage: appimage2flatpak [OPTIONS]
appimage2flatpak is a CLI utility for converting AppImage files to Flatpak manifests
Options:

      --app-id  <arg>            Specifies application id. Reversed domain name
                                 (e.g. io.github.user.superapp).
      --app-name  <arg>          Specifies application name. Usually matches an
                                 executable name.
      --appimage-url  <arg>      Specifies appimage URL for download.
      --debug                    Enables debugging mode if specified. Enables
                                 detailed stacktraces for errors.
      --repo-src  <arg>          Specifies a URL where to get raw template
                                 files. Has a default value.
      --runtime  <arg>           Specifies a Flatpak runtime.
                                 'org.freedesktop.Platform' by default.
      --runtime-version  <arg>   Specifies a Flatpak runtime version. Default:
                                 23.08.
      --sdk  <arg>               Specifies a Flatpak SDK. Default:
                                 org.freedesktop.Sdk.
  -h, --help                     Show help message

```

# Production Build
```
sbt fullOptJS
```
# Dev Build (non-optimized & fast)
```
sbt fastLinkJS
```
# Install sample
```
cp target/scala-3.3.1/scala-js-tutorial-opt/main.js appimage2flatpak
sed -i '1 i\#\!/usr/bin/env node' appimage2flatpak 
chmod +x appimage2flatpak 
sudo cp appimage2flatpak /usr/local/bin/
```
