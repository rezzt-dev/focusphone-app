# Privacy Policy — FocusPhone

**Effective Date:** 25 May 2026  
**Last Updated:** 25 May 2026  
**App:** FocusPhone (`app.focusphone`)  
**Developer:** Juan Garcia Cazallas  
**Contact:** contacto@rezzt.dev

---

## 1. Our Fundamental Commitment to Privacy

FocusPhone is designed from the ground up to respect your privacy. **We do not collect, transmit, store, or share any personal data on external servers.** The application operates entirely on your device in a fully offline mode. There is no user account system, no analytics, no advertising, and no telemetry of any kind.

---

## 2. Information We Do Not Collect

To be absolutely clear, FocusPhone does **not** collect or process:

- Names, email addresses, phone numbers, or any contact information.
- Location data (GPS, network-based, or otherwise).
- Browsing history, search queries, or usage patterns.
- Device identifiers (such as IMEI, MAC address, or advertising IDs).
- Personal files, photos, messages, or media.
- Any data transmitted to the internet.

Because we have no servers and no third-party analytics SDKs integrated, no information ever leaves your device.

---

## 3. Why We Request `QUERY_ALL_PACKAGES`

FocusPhone requests the `QUERY_ALL_PACKAGES` permission solely to fulfill its core function as an Android launcher. Specifically, this permission is used to:

- **List installed applications** on your device so you can select which apps to add to your favourites.
- **Display app names** in the app drawer and the favourites list.
- **Launch applications** when you tap on them.

We do **not** use this permission to inspect app contents, collect app usage statistics, transmit app lists externally, or infer personal interests. The list of installed apps is processed entirely in-memory or stored locally on your device (see Section 4).

---

## 4. Local Data Storage

All data created by FocusPhone is stored **locally and offline** using Android standard storage mechanisms:

| Storage Mechanism | Purpose | Data Stored |
|---|---|---|
| **Room Database** | Persist your favourite apps list | App package names and user-defined sort order |
| **SharedPreferences** | App settings and preferences | Theme selection, font choice, profile settings (Normal / Work / Detox), conscious pause duration, custom app labels |
| **SharedPreferences** | Detox password (hashed) | SHA-256 hash of your locally-set Detox password |

**Important:** None of this data is encrypted by FocusPhone itself, as it is protected by Android's standard application sandbox. However, no data is ever backed up to cloud services (see Section 5).

---

## 5. Security Measures

### 5.1 Detox Password Protection
If you enable the Detox profile and set a password, FocusPhone stores only a **SHA-256 cryptographic hash** of that password. The original plaintext password is never retained. After four consecutive incorrect password attempts, the Detox mode is automatically disabled and the stored hash is permanently erased as a safety measure.

### 5.2 No Cloud Backup
FocusPhone explicitly excludes its local data from Android cloud backup and restore operations. This is enforced via `data_extraction_rules.xml` in the application package. Your settings, favourites, and password hash remain on your device only and are never transferred to Google servers or any other cloud provider.

---

## 6. Third-Party Services and SDKs

FocusPhone contains **zero third-party libraries** for analytics, crash reporting, advertising, or data collection. The application uses only standard AndroidX libraries and Google's Material Design components for user interface rendering. No third party receives any data from FocusPhone.

---

## 7. Children's Privacy

FocusPhone does not target children under the age of 13. We do not knowingly collect any personal information from anyone, including children. Since the app collects no data, there is no risk of inadvertent data collection from minors. If you are a parent or guardian and believe your child has provided information of concern, please contact us and we will assist you.

---

## 8. Your Privacy Rights

Even though FocusPhone does not process personal data, we respect your rights under global privacy frameworks, including the **General Data Protection Regulation (GDPR)** in the European Union and the **California Consumer Privacy Act (CCPA)**.

Given that all data is local and under your direct control, you can exercise the following rights at any time without needing to contact us:

- **Right to Access / Portability:** Your favourites list and settings are stored in standard local Android storage.
- **Right to Erasure ("Right to be Forgotten"):** Uninstalling FocusPhone from your device permanently deletes all local data, including the Room database and all SharedPreferences. No copy remains on our servers because we have none.
- **Right to Rectification:** You can modify your favourites, settings, and custom labels directly within the app's Settings screen.
- **Right to Restriction / Objection:** You may disable any feature (including profiles or conscious pause) at any time via the Settings screen.

If you have questions about these rights or need assistance, you may contact us at **contacto@rezzt.dev**.

---

## 9. International Users

By downloading and using FocusPhone, you acknowledge that the app is provided by an individual developer based in Spain. The app is distributed globally via our website and, in the future, the Google Play Store. Regardless of your location, no personal data is transferred across borders because no personal data is collected or transmitted.

---

## 10. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in the application or legal requirements. Any changes will be posted on this page with an updated "Last Updated" date. We encourage you to review this policy periodically. Continued use of FocusPhone after any changes constitutes your acceptance of the revised policy.

---

## 11. Contact Us

If you have any questions, concerns, or requests regarding this Privacy Policy or FocusPhone's data practices, please contact:

**Juan Garcia Cazallas**  
Email: **contacto@rezzt.dev**

We aim to respond to all privacy-related inquiries within 30 days.

---

*This Privacy Policy is effective as of 25 May 2026.*
