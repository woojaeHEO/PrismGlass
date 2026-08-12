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
implementation("com.github.woojaeHEO:PrismGlass:1.1.0")
```

## Surface

```kotlin
PrismGlassInteractiveSurface(onClick = onClick) {
    Text("Open", Modifier.padding(20.dp))
}
```

The `Modifier.prismGlass` extension can decorate any Compose component.

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

The navigation component supports finger tracking, release selection, arbitrary item content, a custom indicator slot, reduced motion, RTL layouts, accessibility tab semantics, and any item model with stable equality.

## Requirements

- Android 8.0 or newer
- Jetpack Compose
- Java 17 or newer build toolchain

## License

Apache License 2.0
