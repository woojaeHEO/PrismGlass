# PrismGlass

PrismGlass is a slot based liquid glass component library for Jetpack Compose. It provides adaptive glass surfaces, press interactions, and a stretching navigation indicator while keeping application content fully customizable.

## Install

Add JitPack to `settings.gradle.kts`.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency.

```kotlin
implementation("com.github.woojaeHEO:PrismGlass:1.2.2")
```

## Surface

```kotlin
PrismGlassInteractiveSurface(onClick = onClick) {
    Text("Open", Modifier.padding(20.dp))
}
```

The `Modifier.prismGlass` extension can decorate any Compose component.

## Backdrop

`PrismGlassBackdropHost` keeps the captured background and glass overlays in separate layers. Any Compose content can be used in the background and surface slots.

```kotlin
PrismGlassBackdropHost(
    background = {
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    },
) { backdrop ->
    PrismGlassBackdropSurface(
        state = backdrop,
        modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
    ) {
        Row(Modifier.padding(16.dp)) {
            Text("Any Compose content")
        }
    }
}
```

Use `Modifier.prismGlassBackdropSource` when the source and overlay are already separate siblings. Android 13 and newer uses an AGSL refraction shader chained with GPU blur. Android 12 uses GPU blur. Android 8 through 11 keeps the translucent surface without a runtime effect.

## Navigation

```kotlin
PrismGlassNavigationBar(
    items = destinations,
    selectedItem = selected,
    onItemSelected = onSelect,
    itemLabel = { it.label },
) { item, isSelected ->
    Icon(item.icon, contentDescription = null)
    Text(item.label)
}
```

The navigation component supports AGSL lens refraction on Android 13 and newer, velocity-driven elastic stretching, finger tracking, release selection, arbitrary item content, a custom indicator slot, reduced motion, RTL layouts, accessibility tab semantics, and any item model with stable equality.

## Requirements

- Android 8.0 or newer
- Jetpack Compose
- Java 17 or newer build toolchain

## License

Apache License 2.0
