# FocusPhone — FAQ (Preguntas Frecuentes)

> **Versión:** 1.0.0  
> **Estado:** Borrador para revisión  
> **Última revisión:** 2026-05-17  
> **Propietario:** Contenido + Soporte + Frontend  
> **Formato:** Inglés (copy de usuario) + español (anotaciones de contexto y notas técnicas)  
> **Dependencias:** `DESIGN_GUIDE.md`, `SEO_AND_KEYWORDS.md`, `PRODUCT_COPY.md`, `FEATURES_DETAILS.md`  

---

## Cómo está estructurado este documento

Cada pregunta sigue el formato **"mix: corta con expansión opcional"**:

1. **Respuesta breve (EN):** La respuesta directa, visible inmediatamente. Máximo 2-3 líneas. Ideal para móvil y scan rápido.
2. **Expansión / contexto (EN):** El porqué técnico o el detalle educativo. Puede mostrarse en acordeón, tooltip, o párrafo secundario según decida el frontend.
3. **Notas (ES):** Explicación para el equipo de por qué se redactó así, keywords SEO incluidas, y notas de implementación.

> **Regla de oro:** Un usuario impaciente debe obtener la respuesta en 3 segundos. Un usuario curioso puede profundizar sin salir de la página.

---

## 1. Installation

> **Keywords objetivo:** `install focusphone`, `uninstall`, `google play`, `apk`, `android`.  
> **Heading SEO:** `h2` en `/faq`.

---

### 1.1 Will I lose my apps when I install FocusPhone?

**[EN] Short answer:** No. FocusPhone is a launcher, not a system reset. All your apps stay installed. It only changes what you see on the home screen.

**[EN] Context:** A launcher is just a different way of displaying your apps. Your photos, messages, accounts, and settings remain untouched. Think of it as changing the wallpaper, but for your entire home screen.

> **[ES]** Esta es la objeción #1 para cualquier launcher. La respuesta corta elimina el miedo inmediatamente. La expansión usa la analogía del wallpaper para que usuarios no técnicos entiendan.  
> **Keywords:** `install focusphone`, `lose my apps`.  
> **Schema:** Esta pregunta está incluida en el FAQPage schema de `SEO_AND_KEYWORDS.md` §7.2.

---

### 1.2 How do I uninstall FocusPhone and go back?

**[EN] Short answer:** Go to Android Settings → Apps → FocusPhone → Uninstall. If it is your default launcher, first switch to another launcher in Settings → Default apps.

**[EN] Context:** Android requires you to have at least one launcher active. Before uninstalling FocusPhone, you must set another launcher (like your original one) as the default. If you forget, Android will prompt you automatically.

> **[ES]** La respuesta corta es procedural y escaneable. La expansión explica el porqué (restricción del sistema operativo) para que el usuario no sienta que FocusPhone lo "atrapa".  
> **Keywords:** `uninstall focusphone`, `default launcher android`, `go back`.  
> **Nota técnica:** Esta pregunta también reduce el miedo a probar el producto. Es estratégica para la conversión.

---

### 1.3 Why does the APK come from a website and not Google Play?

**[EN] Short answer:** FocusPhone is distributed directly to remain fully open source and privacy-first, without Google Play policies or tracking.

**[EN] Context:** Publishing on Google Play requires compliance with policies that can change, and it introduces Google’s analytics and signing requirements. By distributing the APK directly, we keep the app lightweight, transparent, and under your control. You always get the exact code that is published on GitHub.

> **[ES]** Respuesta delicada. No atacamos a Google Play, pero reframeamos la decisión como una ventaja (transparencia, control). La expansión conecta con el valor de open source.  
> **Keywords:** `google play`, `apk download`, `android launcher no google play`, `open source`.  
> **Nota de confianza:** Mencionar GitHub aquí refuerza la legitimidad del APK.

---

### 1.4 Is it safe to install an APK from a website?

**[EN] Short answer:** Yes, if you trust the source. FocusPhone is open source on GitHub, where you can inspect the code and verify the APK yourself.

**[EN] Context:** The APK on focusphone.app is the same build published on GitHub Releases. You can check the SHA-256 checksum, review the source code, or even build the app yourself. No hidden code. no remote connections.

> **[ES]** Objeción crítica para apps fuera de Google Play. La respuesta corta valida el miedo (`if you trust the source`) en lugar de ignorarlo. La expansión da herramientas concretas (SHA-256, GitHub, build yourself) para que el usuario tenga agency.  
> **Keywords:** `apk download`, `safe`, `open source`, `github`.  
> **Nota:** No prometemos "100% seguro" de forma genérica. Eso suena a marketing vacío.

---

### 1.5 How do I get updates?

**[EN] Short answer:** Visit the Download page or check GitHub Releases. We post every update there with a clear changelog.

**[EN] Context:** FocusPhone does not include an automatic updater to keep the app offline and lightweight. You download the new APK and install it over the current version. Your settings and favourites are preserved.

> **[ES]** La ausencia de auto-updater puede parecer una desventaja; aquí se reframea como coherencia con la filosofía offline-first. La expansión tranquiliza sobre la pérdida de datos.  
> **Keywords:** `download`, `updates`, `changelog`.

---

## 2. Using FocusPhone

> **Keywords objetivo:** `profiles`, `conscious pause`, `reorder`, `widgets`, `dark mode`.  
> **Heading SEO:** `h2` en `/faq`.

---

### 2.1 What are the three profiles for?

**[EN] Short answer:** Normal lets you use everything. Work blocks distracting apps automatically. Detox locks everything except FocusPhone itself for a set time.

**[EN] Context:** Profiles adapt your phone to your context, not the other way around. Normal is your everyday mode. Work protects your focus hours. Detox is for when you need a complete break. You switch between them with a single tap.

> **[ES]** La respuesta corta usa paralelismo para escaneo rápido. La expansión conecta con el beneficio emocional (adaptación al contexto, no al teléfono).  
> **Keywords:** `profiles`, `work profile`, `detox mode`, `normal profile`.  
> **Alineación con FEATURES_DETAILS.md:** §3.

---

### 2.2 What is Conscious Pause?

**[EN] Short answer:** A breathing countdown that appears before opening an app. You set the duration. It gives you a moment between impulse and action.

**[EN] Context:** When enabled, tapping any favourite app triggers a full-screen timer. You see the countdown, you breathe, and only then the app opens. You can cancel at any time. It is designed to break unconscious scrolling habits, not to frustrate you.

> **[ES]** La respuesta corta define el feature en una oración. La expansión explica la mecánica y la intención psicológica (break unconscious habits). El cierre (`not to frustrate you`) anticipa una objeción común.  
> **Keywords:** `conscious pause`, `breathing timer`, `what is conscious pause`.  
> **Alineación con FEATURES_DETAILS.md:** §4.

---

### 2.3 Can I reorder my favourite apps?

**[EN] Short answer:** Yes. Press and hold any favourite on the home screen, then drag it to a new position.

**[EN] Context:** There is no save button. The order updates instantly and persists across reboots. You can have up to ten favourites. If you want to remove one, long-press it and select remove.

> **[ES]** Respuesta procedural simple. La expansión añade detalles útiles (persistencia, límite de 10, cómo remover) que evitan preguntas de seguimiento.  
> **Keywords:** `reorder favourite apps`, `drag`, `home screen`.

---

### 2.4 Can I still use widgets?

**[EN] Short answer:** No. FocusPhone does not support widgets. The home screen is intentionally minimal: a clock, a date, and your favourite apps.

**[EN] Context:** Widgets are one of the main sources of visual distraction and battery drain on Android home screens. By removing them, FocusPhone keeps your screen calm and your attention protected. The full app drawer is always one swipe away if you need quick access.

> **[ES]** Esta es una objeción esperada del nicho minimalismo/digital detox. La respuesta corta es honesta (`No`). La expansión no se disculpa; explica la filosofía del producto. Esto filtra a usuarios que realmente buscan minimalismo y aleja a quienes solo quieren otro launcher.  
> **Keywords:** `widgets`, `minimalist launcher`, `text-only`.

---

### 2.5 Does FocusPhone have a dark mode?

**[EN] Short answer:** Yes. FocusPhone follows your system theme automatically. Each of the four built-in themes has a dark variant.

**[EN] Context:** There is no manual toggle inside the app. This is intentional: one less decision to make. If your phone is in dark mode, FocusPhone is too. The dark variants use deep charcoal tones (`#0F0F12`) rather than pure black, which is easier on the eyes.

> **[ES]** La respuesta corta es tranquilizadora. La expansión refuerza la filosofía de marca (sin opciones innecesarias) y educa sobre la elección de color (charcoal vs pure black).  
> **Keywords:** `dark mode`, `themes`, `automatic`.  
> **Alineación con DESIGN_GUIDE.md:** §3.3 (modo oscuro).

---

### 2.6 Will my notifications still work?

**[EN] Short answer:** Yes. Notifications from your apps arrive exactly as before. FocusPhone only changes your home screen, not the rest of your phone.

**[EN] Context:** Your status bar, notification shade, sound alerts, and lock screen notifications remain unchanged. However, FocusPhone does not show notification badges on the home screen, which helps reduce visual anxiety.

> **[ES]** Objeción de usuarios que dependen de notificaciones. La respuesta corta es clara. La expansión añade el beneficio diferenciador (no badges en home).  
> **Keywords:** `notifications`, `work`, `alerts`.

---

## 3. Privacy & Security

> **Keywords objetivo:** `privacy`, `personal data`, `query_all_packages`, `detox password`, `safe`.  
> **Heading SEO:** `h2` en `/faq`.  
> **Nota:** Esta sección es crítica para la conversión dado que el APK se descarga fuera de Google Play.

---

### 3.1 Does FocusPhone collect personal data?

**[EN] Short answer:** No. FocusPhone does not collect, transmit, or sell any personal data. Everything stays on your device.

**[EN] Context:** There are no analytics libraries, no ad networks, no crash reporting services, and no remote servers. Your favourite apps, themes, and settings are stored locally using Room and SharedPreferences. We do not know who you are, what apps you use, or where you are.

> **[ES]** La respuesta corta es absoluta (`No`). La expansión enumera específicamente qué NO hay (analytics, ads, crash reporting) porque la ausencia concreta genera más confianza que la afirmación genérica.  
> **Keywords:** `does focusphone collect data`, `personal data`, `privacy`.  
> **Schema:** Incluida en FAQPage schema de `SEO_AND_KEYWORDS.md` §7.2.

---

### 3.2 Why does the app need QUERY_ALL_PACKAGES?

**[EN] Short answer:** To list all your installed apps in the drawer and let you pick favourites. That is the only reason.

**[EN] Context:** Android requires this permission for any launcher that wants to show a complete app list. Without it, FocusPhone could not display your apps or let you select favourites. The permission is only used to read app names and package names. We do not inspect app content, usage data, or personal files.

> **[ES]** `QUERY_ALL_PACKAGES` suena alarmante para usuarios conscientes de privacidad. La respuesta corta lo normaliza. La expansión explica la restricción del sistema operativo y delimita exactamente qué se lee y qué no.  
> **Keywords:** `query_all_packages`, `permissions`, `privacy`.  
> **Nota:** Es crucial no sonar a la defensiva. La explicación debe ser factual, no excusa.

---

### 3.3 Is my detox password safe?

**[EN] Short answer:** Yes. Your detox password is hashed with SHA-256 before being stored locally on your device.

**[EN] Context:** The password never leaves your phone. It is not transmitted, not backed up to the cloud, and not readable by any other app. Even if someone gains access to your device files, they cannot reverse the hash to discover your password.

> **[ES]** La respuesta corta menciona SHA-256 como señal de seriedad técnica. La expansión explica las tres capas de protección (local, no backup, no reversible).  
> **Keywords:** `detox password`, `sha-256`, `safe`, `security`.  
> **Alineación con AGENTS.md:** §Consideraciones de Seguridad.

---

### 3.4 What happens if I forget my detox password?

**[EN] Short answer:** After four failed attempts, detox mode disables itself automatically and clears the password. You will not be locked out permanently.

**[EN] Context:** This is a safety measure. We intentionally do not offer password recovery via email or cloud, because FocusPhone has no servers. The four-attempt limit protects you from being permanently blocked while keeping the feature secure. Once detox disables itself, you can re-enable it with a new password.

> **[ES]** Esta respuesta aborda el miedo real a quedar atrapado. La respuesta corta es tranquilizadora. La expansión explica el trade-off (sin recuperación por email = sin servidores = más privacidad) y refuerza que el usuario siempre tiene control.  
> **Keywords:** `forget detox password`, `locked out`, `security`.  
> **Nota:** El tono debe ser calmado, no alarmista. "You will not be locked out permanently" es la frase clave.

---

### 3.5 Is the APK signed and verifiable?

**[EN] Short answer:** Yes. The release APK is signed with a release key. You can verify the signature and compare the SHA-256 checksum against GitHub Releases.

**[EN] Context:** Signed APKs ensure that the app you install is exactly the one we built, without modification. The signing certificate and checksums are published alongside every release. If you are technically inclined, you can use `apksigner` or `jarsigner` to verify.

> **[ES]** Pregunta de usuarios técnicos que descargan APKs fuera de Play. La respuesta corta valida la preocupación. La expansión da herramientas técnicas concretas (checksums, `apksigner`).  
> **Keywords:** `apk`, `signed`, `sha-256`, `verify`, `security`.

---

## 4. Compatibility

> **Keywords objetivo:** `compatibility`, `android 7.0`, `will focusphone work on my phone`.  
> **Heading SEO:** `h2` en `/faq`.

---

### 4.1 Will FocusPhone work on my phone?

**[EN] Short answer:** If your phone runs Android 7.0 (API 24) or higher, yes.

**[EN] Context:** FocusPhone is built for modern Android but keeps compatibility back to Nougat (2016). It works on phones from Samsung, Google, OnePlus, Xiaomi, Motorola, and most other manufacturers. If your phone can run apps from Google Play, it can almost certainly run FocusPhone.

> **[ES]** La respuesta corta da el requisito exacto. La expansión nombra fabricantes populares para que el usuario se sienta incluido.  
> **Keywords:** `will focusphone work on my phone`, `android 7.0`, `compatibility`.  
> **Nota:** Evitar decir "todos los teléfonos". Es mejor under-promise y over-deliver.

---

### 4.2 Does it work on tablets or foldables?

**[EN] Short answer:** Yes, though the interface is optimized for phones. It will run on tablets and foldable devices running Android 7.0+.

**[EN] Context:** The layout uses responsive sizing, but it is designed primarily for portrait phone screens. On tablets, the interface will work correctly but may not take full advantage of the larger screen. We may add tablet-specific optimizations in future releases.

> **[ES]** Respuesta honesta. No prometemos optimización tablet pero tampoco la excluimos. La expansión gestiona expectativas y deja abierta la puerta a futuras mejoras.  
> **Keywords:** `tablet`, `foldable`, `compatibility`.

---

### 4.3 Can I use FocusPhone alongside another launcher?

**[EN] Short answer:** Android only allows one default launcher at a time. You can switch between FocusPhone and any other launcher in Settings → Default apps.

**[EN] Context:** You do not need to uninstall your previous launcher to try FocusPhone. You can switch back and forth as often as you like. Your previous launcher’s settings and widgets remain intact while FocusPhone is active.

> **[ES]** Objeción de usuarios que quieren probar sin compromiso. La respuesta corta explica la limitación del SO. La expansión enfatiza que el cambio es reversible y sin pérdida de datos.  
> **Keywords:** `default launcher`, `switch`, `multiple launchers`.

---

## 5. Schema Markup FAQPage (JSON-LD)

> **Listo para copiar y pegar en el `<head>` de `/faq`.** Derivado de `SEO_AND_KEYWORDS.md` §7.2, expandido con todas las preguntas de este documento.

```json
{
  "@context": "https://schema.org",
  "@type": "FAQPage",
  "mainEntity": [
    {
      "@type": "Question",
      "name": "Will I lose my apps when I install FocusPhone?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "No. FocusPhone is a launcher, not a system reset. All your apps stay installed. It only changes what you see on the home screen."
      }
    },
    {
      "@type": "Question",
      "name": "How do I uninstall FocusPhone and go back?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Go to Android Settings → Apps → FocusPhone → Uninstall. If it is your default launcher, first switch to another launcher in Settings → Default apps."
      }
    },
    {
      "@type": "Question",
      "name": "Why does the APK come from a website and not Google Play?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "FocusPhone is distributed directly to remain fully open source and privacy-first, without Google Play policies or tracking. You always get the exact code published on GitHub."
      }
    },
    {
      "@type": "Question",
      "name": "Does FocusPhone collect personal data?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "No. FocusPhone does not collect, transmit, or sell any personal data. Everything stays on your device. There are no analytics, no ads, and no remote servers."
      }
    },
    {
      "@type": "Question",
      "name": "Why does the app need QUERY_ALL_PACKAGES?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "To list all your installed apps in the drawer and let you pick favourites. Android requires this permission for any launcher. The permission is only used to read app names and package names."
      }
    },
    {
      "@type": "Question",
      "name": "Is my detox password safe?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. Your detox password is hashed with SHA-256 before being stored locally on your device. It never leaves your phone and is not backed up to the cloud."
      }
    },
    {
      "@type": "Question",
      "name": "What happens if I forget my detox password?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "After four failed attempts, detox mode disables itself automatically and clears the password. You will not be locked out permanently."
      }
    },
    {
      "@type": "Question",
      "name": "Will FocusPhone work on my phone?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "If your phone runs Android 7.0 (API 24) or higher, yes. It works on phones from Samsung, Google, OnePlus, Xiaomi, Motorola, and most other manufacturers."
      }
    },
    {
      "@type": "Question",
      "name": "What is Conscious Pause?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "A breathing countdown that appears before opening an app. You set the duration. It gives you a moment between impulse and action."
      }
    },
    {
      "@type": "Question",
      "name": "Does FocusPhone have a dark mode?",
      "acceptedAnswer": {
        "@type": "Answer",
        "text": "Yes. FocusPhone follows your system theme automatically. Each of the four built-in themes has a dark variant."
      }
    }
  ]
}
```

> **[ES]** Este schema utiliza las 10 preguntas principales del FAQ. Si se añaden más preguntas al documento, actualizar también el JSON-LD.  
> **Nota:** El texto de cada `acceptedAnswer.text` usa la **respuesta corta** de este documento, no la expansión. Google prefiere respuestas concisas para rich snippets.

---

## 6. Checklist de Publicación

Antes de publicar `/faq`, verificar:

- [ ] Cada pregunta tiene **una respuesta corta** escaneable en 3 segundos.
- [ ] Las expansiones técnicas son **opcionales visivamente** (acordeón, tooltip, o párrafo secundario).
- [ ] Las preguntas siguen el heading map de `SEO_AND_KEYWORDS.md` §5.2.
- [ ] Las keywords de cada sección están presentes en las preguntas o respuestas.
- [ ] El schema FAQPage JSON-LD está incluido en el `<head>` de la página.
- [ ] Los acordeones usan el estilo definido en `DESIGN_GUIDE.md` §6.4 (chevron ▶, border-bottom sutil, hover en desktop).
- [ ] No hay respuestas que prometan "100% seguro" de forma genérica.
- [ ] Las respuestas sobre desinstalación y contraseñas no generan ansiedad adicional.
- [ ] El enlace a `/download` aparece al final de la sección "still have questions?".

---

> **Nota para redactores y desarrolladores:**  
> El FAQ es una herramienta de conversión disfrazada de soporte. Cada respuesta que reduce miedo o elimina fricción aumenta la probabilidad de descarga. Mantener el tono calmado, factual y empoderador.
