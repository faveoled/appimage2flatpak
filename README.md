# Use
```
$ appimage2flatpak -h
Usage: appimage2flatpak [OPTIONS]
appimage2flatpak is a CLI utility for converting AppImage files to Flatpak manifests
Options:

      --app-id  <arg>            Specifies application id. Reversed domain name
                                 (e.g. io.github.user.Superapp).
      --app-name  <arg>          Specifies application name. Usually matches an
                                 executable name.
      --appimage-url  <arg>      Specifies appimage URL for download.
      --debug                    Enables debugging mode if specified. Enables
                                 detailed stacktraces for errors.
      --repo-archive  <arg>      Specifies a URL where to get raw template
                                 archive. Default:
                                 https://github.com/faveoled/AppImage-Flatpak-Template/archive/refs/heads/foss.tar.gz
      --runtime  <arg>           Specifies a Flatpak runtime. Default:
                                 org.freedesktop.Platform
      --runtime-version  <arg>   Specifies a Flatpak runtime version. Default:
                                 23.08
      --sdk  <arg>               Specifies a Flatpak SDK. Default:
                                 org.freedesktop.Sdk
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
