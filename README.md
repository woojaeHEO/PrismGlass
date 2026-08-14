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
implementation("com.github.woojaeHEO:PrismGlass:1.3.1")
```

## Surface

```kotlin
PrismGlassInteractiveSurface(onClick = onClick) {
    Text("Open", Modifier.padding(20.dp))
}
```

The `Modifier.prismGlass` extension can decorate any Compose component.

Customize press motion without replacing the component.

```kotlin
PrismGlassInteractiveSurface(
    onClick = onClick,
    pressSpec = PrismGlassPressSpec(pressedScale = .94f),
) {
    content()
}
```

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

`PrismGlassOptics` separates visual policy from rendering. Use `Automatic` for platform-aware behavior, `Translucent` for the lowest GPU cost, `Blur` to disable refraction, or `Refractive` to request the full effect with a safe fallback.

```kotlin
PrismGlassBackdropSurface(
    state = backdrop,
    optics = PrismGlassOptics(
        blurRadius = 14.dp,
        refraction = .24f,
        quality = PrismGlassQuality.Automatic,
    ),
) {
    content()
}
```

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

## Architecture and customization

PrismGlass keeps policy objects independent from Compose rendering.

- `PrismGlassStyle` owns colors, shape, edge, border, and shadow tokens.
- `PrismGlassOptics` owns blur, refraction, and platform quality policy.
- `PrismGlassMotionSpec` and `PrismGlassPressSpec` own spring and scale behavior.
- `PrismGlassSelectionPolicy` decides how a drag position settles.
- `PrismGlassNavigationState` exposes current index, continuous position, velocity, and drag state.
- Surface, backdrop, and navigation composables adapt those policies to Compose.

All configuration types have safe defaults. Invalid dimensions and non-finite motion or optical values are clamped before rendering.

```kotlin
val glassState = rememberPrismGlassNavigationState()

PrismGlassNavigationBar(
    items = destinations,
    selectedItem = selected,
    onItemSelected = onSelect,
    itemLabel = { it.label },
    state = glassState,
    motionSpec = PrismGlassMotionSpec(pressedScale = 1.32f),
    selectionPolicy = PrismGlassSelectionPolicy { position, count ->
        position.toInt().coerceIn(0, count - 1)
    },
    indicatorOptics = PrismGlassOptics(10.dp, .28f),
) { item, isSelected ->
    NavigationItem(item, isSelected)
}
```

## Requirements

- Android 8.0 or newer
- Jetpack Compose
- Java 17 or newer build toolchain

## License

Apache License 2.0
